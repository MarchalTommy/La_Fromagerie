import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { InvoiceLine, InvoiceNinjaClient, describeError } from "./invoiceNinja.js";

/**
 * Logique de facturation partagée entre les deux déclencheurs :
 *  - handleSumUpWebhook (chemin hosted-checkout, vérifié côté SumUp) ;
 *  - onOrderPaidCreateInvoice (trigger Firestore, sur toute transition vers
 *    PAID, quel que soit le chemin de paiement).
 *
 * Idempotent : chaque commande est facturée une seule fois, via une
 * réclamation transactionnelle dans la collection `invoices`. Les deux
 * déclencheurs peuvent donc coexister sans risque de double facture.
 *
 * Une seule exception, décidée par la boutique le 2026-08-19 : une commande
 * réglée hors application n'est pas facturée du tout (voir ON_SITE plus bas).
 */

export type InvoiceResult =
    | { status: "invoiced"; invoiceId: string }
    | { status: "skipped"; reason: string }
    | { status: "failed"; reason: string };

export interface InvoiceOptions {
    /** Si fourni, doit égaler le total commande (garde-fou du webhook SumUp). */
    expectedAmountCents?: number;
    /** Email de repli (ex. personal_details.email du checkout SumUp). */
    fallbackEmail?: string;
}

/**
 * Facture une commande PAID : réclame -> lit la commande -> Invoice Ninja
 * (client, facture, paiement, email) -> marque facturée.
 *
 * En cas de panne transitoire (Invoice Ninja injoignable…), la réclamation est
 * relâchée et le statut `failed` est renvoyé pour que l'appelant relance.
 *
 * Le garde-fou ON_SITE vit ICI et pas dans les déclencheurs : les deux passent
 * par cette fonction, donc une règle unique les couvre tous les deux et il n'y
 * a pas de version divergente à maintenir. Il renvoie `skipped` et jamais
 * `failed` — le trigger Firestore tourne en `retry: true` et rejouerait
 * indéfiniment une décision qui ne changera pas.
 */
export async function createAndSendInvoice(
    orderId: string,
    opts: InvoiceOptions = {},
): Promise<InvoiceResult> {
    const claimed = await claimInvoice(orderId);
    if (!claimed) {
        // Une autre invocation détient la réclamation : ne PAS la relâcher.
        return { status: "skipped", reason: "déjà facturée / en cours" };
    }

    try {
        const orderSnap = await getFirestore().collection("orders").doc(orderId).get();
        if (!orderSnap.exists) {
            await releaseClaim(orderId);
            return { status: "skipped", reason: "commande introuvable" };
        }

        // Paiement hors application : la boutique encaisse au comptoir et ne veut
        // aucune facture émise. Testé AVANT les validations de montant et d'email
        // parce que la décision n'en dépend pas : une commande ON_SITE sans email
        // serait autrement classée « email client manquant », ce qui décrirait un
        // incident là où il n'y en a pas.
        //
        // Le critère est le mode de PAIEMENT, pas le mode de retrait : un client
        // qui réserve en click & collect mais règle dans l'application reçoit sa
        // facture comme n'importe quelle commande en ligne.
        //
        // La commande passe quand même à PAID — c'est ce que fait
        // MarkOrderPaidOnSiteUseCase quand la boutique encaisse — donc ce
        // déclenchement-ci est normal et attendu, pas une anomalie.
        if (orderSnap.get("payment_mode") === "ON_SITE") {
            await releaseClaim(orderId);
            return { status: "skipped", reason: "réglée hors application (ON_SITE)" };
        }

        const orderTotalCents = orderSnap.get("total_price");
        const email = orderSnap.get("customer_email") ?? opts.fallbackEmail;
        const customerName = orderSnap.get("customer_name") ?? "Client";
        const billingAddress = orderSnap.get("billing_address")
            ?? orderSnap.get("customer_address") ?? "";
        const deliveryDate = orderSnap.get("delivery_date");
        const products = (orderSnap.get("products") ?? {}) as Record<string, number>;

        if (typeof orderTotalCents !== "number" || orderTotalCents <= 0) {
            await releaseClaim(orderId);
            return { status: "skipped", reason: `total_price invalide (${orderTotalCents})` };
        }
        if (opts.expectedAmountCents != null && opts.expectedAmountCents !== orderTotalCents) {
            await releaseClaim(orderId);
            return {
                status: "skipped",
                reason: `montant incohérent (attendu=${opts.expectedAmountCents}, commande=${orderTotalCents})`,
            };
        }
        if (typeof email !== "string" || !email.includes("@")) {
            await releaseClaim(orderId);
            return { status: "skipped", reason: "email client manquant" };
        }

        const lines = await buildInvoiceLines(products, orderId, orderTotalCents);
        const invoiceNinja = new InvoiceNinjaClient(
            process.env.INVOICE_NINJA_URL ?? "",
            process.env.INVOICE_NINJA_TOKEN ?? "",
        );
        const invoiceReq = {
            email,
            clientName: customerName,
            billingAddress,
            orderId,
            lines,
            totalCents: orderTotalCents,
            deliveryDate,
        };
        const clientId = await invoiceNinja.findOrCreateClient(invoiceReq);
        const invoiceId = await invoiceNinja.createInvoice(clientId, invoiceReq);
        await invoiceNinja.markPaidAndEmail(invoiceId);

        await markInvoiced(orderId, invoiceId);
        return { status: "invoiced", invoiceId };
    } catch (error) {
        // Panne transitoire : on relâche la réclamation pour autoriser un retry.
        await releaseClaim(orderId).catch(() => undefined);
        return { status: "failed", reason: describeError(error) };
    }
}

