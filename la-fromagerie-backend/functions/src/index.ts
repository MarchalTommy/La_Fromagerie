import { onRequest } from "firebase-functions/v2/https";
import { onDocumentUpdated } from "firebase-functions/v2/firestore";
import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { createHmac, timingSafeEqual } from "crypto";
import axios, { AxiosError } from "axios";
import { createAndSendInvoice } from "./billing.js";
import { describeError } from "./invoiceNinja.js";

initializeApp();

const SUMUP_API = "https://api.sumup.com/v0.1";

export const createSumUpCheckout = onRequest({ invoker: "public", cors: true }, async (req, res) => {
    // 1. Sécurité : On vérifie la méthode POST
    if (req.method !== "POST") {
        res.status(405).send("Méthode non autorisée");
        return;
    }

    const { orderId } = req.body;

    if (typeof orderId !== "string" || orderId.trim() === "") {
        res.status(400).send("orderId manquant");
        return;
    }

    // 2. Intégrité du montant : on lit le total depuis la commande Firestore.
    // Le montant envoyé par le client est ignoré — un client modifié pourrait
    // sinon créer un checkout à 0,01 € pour sa propre référence de commande.
    const orderSnap = await getFirestore().collection("orders").doc(orderId).get();
    const totalPriceCents = orderSnap.get("total_price");
    if (!orderSnap.exists || typeof totalPriceCents !== "number" || totalPriceCents <= 0) {
        console.error(`Commande introuvable ou total_price invalide pour ${orderId}`);
        res.status(400).send("Commande invalide");
        return;
    }
    const amount = Number((totalPriceCents / 100).toFixed(2));

    // Récupération sécurisée du secret
    const sumUpSecretKey = process.env.SUMUP_SECRET_KEY;
    const sumUpMerchantCode = process.env.SUMUP_MERCHANT_CODE;

    // Abonnement webhook : si SUMUP_WEBHOOK_URL est défini, SumUp notifiera
    // handleSumUpWebhook des changements de statut de ce checkout (return_url).
    // Distinct de redirect_url (retour navigateur vers l'app). No-op si non défini.
    const webhookUrl = process.env.SUMUP_WEBHOOK_URL;

    try {
        // 3. Appel à l'API de SumUp
        const response = await axios.post(
            `${SUMUP_API}/checkouts`,
            {
                amount: amount,
                currency: "EUR",
                checkout_reference: orderId,
                redirect_url: "lafromagerie://checkout-callback",
                merchant_code: sumUpMerchantCode,
                hosted_checkout: {
                    enabled: true
                },
                ...(webhookUrl ? { return_url: webhookUrl } : {})
            },
            {
                headers: {
                    "Authorization": `Bearer ${sumUpSecretKey}`,
                    "Content-Type": "application/json"
                }
            }
        );

        console.log("SumUp Response:", response.data);
        // 4. Renvoi de la payment_url à l'application Android
        res.status(200).json({ payment_url: response.data.hosted_checkout_url });
        return;

    } catch (error) {
        const err = error as AxiosError;
        console.error("Erreur SumUp:", err.response?.data || err.message);
        res.status(500).send("Erreur lors de la création du paiement");
        return;
    }
});

/**
 * Facturation primaire : dès qu'une commande passe à PAID dans Firestore, on
 * émet la facture Invoice Ninja. Couvre TOUS les chemins de paiement (Google
 * Pay direct ET hosted-checkout), car les deux écrivent `status = PAID` sur la
 * commande (avec le filet WorkManager). La commande fait foi : l'app n'écrit
 * PAID qu'après avoir vérifié le paiement auprès de SumUp.
 *
 * `retry: true` : en cas de panne transitoire d'Invoice Ninja, on relève une
 * erreur pour que le trigger soit rejoué (idempotent via la réclamation).
 */
export const onOrderPaidCreateInvoice = onDocumentUpdated(
    { document: "orders/{orderId}", retry: true },
    async (event) => {
        const before = event.data?.before.data();
        const after = event.data?.after.data();
        if (!after) {
            return;
        }

        // On ne déclenche que sur la TRANSITION vers PAID (pas les mises à jour
        // ultérieures : IN_PREPARATION, DELIVERED…).
        const becamePaid = after.status === "PAID" && before?.status !== "PAID";
        if (!becamePaid) {
            return;
        }

        const orderId = event.params.orderId;
        const result = await createAndSendInvoice(orderId);

        if (result.status === "invoiced") {
            console.log(`Facture ${result.invoiceId} envoyée pour la commande ${orderId} (trigger Firestore).`);
            return;
        }
        if (result.status === "skipped") {
            console.log(`Commande ${orderId} non facturée (trigger Firestore) : ${result.reason}.`);
            return;
        }
        // Panne transitoire : on relève pour rejouer le trigger.
        throw new Error(`Facturation commande ${orderId} échouée : ${result.reason}`);
    },
);

