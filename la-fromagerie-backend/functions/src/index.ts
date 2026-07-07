import { onRequest } from "firebase-functions/v2/https";
import axios, { AxiosError } from "axios";

export const createSumUpCheckout = onRequest({ invoker: "public", cors: true }, async (req, res) => {
    // 1. Sécurité : On vérifie la méthode POST
    if (req.method !== "POST") {
        res.status(405).send("Méthode non autorisée");
        return;
    }

    const { amount, orderId } = req.body;

    // Récupération sécurisée du secret
    const sumUpSecretKey = process.env.SUMUP_SECRET_KEY;
    const sumUpMerchantCode = process.env.SUMUP_MERCHANT_CODE;

    try {
                // 2. Appel à l'API de SumUp
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
        // 3. Renvoi de la payment_url à l'application Android
        res.status(200).json({ payment_url: response.data.hosted_checkout_url });
        return;

    } catch (error) {
        const err = error as AxiosError;
        console.error("Erreur SumUp:", err.response?.data || err.message);
        res.status(500).send("Erreur lors de la création du paiement");
        return;
    }
});