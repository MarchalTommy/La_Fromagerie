package com.mtdevelopment.core.model

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class OrderData(
    @SerialName("id")
    val id: String,
    @SerialName("customer_name")
    val customer_name: String,
    @SerialName("customer_email")
    val customer_email: String? = null,
    @SerialName("customer_address")
    val customer_address: String,
    @SerialName("billing_address")
    val billing_address: String,
    @SerialName("delivery_date")
    val delivery_date: String,
    @SerialName("order_date")
    val order_date: String,
    @SerialName("products")
    val products: Map<String, Int>,
    @SerialName("status")
    val status: OrderStatus,
    @SerialName("note")
    val note: String?,
    @SerialName("is_manually_added")
    val is_manually_added: Boolean?,
    @SerialName("total_price")
    val total_price: Long? = null,
    // Everything below is additive: the app is live, so orders written by older versions
    // arrive without these keys and must map to the defaults rather than fail.
    //
    // Property names are snake_case on purpose. Firestore's `set` uses its own reflective
    // POJO mapper, not kotlinx.serialization, so on the write path the Kotlin property
    // name *is* the document key and @SerialName has no say in it.
    @SerialName("fulfillment_type")
    val fulfillment_type: FulfillmentType = FulfillmentType.DELIVERY,
    @SerialName("payment_mode")
    val payment_mode: PaymentMode = PaymentMode.ONLINE,
    @SerialName("customer_phone")
    val customer_phone: String? = null,
    @SerialName("pickup_point_id")
    val pickup_point_id: String? = null,
    @SerialName("pickup_label")
    val pickup_label: String? = null,
    @SerialName("pickup_address")
    val pickup_address: String? = null,
    @SerialName("pickup_time_range")
    val pickup_time_range: String? = null
)

fun OrderData.toOrder(): Order {
    return Order(
        id = id,
        customerName = customer_name,
        customerEmail = customer_email,
        customerAddress = customer_address,
        customerBillingAddress = billing_address,
        deliveryDate = delivery_date,
        orderDate = order_date,
        products = products,
        status = status,
        note = note,
        isManuallyAdded = is_manually_added,
        totalPrice = total_price,
        fulfillmentType = fulfillment_type,
        paymentMode = payment_mode,
        customerPhone = customer_phone,
        pickupPointId = pickup_point_id,
        pickupLabel = pickup_label,
        pickupAddress = pickup_address,
        pickupTimeRange = pickup_time_range
    )
}

fun Order.toOrderData(): OrderData {
    return OrderData(
        id = id,
        customer_name = customerName,
        customer_email = customerEmail,
        customer_address = customerAddress,
        billing_address = customerBillingAddress,
        delivery_date = deliveryDate,
        order_date = orderDate,
        products = products,
        status = status,
        note = note,
        is_manually_added = isManuallyAdded,
        total_price = totalPrice,
        fulfillment_type = fulfillmentType,
        payment_mode = paymentMode,
        customer_phone = customerPhone,
        pickup_point_id = pickupPointId,
        pickup_label = pickupLabel,
        pickup_address = pickupAddress,
        pickup_time_range = pickupTimeRange
    )
}