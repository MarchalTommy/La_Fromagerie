import { onRequest } from "firebase-functions/v2/https";
import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import axios, { AxiosError } from "axios";

initializeApp();

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

    try {
                // 3. Appel à l'API de SumUp
        const response = await axios.post(
            "https://api.sumup.com/v0.1/checkouts",
            {
                amount: amount,
                currency: "EUR",
                checkout_reference: orderId,
                redirect_url: "lafromagerie://checkout-callback",
                merchant_code: sumUpMerchantCode,
                hosted_checkout: {
                    enabled: true
                }
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
