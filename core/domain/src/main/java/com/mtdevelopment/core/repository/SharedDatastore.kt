package com.mtdevelopment.core.repository

import androidx.annotation.Keep
import com.mtdevelopment.core.model.CartItems
import com.mtdevelopment.core.model.UserInformation
import kotlinx.coroutines.flow.Flow

/**
 * Interface for the shared persistent storage of the application.
 * It manages global state across different modules, including:
 * - Shopping Cart items and total price.
 * - User profile/information (name, address, billing).
 * - Delivery preferences (date).
 * - Cache synchronization flags and timestamps for Firestore data.
 */
@Keep
interface SharedDatastore {

    /**
     * Flow of the current items in the shopping cart.
     */
    val cartItemsFlow: Flow<CartItems?>

    /**
     * Saves the provided cart items to persistent storage.
     */
    suspend fun setCartItems(cartItems: CartItems)

    /**
     * Empties the shopping cart in persistent storage.
     */
    suspend fun clearCartItems()

    /**
     * Flow of the current user profile information.
     */
    val userInformationFlow: Flow<UserInformation?>

    /**
     * Saves user profile details to persistent storage.
     */
    suspend fun setUserInformation(userInformation: UserInformation)

    /**
     * Clears all user-related profile information.
     */
    suspend fun clearUserInformation()

    /**
     * Flow of the selected delivery date (as a timestamp).
     */
    val deliveryDateFlow: Flow<Long>

    /**
     * Saves the selected delivery date.
     */
    suspend fun setDeliveryDate(date: Long)

    /**
     * Clears the selected delivery date.
     */
    suspend fun clearDeliveryDate()

    /**
     * Resets order-related state (usually combines clearing cart and delivery date).
     */
    suspend fun clearOrder()

    /**
     * Flow of the locally known timestamp for the last product update in Firestore.
     */
    val lastFirestoreProductsUpdate: Flow<Long>

    /**
     * Updates the local product update timestamp.
     */
    suspend fun lastFirestoreProductsUpdate(timestamp: Long)

    /**
     * Flow of the locally known timestamp for the last delivery path update in Firestore.
     */
    val lastFirestorePathsUpdate: Flow<Long>

    /**
     * Updates the local delivery path update timestamp.
     */
    suspend fun lastFirestorePathsUpdate(timestamp: Long)

    /**
     * Flow indicating if a full product cache refresh from the server is required.
     */
    val shouldRefreshProducts: Flow<Boolean>

    /**
     * Sets the flag indicating if products should be refreshed on the next load.
     */
    suspend fun setShouldRefreshProducts(shouldRefresh: Boolean)

    /**
     * Flow indicating if a full delivery path cache refresh from the server is required.
     */
    val shouldRefreshPaths: Flow<Boolean>

    /**
     * Sets the flag indicating if delivery paths should be refreshed on the next load.
     */
    suspend fun setShouldRefreshPaths(shouldRefresh: Boolean)

    /**
     * Completely wipes all data from the shared storage.
     */
    suspend fun clearAllDatastore()
}
