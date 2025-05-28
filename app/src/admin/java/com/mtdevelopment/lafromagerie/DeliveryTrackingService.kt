package com.mtdevelopment.lafromagerie

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mtdevelopment.admin.domain.usecase.DetermineNextDeliveryStopUseCase
import com.mtdevelopment.admin.domain.usecase.GetAllOrdersUseCase
import com.mtdevelopment.admin.domain.usecase.GetCurrentLocationUseCase
import com.mtdevelopment.admin.domain.usecase.GetOptimizedDeliveryUseCase
import com.mtdevelopment.core.model.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

class DeliveryTrackingService : Service(), KoinComponent {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    // Injection des UseCases
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase by inject()
    private val determineNextDeliveryStopUseCase: DetermineNextDeliveryStopUseCase by inject()
    private val getAllOrdersUseCase: GetAllOrdersUseCase by inject()
    private val getOptimizedDeliveryUseCase: GetOptimizedDeliveryUseCase by inject()

    private var todaysOrders: List<Order> = emptyList()
    private var optimizedRouteWaypoints: List<Pair<Double, Double>> = emptyList()

    companion object {
        const val NOTIFICATION_ID = 123
        const val NOTIFICATION_CHANNEL_ID = "delivery_tracking_channel"
        private const val TAG = "DeliveryTrackingSvc" // Pour les logs
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "Service onBind")
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand - Intent: $intent")
        // startForeground doit être appelé dans les 5 secondes sur Android O+
        startForeground(NOTIFICATION_ID, createInitialNotification())
        Log.d(TAG, "Service started in foreground")

        fetchOrdersAndRoute()

        return START_STICKY // Le service redémarrera s'il est tué par le système
    }

    private fun fetchOrdersAndRoute() {
        serviceScope.launch {
            try {
                Log.d(TAG, "Fetching orders...")
                getAllOrdersUseCase.invoke { ordersResult ->
                    Log.d(TAG, "getAllOrdersUseCase callback. Orders: ${ordersResult?.size}")
                    val allOrders = ordersResult ?: emptyList()

                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val todayStr = dateFormat.format(Date())
                    todaysOrders = allOrders.filter { it.deliveryDate == todayStr }
                    Log.d(TAG, "Today's orders: ${todaysOrders.size}")

                    if (todaysOrders.isNotEmpty()) {
                        Log.d(
                            TAG,
                            "todaysOrders is not empty (${todaysOrders.size}). Preparing to fetch optimized route."
                        ) // LOG 1
                        Log.d(
                            TAG,
                            "ServiceScope isActive: ${serviceScope.isActive}, isCancelled: ${serviceScope.coroutineContext[Job]?.isCancelled}"
                        ) // AJOUTEZ CE LOG
                        serviceScope.launch { // Coroutine pour la route optimisée
                            Log.d(TAG, "Coroutine for optimized route fetch: LAUNCHED.") // LOG 2
                            try {
                                val addresses = todaysOrders.map { it.customerAddress }
                                Log.d(TAG, "Addresses for route: $addresses") // LOG 3
                                Log.d(TAG, "Calling getOptimizedDeliveryUseCase.invoke...") // LOG 4

                                optimizedRouteWaypoints =
                                    getOptimizedDeliveryUseCase.invoke(addresses) ?: emptyList()

                                Log.d(
                                    TAG,
                                    "getOptimizedDeliveryUseCase.invoke finished. Waypoints count: ${optimizedRouteWaypoints.size}"
                                ) // LOG 5

                                if (optimizedRouteWaypoints.isNotEmpty()) {
                                    startLocationTracking()
                                } else {
                                    Log.w(
                                        TAG,
                                        "Optimized route returned no waypoints."
                                    ) // LOG 6 (Warning)
                                    updateNotification(
                                        "Aucun itinéraire optimisé trouvé.",
                                        "Vérifiez les adresses des commandes."
                                    )
                                }
                            } catch (e: CancellationException) {
                                Log.w(
                                    TAG,
                                    "Coroutine for optimized route was cancelled.",
                                    e
                                ) // LOG 7 (Cancellation)
                                throw e // Re-throw cancellation exceptions
                            } catch (e: Exception) {
                                Log.e(
                                    TAG,
                                    "Error fetching optimized route in service",
                                    e
                                ) // LOG 8 (Error)
                                updateNotification(
                                    "Erreur de calcul d'itinéraire",
                                    e.message ?: "Erreur inconnue"
                                )
                            }
                        }
                    } else {
                        Log.d(TAG, "No deliveries for today.")
                        updateNotification(
                            "Aucune livraison pour aujourd'hui.",
                            "Revenez plus tard."
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in fetchOrdersAndRoute", e)
                updateNotification("Erreur de préparation", e.localizedMessage ?: "Détail inconnu")
            }
        }
    }


    private fun startLocationTracking() {
        Log.d(TAG, "Starting location tracking...")
        serviceScope.launch {
            getCurrentLocationUseCase()
                .catch { e ->
                    Log.e(
                        TAG,
                        "Error in location flow",
                        e
                    )
                } // Capter les erreurs du flow
                .collectLatest { currentLocation ->
                    Log.d(TAG, "Current location: $currentLocation")
                    if (todaysOrders.isNotEmpty() && optimizedRouteWaypoints.isNotEmpty()) {
                        val nextStopOrder = determineNextDeliveryStopUseCase.invoke(
                            currentLocation,
                            todaysOrders,
                            optimizedRouteWaypoints
                        )

                        if (nextStopOrder != null) {
                            val customerName = nextStopOrder.customerName
                            val orderContent = formatOrderContent(nextStopOrder)
                            Log.d(TAG, "Next stop: $customerName, Order: $orderContent")
                            updateNotification("Prochain arrêt: $customerName", orderContent)
                        } else {
                            Log.d(TAG, "No next stop determined (end of route or issue).")
                            updateNotification(
                                "Dernière livraison effectuée ou itinéraire terminé.",
                                ""
                            )
                            stopSelf()
                        }
                    } else {
                        Log.d(TAG, "Location update received but no orders/waypoints to process.")
                    }
                }
        }
    }

    private fun createInitialNotification(): Notification {
        Log.d(TAG, "Creating initial notification")
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags =
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlags)

        val smallIconRes = R.drawable.appicon_simpler

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Suivi de Livraison Actif")
            .setContentText("Calcul de l'itinéraire en cours...")
            .setSmallIcon(smallIconRes)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        Log.d(TAG, "Updating notification: Title='$title', Content='$content'")
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags =
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlags)
        val smallIconRes = R.drawable.appicon_simpler


        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setSmallIcon(smallIconRes)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "Notification updated/posted with ID: $NOTIFICATION_ID")
    }

    private fun createNotificationChannel() {
        // Vérifier la version d'Android (Oreo et plus)
        val channelName = "Suivi de Livraison"
        val channelDescription = "Notifications pour le suivi des livraisons en cours"
        val importance = NotificationManager.IMPORTANCE_LOW

        val channel =
            NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, importance).apply {
                description = channelDescription
            }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        Log.d(TAG, "Notification channel '$NOTIFICATION_CHANNEL_ID' created.")
    }

    private fun formatOrderContent(order: Order): String {
        return order.products.map { (productName, quantity) ->
            "$quantity x $productName"
        }.joinToString("\n")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "Service onDestroy, coroutines cancelled.")
    }
}