/**
 * Réclame l'unique facture de cette commande via une transaction.
 * Renvoie true si acquise, false si déjà réclamée/facturée.
 */
async function claimInvoice(orderId: string): Promise<boolean> {
    const ref = getFirestore().collection("invoices").doc(orderId);
    return getFirestore().runTransaction(async (tx) => {
        const snap = await tx.get(ref);
        if (snap.exists) {
            return false;
        }
        tx.set(ref, {
            order_id: orderId,
            status: "processing",
            created_at: FieldValue.serverTimestamp(),
        });
        return true;
    });
}

/** Libère une réclamation (échec en cours de traitement) pour autoriser un retry. */
async function releaseClaim(orderId: string): Promise<void> {
    await getFirestore().collection("invoices").doc(orderId).delete();
}

/** Marque la commande comme facturée et envoyée. */
async function markInvoiced(orderId: string, invoiceNinjaId: string): Promise<void> {
    await getFirestore().collection("invoices").doc(orderId).set(
        {
            order_id: orderId,
            status: "sent",
            invoice_ninja_id: invoiceNinjaId,
            sent_at: FieldValue.serverTimestamp(),
        },
        { merge: true },
    );
}

/**
 * Construit les lignes de facture depuis la collection `products`.
 *
 * ATTENTION : dans la commande, la map `products` est indexée par NOM de
 * produit (`{ nomProduit -> quantité }`), PAS par id de document — c'est le
 * contrat écrit par le client (CheckoutViewModel) et lu par l'admin
 * (OrderPreparationScreen). On résout donc le prix en indexant la collection
 * `products` par son champ `name`, jamais via `.doc(cle)`.
 *
 * Repli : commande sans produits ou nom introuvable -> une seule ligne au
 * montant total débité (le total facturé reste exact).
 */
async function buildInvoiceLines(
    products: Record<string, number>,
    orderId: string,
    totalCents: number,
): Promise<InvoiceLine[]> {
    const entries = Object.entries(products);
    const fallback: InvoiceLine[] = [
        { description: `Commande FromDeBique #${orderId}`, unitPriceCents: totalCents, quantity: 1 },
    ];
    if (entries.length === 0) {
        return fallback;
    }

    // Index nom -> priceCents (la collection produits est petite : un shop).
    const db = getFirestore();
    const productsSnap = await db.collection("products").get();
    const priceByName = new Map<string, number>();
    productsSnap.forEach((doc) => {
        const name = doc.get("name");
        const priceCents = doc.get("priceCents");
        if (typeof name === "string" && typeof priceCents === "number") {
            priceByName.set(name, priceCents);
        }
    });

    const lines: InvoiceLine[] = [];
    let allResolved = true;
    entries.forEach(([name, quantity]) => {
        const priceCents = priceByName.get(name);
        if (priceCents === undefined) {
            allResolved = false;
            return;
        }
        lines.push({ description: name, unitPriceCents: priceCents, quantity });
    });

    if (!allResolved || lines.length === 0) {
        return fallback;
    }
    return lines;
}
