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
    @SerialName("customer_address")
    val customer_address: String,
    @SerialName("delivery_date")
    val delivery_date: String,
    @SerialName("order_date")
    val order_date: String,
    @SerialName("products")
    val products: Map<String, Int>,
    @SerialName("status")
    val status: OrderStatus,
    @SerialName("note")
    val note: String?
)

fun OrderData.toOrder(): Order {
    return Order(
        id = id,
        customerName = customer_name,
        customerAddress = customer_address,
        deliveryDate = delivery_date,
        orderDate = order_date,
        products = products,
        status = status,
        note = note
    )
}

fun Order.toOrderData(): OrderData {
    return OrderData(
        id = id,
        customer_name = customerName,
        customer_address = customerAddress,
        delivery_date = deliveryDate,
        order_date = orderDate,
        products = products,
        status = status,
        note = note
    )
}