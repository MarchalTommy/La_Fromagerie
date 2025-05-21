package com.mtdevelopment.core.model

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Keep
data class OrderData(
    @SerialName("id")
    val id: String,
    @SerialName("customer_name")
    val customerName: String,
    @SerialName("customer_address")
    val customerAddress: String,
    @SerialName("delivery_date")
    val deliveryDate: String,
    @SerialName("order_date")
    val orderDate: String,
    @SerialName("products")
    val products: Map<String, Int>,
    @SerialName("status")
    val status: OrderStatus
)

fun OrderData.toOrder(): Order {
    return Order(
        id = id,
        customerName = customerName,
        customerAddress = customerAddress,
        deliveryDate = deliveryDate,
        orderDate = orderDate,
        products = products,
        status = status
    )
}

fun Order.toOrderData(): OrderData {
    return OrderData(
        id = id,
        customerName = customerName,
        customerAddress = customerAddress,
        deliveryDate = deliveryDate,
        orderDate = orderDate,
        products = products,
        status = status
    )
}