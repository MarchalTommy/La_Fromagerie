package com.mtdevelopment.admin.data.repository

import com.mtdevelopment.admin.data.BuildConfig
import com.mtdevelopment.admin.data.model.toDataDeliveryPath
import com.mtdevelopment.admin.data.source.FirestoreAdminDatasource
import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.DeliveryPath
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.OrderStatus
import com.mtdevelopment.core.model.PreparationStatus
import com.mtdevelopment.core.model.toData
import com.mtdevelopment.core.model.toDomain
import com.mtdevelopment.core.model.toOrder
import com.mtdevelopment.core.model.toProductData
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Implementation of [FirebaseAdminRepository] that interacts with [FirestoreAdminDatasource].
 * This implementation ensures that every time a product or a delivery path is modified (added, updated, or deleted),
 * a global timestamp in the database is updated to signal changes to other clients.
 */
class FirebaseAdminRepositoryImpl(
    private val firestore: FirestoreAdminDatasource
) : FirebaseAdminRepository {

    ///////////////////////////////////////////////////////////////////////////
    // Product Management
    // Each modification triggers a call to saveNewDatabaseProductsUpdate.
    ///////////////////////////////////////////////////////////////////////////
    override suspend fun addNewProduct(product: com.mtdevelopment.core.model.Product): Result<Unit> {
        val result = firestore.addNewProduct(product = product.toProductData())
        var finalResult: Result<Unit>? = null

        result.onSuccess {
            // Signal database update for products
            finalResult = saveNewDatabaseProductsUpdate(System.currentTimeMillis())
        }

        return finalResult ?: result
    }

    override suspend fun updateProduct(product: com.mtdevelopment.core.model.Product): Result<Unit> {
        val result = firestore.updateProduct(product = product.toProductData())
        var finalResult: Result<Unit>? = null

        result.onSuccess {
            // Signal database update for products
            finalResult = saveNewDatabaseProductsUpdate(System.currentTimeMillis())
        }

        return finalResult ?: result
    }

    override suspend fun deleteProduct(product: com.mtdevelopment.core.model.Product): Result<Unit> {
        val result = firestore.deleteProduct(product = product.toProductData())
        var finalResult: Result<Unit>? = null

        result.onSuccess {
            // Signal database update for products
            finalResult = saveNewDatabaseProductsUpdate(System.currentTimeMillis())
        }

        return finalResult ?: result
    }

    //////////////////////////////////////////////////////////////////////////
    // Delivery Path Management
    // Each modification triggers a call to saveNewDatabasePathsUpdate.
    ///////////////////////////////////////////////////////////////////////////
    override suspend fun addNewDeliveryPath(path: DeliveryPath): Result<Unit> {
        val result = firestore.addNewDeliveryPath(path = path.toDataDeliveryPath())
        var finalResult: Result<Unit>? = null

        result.onSuccess {
            // Signal database update for delivery paths
            finalResult = saveNewDatabasePathsUpdate(System.currentTimeMillis())
        }

        return finalResult ?: result
    }

    override suspend fun updateDeliveryPath(path: DeliveryPath): Result<Unit> {
        val result = firestore.updateDeliveryPath(path = path.toDataDeliveryPath())
        var finalResult: Result<Unit>? = null

        result.onSuccess {
            // Signal database update for delivery paths
            finalResult = saveNewDatabasePathsUpdate(System.currentTimeMillis())
        }

        return finalResult ?: result
    }

    override suspend fun deleteDeliveryPath(path: DeliveryPath): Result<Unit> {
        val result = firestore.deleteDeliveryPath(path = path.toDataDeliveryPath())
        var finalResult: Result<Unit>? = null

        result.onSuccess {
            // Signal database update for delivery paths
            finalResult = saveNewDatabasePathsUpdate(System.currentTimeMillis())
        }

        return finalResult ?: result
    }

    ///////////////////////////////////////////////////////////////////////////
    // Order and Status Management
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Retrieves all orders and maps them from data models to domain models.
     */
    override suspend fun getAllOrders(onSuccess: (List<Order>?) -> Unit) {
        if (BuildConfig.DEBUG) {
            onSuccess.invoke(generateRandomOrders())
        } else {
            firestore.getAllOrders(onSuccess = { orders ->
                onSuccess.invoke(
                    orders.map {
                        it.toOrder()
                    }
                )
            }, onFailure = {
                onSuccess.invoke(
                    null
                )
            })
        }
    }

    private fun generateRandomOrders(): List<Order> {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val today = LocalDate.now()

        val dates = listOf(
            today.format(formatter),
            today.plusDays(1).format(formatter),
            today.plusDays(2).format(formatter),
            today.plusDays(3).format(formatter)
        )

        val firstNames = listOf(
            "Jean",
            "Pierre",
            "Marie",
            "Sophie",
            "Thomas",
            "Antoine",
            "Julie",
            "Nicolas",
            "Lucie",
            "François",
            "Émilie",
            "Guillaume",
            "Camille",
            "Julien",
            "Charlotte",
            "Marc",
            "Aurélie",
            "Paul",
            "Lucas",
            "Léa"
        )
        val lastNames = listOf(
            "Dupont",
            "Martin",
            "Dubois",
            "Lefebvre",
            "Moreau",
            "Laurent",
            "Simon",
            "Michel",
            "Garcia",
            "David",
            "Bertrand",
            "Roux",
            "Vincent",
            "Fournier",
            "Morel",
            "Girard",
            "Andre",
            "Lefevre",
            "Mercier",
            "Dupuis"
        )

        val addresses = listOf(
            "1 Rue de la Paix, 75002 Paris",
            "5 Rue Cuvier, 75005 Paris",
            "6 Place des Vosges, 75004 Paris",
            "5 Rue Crespin du Gast, 75011 Paris",
            "12 Avenue des Champs-Élysées, 75008 Paris",
            "24 Rue du Faubourg Saint-Antoine, 75012 Paris",
            "8 Rue de Lodi, 75006 Paris",
            "15 Rue de la Harpe, 75005 Paris",
            "47 Boulevard Saint-Michel, 75005 Paris",
            "10 Rue de la Pompe, 75016 Paris",
            "3 Place d'Italie, 75013 Paris",
            "18 Rue de Belleville, 75020 Paris",
            "55 Rue de Vaugirard, 75006 Paris",
            "30 Rue des Abbesses, 75018 Paris",
            "14 Rue Daguerre, 75014 Paris"
        )

        val productsList = listOf(
            "Comté AOP",
            "Reblochon AOP",
            "Morbier AOP",
            "Roquefort AOP",
            "Camembert de Normandie",
            "Brie de Meaux",
            "Chèvre Frais",
            "Tomme de Savoie",
            "Saint-Nectaire AOP",
            "Abondance AOP",
            "Lait Frais Bio (1L)",
            "Beurre de Baratte"
        )

        val notes = listOf(
            "Code d'entrée 1234. Laisser le colis devant la porte.",
            "Sonner chez le gardien si absent.",
            "Livraison avant midi si possible.",
            "Attention chien gentil mais curieux.",
            "Déposer sur la terrasse.",
            "Code 24B8. 3ème étage gauche.",
            null, null, null, null
        )

        val statuses = OrderStatus.values()

        val orders = mutableListOf<Order>()
        val orderCount = (12..20).random()
        for (i in 0 until orderCount) {
            val customerName = "${firstNames.random()} ${lastNames.random()}"
            val customerAddress = addresses.random()
            val deliveryDate = dates.random()
            val orderDate = today.minusDays((0..3).random().toLong()).format(formatter)

            val selectedProducts = mutableMapOf<String, Int>()
            val productCount = (1..4).random()
            val shuffledProducts = productsList.shuffled()
            for (p in 0 until productCount) {
                selectedProducts[shuffledProducts[p]] = (1..5).random()
            }

            orders.add(
                Order(
                    id = "order-debug-${1000 + i}",
                    customerName = customerName,
                    customerAddress = customerAddress,
                    customerBillingAddress = customerAddress,
                    deliveryDate = deliveryDate,
                    orderDate = orderDate,
                    products = selectedProducts,
                    status = statuses.random(),
                    note = notes.random(),
                    isManuallyAdded = false
                )
            )
        }

        return orders.sortedWith(compareByDescending<Order> { it.deliveryDate }.thenBy { it.customerName })
    }

    /**
     * Retrieves preparation statuses and maps them from data models to domain models.
     */
    override suspend fun getPreparationStatuses(onSuccess: (List<PreparationStatus>?) -> Unit) {
        val result = firestore.getPreparationStatuses()
        result.onSuccess { list ->
            onSuccess(list.map { it.toDomain() })
        }
        result.onFailure {
            onSuccess(null)
        }
    }

    /**
     * Updates a preparation status in the database.
     */
    override suspend fun updatePreparationStatus(status: PreparationStatus): Result<Unit> {
        return firestore.updatePreparationStatus(status.toData())
    }

    ///////////////////////////////////////////////////////////////////////////
    // Global Update Triggers
    ///////////////////////////////////////////////////////////////////////////
    override suspend fun saveNewDatabaseProductsUpdate(timestamp: Long): Result<Unit> {
        return firestore.saveNewDatabaseProductUpdate(timestamp)
    }

    override suspend fun saveNewDatabasePathsUpdate(timestamp: Long): Result<Unit> {
        return firestore.saveNewDatabasePathsUpdate(timestamp)
    }
}