package com.mtdevelopment.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreparationGroupTest {

    private fun order(
        id: String,
        deliveryDate: String = "05/07/2026",
        fulfillmentType: FulfillmentType = FulfillmentType.DELIVERY,
        pickupPointId: String? = null,
        pickupLabel: String? = null,
        products: Map<String, Int> = mapOf("Comté AOP" to 1)
    ) = Order(
        id = id,
        customerName = "Jean Dupont",
        customerAddress = "1 rue du Comté, 25300 Pontarlier",
        customerBillingAddress = "1 rue du Comté, 25300 Pontarlier",
        deliveryDate = deliveryDate,
        orderDate = "01/07/2026",
        products = products,
        status = OrderStatus.PAID,
        note = null,
        fulfillmentType = fulfillmentType,
        pickupPointId = pickupPointId,
        pickupLabel = pickupLabel
    )

    @Test
    fun `an order defaults to a paid-online delivery`() {
        // The default is what every order written before these fields existed reads as.
        val order = Order(
            id = "order-1",
            customerName = "Jean Dupont",
            customerAddress = "1 rue du Comté",
            customerBillingAddress = "1 rue du Comté",
            deliveryDate = "05/07/2026",
            orderDate = "01/07/2026",
            products = emptyMap(),
            status = OrderStatus.PAID,
            note = null
        )

        assertEquals(FulfillmentType.DELIVERY, order.fulfillmentType)
        assertEquals(PaymentMode.ONLINE, order.paymentMode)
    }

    @Test
    fun `an absent or unknown stored fulfillment type reads as a delivery`() {
        assertEquals(FulfillmentType.DELIVERY, FulfillmentType.fromStoredValue(null))
        assertEquals(FulfillmentType.DELIVERY, FulfillmentType.fromStoredValue(""))
        // A mode introduced by a future app version must degrade, not crash the list.
        assertEquals(FulfillmentType.DELIVERY, FulfillmentType.fromStoredValue("PICKUP_LOCKER"))
        assertEquals(
            FulfillmentType.PICKUP_MARKET,
            FulfillmentType.fromStoredValue("PICKUP_MARKET")
        )
    }

    @Test
    fun `an absent or unknown stored payment mode reads as online`() {
        assertEquals(PaymentMode.ONLINE, PaymentMode.fromStoredValue(null))
        assertEquals(PaymentMode.ONLINE, PaymentMode.fromStoredValue("CHEQUE"))
        assertEquals(PaymentMode.ON_SITE, PaymentMode.fromStoredValue("ON_SITE"))
    }

    @Test
    fun `a day mixing a tournee, a shop pickup and a market makes three batches`() {
        val orders = listOf(
            order("delivery-1"),
            order(
                "shop-1",
                fulfillmentType = FulfillmentType.PICKUP_SHOP,
                pickupPointId = "shop",
                pickupLabel = "La Fromagerie"
            ),
            order(
                "market-1",
                fulfillmentType = FulfillmentType.PICKUP_MARKET,
                pickupPointId = "market-pontarlier-0507",
                pickupLabel = "Marché de Pontarlier"
            )
        )

        val batches = orders.groupBy { it.preparationGroup }

        assertEquals(3, batches.size)
    }

    @Test
    fun `two markets on the same day are two batches`() {
        val orders = listOf(
            order(
                "market-1",
                fulfillmentType = FulfillmentType.PICKUP_MARKET,
                pickupPointId = "market-pontarlier-0507",
                pickupLabel = "Marché de Pontarlier"
            ),
            order(
                "market-2",
                fulfillmentType = FulfillmentType.PICKUP_MARKET,
                pickupPointId = "market-frasne-0507",
                pickupLabel = "Marché de Frasne"
            )
        )

        assertEquals(2, orders.groupBy { it.preparationGroup }.size)
    }

    /**
     * The label is a per-order snapshot taken at purchase, so renaming a market splits its
     * orders into a before and an after. While the label was part of the group's identity
     * that produced two batches on screen for one point — and statusIdFor keys only on the
     * point, so both shared one set of ticks: ticking "4 Comté" in either ticked the other.
     */
    @Test
    fun `two orders for one point with different labels form a single batch`() {
        val orders = listOf(
            order(
                "market-1",
                fulfillmentType = FulfillmentType.PICKUP_MARKET,
                pickupPointId = "market-pontarlier-0507",
                pickupLabel = "Marché de Pontarlier"
            ),
            order(
                "market-2",
                fulfillmentType = FulfillmentType.PICKUP_MARKET,
                pickupPointId = "market-pontarlier-0507",
                pickupLabel = "Grand marché de Pontarlier"
            )
        )

        val batches = orders.groupBy { it.preparationGroup }

        assertEquals(1, batches.size)
        assertEquals(2, batches.values.first().size)
    }

    @Test
    fun `deliveries of the same day still aggregate into one batch`() {
        val orders = listOf(order("delivery-1"), order("delivery-2"))

        assertEquals(1, orders.groupBy { it.preparationGroup }.size)
    }

    @Test
    fun `a delivery keeps the historical preparation status id`() {
        // Changing this format would orphan the ticks already stored in Firestore.
        val group = PreparationGroup(deliveryDate = "05/07/2026")

        assertEquals("05072026_ComtéAOP", group.statusIdFor("Comté AOP"))
    }

    @Test
    fun `two batches of one product on one day get different status ids`() {
        val delivery = PreparationGroup(deliveryDate = "05/07/2026")
        val market = PreparationGroup(
            deliveryDate = "05/07/2026",
            fulfillmentType = FulfillmentType.PICKUP_MARKET,
            pickupPointId = "market-pontarlier-0507"
        )
        val otherMarket = market.copy(pickupPointId = "market-frasne-0507")

        val ids = listOf(delivery, market, otherMarket).map { it.statusIdFor("Comté AOP") }

        assertEquals(3, ids.toSet().size)
        assertTrue(ids[1].startsWith("05072026_ComtéAOP_"))
    }

    @Test
    fun `deliveriesOnly keeps the tournee stops and drops every pickup`() {
        val orders = listOf(
            order("delivery-1"),
            order("shop-1", fulfillmentType = FulfillmentType.PICKUP_SHOP, pickupPointId = "shop"),
            order(
                "market-1",
                fulfillmentType = FulfillmentType.PICKUP_MARKET,
                pickupPointId = "market-1"
            ),
            order("delivery-2")
        )

        val routable = orders.deliveriesOnly()

        assertEquals(listOf("delivery-1", "delivery-2"), routable.map { it.id })
    }

    @Test
    fun `isPickup separates collected orders from driven ones`() {
        assertEquals(false, FulfillmentType.DELIVERY.isPickup)
        assertTrue(FulfillmentType.PICKUP_SHOP.isPickup)
        assertTrue(FulfillmentType.PICKUP_MARKET.isPickup)
    }
}
