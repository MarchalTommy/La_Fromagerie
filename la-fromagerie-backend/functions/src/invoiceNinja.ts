import axios, { AxiosError, AxiosInstance } from "axios";

/**
 * Petit client pour l'API Invoice Ninja v5 (self-hosted).
 * Auth : header `X-Api-Token` (v5 ; c'était `X-Ninja-Token` en v4).
 * Les montants Invoice Ninja sont en unités monétaires (euros), PAS en centimes.
 */

export interface InvoiceLine {
    /** Libellé affiché sur la facture (nom du produit). */
    description: string;
    /** Prix unitaire en CENTIMES (converti en euros au moment de l'envoi). */
    unitPriceCents: number;
    quantity: number;
}

export interface InvoiceRequest {
    email: string;
    clientName: string;
    billingAddress: string;
    orderId: string;
    lines: InvoiceLine[];
    /** Montant total réellement débité (centimes) — fait foi sur le total facturé. */
    totalCents: number;
    deliveryDate?: string;
}

/** Centimes -> euros, 2 décimales (les montants ont au plus 2 décimales). */
function centsToUnits(cents: number): number {
    return Number((cents / 100).toFixed(2));
}

/**
 * Récapitulatif client pour `public_notes` : un produit par ligne (`\n` ->
 * `<br>` via nl2br côté Invoice Ninja), puis la date de livraison en dernier,
 * séparée par une ligne vide. Dérivé des lignes déjà envoyées dans
 * `line_items` — sans le total ni le numéro de facture (l'email les gère à
 * part). Ce texte apparaît dans l'email de confirmation ET sur le PDF.
 */
function buildPublicNotes(req: InvoiceRequest): string {
    const blocks: string[] = [];
    if (req.lines.length > 0) {
        blocks.push(
            req.lines.map((l) => `${l.quantity} × ${l.description}`).join("\n"),
        );
    }
    if (req.deliveryDate) {
        blocks.push(`Livraison prévue le ${req.deliveryDate}`);
    }
    return blocks.join("\n\n");
}

/** Id ISO numérique du pays France dans Invoice Ninja. */
const COUNTRY_ID_FRANCE = 250;

interface ParsedAddress {
    address1: string;
    postalCode?: string;
    city?: string;
}

/**
 * Découpe une adresse française mono-ligne (le seul format stocké sur la
 * commande : le `label` renvoyé par l'API adresse gouv, ex.
 * « 12 rue des Fromagers 69001 Lyon ») en rue / code postal / ville, pour
 * alimenter les champs structurés du client Invoice Ninja (bloc destinataire
 * du PDF).
 *
 * Le code postal FR (5 chiffres) sert de point de coupure. Repli : si aucun
 * code postal n'est trouvé, toute la chaîne va dans `address1` (comportement
 * historique) — mieux vaut une adresse brute qu'une adresse vide.
 */
function parseFrenchAddress(raw: string): ParsedAddress {
    const trimmed = raw.trim();
    const match = trimmed.match(/^(.*\S)[,\s]+(\d{5})[,\s]+(.+)$/);
    if (!match) {
        return { address1: trimmed };
    }
    const [, street, postalCode, city] = match;
    return { address1: street.trim(), postalCode, city: city.trim() };
}

export class InvoiceNinjaClient {
    private readonly http: AxiosInstance;

    constructor(baseUrl: string, token: string) {
        this.http = axios.create({
            baseURL: `${baseUrl.replace(/\/+$/, "")}/api/v1`,
            headers: {
                "X-Api-Token": token,
                "X-Requested-With": "XMLHttpRequest",
                "Content-Type": "application/json",
            },
            timeout: 20000,
        });
    }

    /** Trouve un client par email, sinon le crée. Renvoie l'id Invoice Ninja. */
    async findOrCreateClient(req: InvoiceRequest): Promise<string> {
        const existing = await this.http.get("/clients", {
            params: { email: req.email, per_page: 1 },
        });
        const found = existing.data?.data?.[0]?.id as string | undefined;
        if (found) {
            return found;
        }

        const addr = parseFrenchAddress(req.billingAddress);
        const created = await this.http.post("/clients", {
            name: req.clientName,
            address1: addr.address1,
            postal_code: addr.postalCode,
            city: addr.city,
            country_id: COUNTRY_ID_FRANCE,
            contacts: [
                { first_name: req.clientName, email: req.email, send_email: true },
            ],
        });
        const id = created.data?.data?.id as string | undefined;
        if (!id) {
            throw new Error("Invoice Ninja: id client manquant dans la réponse de création");
        }
        return id;
    }

    /**
     * Crée une facture (statut brouillon) pour le client.
     * Ajoute une ligne de réconciliation si la somme des lignes ne correspond
     * pas au montant réellement débité (livraison, arrondis, remises…), afin
     * que le total de la facture soit TOUJOURS égal au montant encaissé.
     */
    async createInvoice(clientId: string, req: InvoiceRequest): Promise<string> {
        const lineItems = req.lines.map((l) => ({
            product_key: l.description,
            notes: l.description,
            cost: centsToUnits(l.unitPriceCents),
            quantity: l.quantity,
        }));

        const linesSum = req.lines.reduce(
            (acc, l) => acc + l.unitPriceCents * l.quantity,
            0,
        );
        const diffCents = req.totalCents - linesSum;
        if (diffCents !== 0) {
            const label = diffCents > 0 ? "Livraison / frais" : "Remise";
            lineItems.push({
                product_key: label,
                notes: label,
                cost: centsToUnits(diffCents),
                quantity: 1,
            });
        }

        const res = await this.http.post("/invoices", {
            client_id: clientId,
            po_number: req.orderId,
            date: new Date().toISOString().slice(0, 10),
            public_notes: buildPublicNotes(req),
            line_items: lineItems,
        });
        const id = res.data?.data?.id as string | undefined;
        if (!id) {
            throw new Error("Invoice Ninja: id facture manquant dans la réponse de création");
        }
        return id;
    }

    /** Action groupée Invoice Ninja (`mark_paid`, `email`, …). */
    private async bulk(action: string, ids: string[]): Promise<void> {
        await this.http.post("/invoices/bulk", { action, ids });
    }

    /** Marque la facture payée (crée le paiement côté IN) puis l'envoie par email. */
    async markPaidAndEmail(invoiceId: string): Promise<void> {
        await this.bulk("mark_paid", [invoiceId]);
        await this.bulk("email", [invoiceId]);
    }
}

/** Message d'erreur lisible pour les logs (axios ou autre). */
export function describeError(error: unknown): string {
    const err = error as AxiosError;
    if (err.response) {
        return `HTTP ${err.response.status} ${JSON.stringify(err.response.data)}`;
    }
    return err.message ?? String(error);
}