/**
 * Webhook SumUp -> facturation Invoice Ninja (défense en profondeur pour le
 * chemin hosted-checkout : SumUp ré-émet le webhook en cas d'échec, ce que le
 * trigger Firestore ne fait pas si l'app n'écrit jamais PAID).
 *
 * SumUp appelle cette fonction sur tout changement de statut d'un checkout
 * abonné (return_url). Le payload est minimal : `{ event_type, id }`. On ne lui
 * fait donc PAS confiance : on relit le checkout via l'API SumUp, on vérifie
 * PAID + montant, puis on délègue à la logique de facturation partagée
 * (idempotente : ne double-facture jamais avec le trigger Firestore).
 */
export const handleSumUpWebhook = onRequest({ invoker: "public" }, async (req, res) => {
    if (req.method !== "POST") {
        res.status(405).send("Méthode non autorisée");
        return;
    }

    // Vérification de signature (optionnelle). SumUp ne documente pas
    // formellement de signature pour les webhooks checkout ; si un secret est
    // configuré on rejette toute requête mal signée, sinon on s'appuie sur la
    // re-vérification via l'API SumUp (recommandation officielle SumUp).
    const webhookSecret = process.env.SUMUP_WEBHOOK_SECRET;
    if (webhookSecret) {
        if (!verifySignature(req.rawBody, req.get("x-payload-signature"), webhookSecret)) {
            console.warn("Webhook SumUp : signature invalide, requête rejetée.");
            res.status(401).send("Signature invalide");
            return;
        }
    } else {
        console.warn("SUMUP_WEBHOOK_SECRET non défini : signature non vérifiée (fallback = re-vérification API).");
    }

    const checkoutId = req.body?.id;
    const eventType = req.body?.event_type;
    if (typeof checkoutId !== "string" || checkoutId.trim() === "") {
        res.status(400).send("id de checkout manquant");
        return;
    }
    console.log(`Webhook SumUp reçu : event=${eventType} checkout=${checkoutId}`);

    try {
        // On relit le checkout via l'API SumUp (jamais le payload).
        const sumUpSecretKey = process.env.SUMUP_SECRET_KEY;
        const checkout = await axios.get(`${SUMUP_API}/checkouts/${checkoutId}`, {
            headers: { "Authorization": `Bearer ${sumUpSecretKey}` },
        });
        const status = checkout.data?.status;
        const orderId = checkout.data?.checkout_reference;
        const paidAmountCents = Math.round(Number(checkout.data?.amount) * 100);
        const emailFromSumUp = checkout.data?.personal_details?.email;

        // On ne facture que les paiements aboutis. Tout autre statut : on acquitte.
        if (status !== "PAID") {
            console.log(`Checkout ${checkoutId} status=${status} — rien à facturer.`);
            res.status(200).send("OK");
            return;
        }
        if (typeof orderId !== "string" || orderId.trim() === "") {
            console.error(`Checkout ${checkoutId} PAID mais checkout_reference absent.`);
            res.status(200).send("OK (référence de commande absente)");
            return;
        }

        const result = await createAndSendInvoice(orderId, {
            expectedAmountCents: paidAmountCents,
            fallbackEmail: emailFromSumUp,
        });

        if (result.status === "failed") {
            // Réponse 5xx => SumUp réessaiera la livraison du webhook.
            console.error(`Webhook facturation commande ${orderId} échouée : ${result.reason}`);
            res.status(500).send("Erreur de traitement");
            return;
        }
        console.log(`Webhook facturation commande ${orderId} : ${result.status} (${eventType}).`);
        res.status(200).send("OK");
        return;
    } catch (error) {
        console.error("Erreur webhook facturation :", describeError(error));
        res.status(500).send("Erreur de traitement");
        return;
    }
});

/** Vérifie la signature HMAC-SHA256 du corps brut contre l'en-tête fourni. */
function verifySignature(rawBody: Buffer, signatureHeader: string | undefined, secret: string): boolean {
    if (!signatureHeader) {
        return false;
    }
    const expected = createHmac("sha256", secret).update(rawBody).digest("hex");
    const a = Buffer.from(expected);
    const b = Buffer.from(signatureHeader);
    if (a.length !== b.length) {
        return false;
    }
    return timingSafeEqual(a, b);
}
