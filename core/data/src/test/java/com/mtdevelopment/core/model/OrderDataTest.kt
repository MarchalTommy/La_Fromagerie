package com.mtdevelopment.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrderDataTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun orderData() = OrderData(
        id = "order-1",
        customer_name = "Jean Dupont",
        customer_address = "1 rue du Comté, 25300 Pontarlier",
        billing_address = "1 rue du Comté, 25300 Pontarlier",
        delivery_date = "05/07/2026",
        order_date = "01/07/2026",
        products = mapOf("Comté AOP" to 2),
        status = OrderStatus.PAID,
        note = null,
        is_manually_added = false
    )

    @Test
    fun `a document without the pickup fields decodes as a paid-online delivery`() {
        // The shape every order written before this change has, and the one older app
        // versions keep writing. It must decode, not blow up on missing keys.
        val stored = """
            {
              "id": "order-1",
              "customer_name": "Jean Dupont",
              "customer_address": "1 rue du Comté",
              "billing_address": "1 rue du Comté",
              "delivery_date": "05/07/2026",
              "order_date": "01/07/2026",
              "products": { "Comté AOP": 2 },
              "status": "PAID",
              "note": null,
              "is_manually_added": false
            }
        """.trimIndent()

        val decoded = json.decodeFromString<OrderData>(stored)

        assertEquals(FulfillmentType.DELIVERY, decoded.fulfillment_type)
        assertEquals(PaymentMode.ONLINE, decoded.payment_mode)
        assertNull(decoded.customer_phone)
        assertNull(decoded.pickup_point_id)
        assertNull(decoded.pickup_label)
        assertNull(decoded.pickup_address)
        assertNull(decoded.pickup_time_range)
    }

    @Test
    fun `a pickup order round-trips through the domain model`() {
        val data = orderData().copy(
            fulfillment_type = FulfillmentType.PICKUP_MARKET,
            payment_mode = PaymentMode.ON_SITE,
            customer_phone = "0601020304",
            pickup_point_id = "market-pontarlier-0507",
            pickup_label = "Marché de Pontarlier",
            pickup_address = "Place Saint-Bénigne, 25300 Pontarlier",
            pickup_time_range = "8h-13h"
        )

        val restored = data.toOrder().toOrderData()

        assertEquals(data, restored)
    }

    @Test
    fun `the domain mapping carries the pickup snapshot both ways`() {
        val order = orderData().copy(
            fulfillment_type = FulfillmentType.PICKUP_SHOP,
            pickup_point_id = "shop",
            pickup_label = "La Fromagerie",
            pickup_address = "3 Grande Rue, 25300 Pontarlier",
            pickup_time_range = "9h-12h / 15h-19h"
        ).toOrder()

        assertEquals(FulfillmentType.PICKUP_SHOP, order.fulfillmentType)
        assertEquals("shop", order.pickupPointId)
        assertEquals("La Fromagerie", order.pickupLabel)
        assertEquals("3 Grande Rue, 25300 Pontarlier", order.pickupAddress)
        assertEquals("9h-12h / 15h-19h", order.pickupTimeRange)
    }

    @Test
    fun `a delivery order still round-trips unchanged`() {
        val data = orderData()

        assertEquals(data, data.toOrder().toOrderData())
    }
}
