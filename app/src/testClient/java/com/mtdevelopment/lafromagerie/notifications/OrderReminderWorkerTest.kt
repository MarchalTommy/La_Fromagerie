package com.mtdevelopment.lafromagerie.notifications

import com.mtdevelopment.core.model.FulfillmentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers only the wording, which is where the bug was: the reminder told a customer coming
 * to collect to stay reachable for a delivery, and never said where or when.
 *
 * Lives in the client source set because the worker does — the admin flavor binds a no-op
 * scheduler and has nobody to remind.
 */
class OrderReminderWorkerTest {

    @Test
    fun `a delivery keeps the wording it already had`() {
        assertEquals(
            "Votre commande arrive aujourd'hui",
            OrderReminderWorker.titleFor(FulfillmentType.DELIVERY)
        )
        assertEquals(
            "Votre commande La Fromagerie est prévue pour aujourd'hui. " +
                    "Pensez à rester joignable pour la livraison !",
            OrderReminderWorker.bodyFor(
                fulfillmentType = FulfillmentType.DELIVERY,
                pickupLabel = "Marché de Pontarlier",
                pickupTimeRange = "8h-13h"
            )
        )
    }

    @Test
    fun `a collection names the point and its opening window`() {
        assertEquals(
            "Votre commande est à retirer aujourd'hui",
            OrderReminderWorker.titleFor(FulfillmentType.PICKUP_MARKET)
        )
        assertEquals(
            "Votre commande La Fromagerie est à retirer aujourd'hui — " +
                    "Marché de Pontarlier, 8h-13h.",
            OrderReminderWorker.bodyFor(
                fulfillmentType = FulfillmentType.PICKUP_MARKET,
                pickupLabel = "Marché de Pontarlier",
                pickupTimeRange = "8h-13h"
            )
        )
    }

    @Test
    fun `a collection with no opening window still names the point`() {
        assertEquals(
            "Votre commande La Fromagerie est à retirer aujourd'hui — La Fromagerie.",
            OrderReminderWorker.bodyFor(
                fulfillmentType = FulfillmentType.PICKUP_SHOP,
                pickupLabel = "La Fromagerie",
                pickupTimeRange = "   "
            )
        )
    }

    /**
     * An order placed before the snapshot existed must still get a reminder — vague is
     * acceptable, a promise of delivery is not.
     */
    @Test
    fun `a collection with no snapshot falls back without mentioning a delivery`() {
        val body = OrderReminderWorker.bodyFor(
            fulfillmentType = FulfillmentType.PICKUP_SHOP,
            pickupLabel = null,
            pickupTimeRange = null
        )

        assertEquals("Votre commande La Fromagerie est prête pour aujourd'hui.", body)
        assertFalse(body.contains("livraison"))
        assertTrue(OrderReminderWorker.titleFor(FulfillmentType.PICKUP_SHOP).contains("retirer"))
    }
}
