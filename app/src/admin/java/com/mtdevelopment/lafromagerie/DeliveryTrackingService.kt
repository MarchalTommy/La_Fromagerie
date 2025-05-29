package com.mtdevelopment.lafromagerie

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mtdevelopment.admin.domain.model.OptimizedRouteWithOrders
import com.mtdevelopment.admin.domain.usecase.DetermineNextDeliveryStopUseCase
import com.mtdevelopment.admin.domain.usecase.GetAllOrdersUseCase
import com.mtdevelopment.admin.domain.usecase.GetCurrentLocationUseCase
import com.mtdevelopment.admin.domain.usecase.GetOptimizedDeliveryUseCase
import com.mtdevelopment.admin.presentation.R.drawable
import com.mtdevelopment.core.domain.toStringDate
import com.mtdevelopment.core.model.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

class DeliveryTrackingService : Service(), KoinComponent {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    // Injection des UseCases
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase by inject()
    private val determineNextDeliveryStopUseCase: DetermineNextDeliveryStopUseCase by inject()
    private val getAllOrdersUseCase: GetAllOrdersUseCase by inject()
    private val getOptimizedDeliveryUseCase: GetOptimizedDeliveryUseCase by inject()

    private var todaysOrders: List<Order> = emptyList()
    private var optimizedRouteWaypoints: OptimizedRouteWithOrders = OptimizedRouteWithOrders(
        emptyList(), emptyList()
    )

    companion object {
        const val ACTION_STOP_UPDATE = "stopUpdate"
        const val NOTIFICATION_ID = 123
        const val NOTIFICATION_CHANNEL_ID = "delivery_tracking_channel"
        const val ACTION_STOP_SERVICE_FROM_NOTIFICATION = "stopService"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action == ACTION_STOP_SERVICE_FROM_NOTIFICATION) {
            stopSelf()
        } else {
            // startForeground doit être appelé dans les 5 secondes sur Android O+
            startForeground(NOTIFICATION_ID, createInitialNotification())
            fetchOrdersAndRoute()
        }

        return START_STICKY // Le service redémarrera s'il est tué par le système
    }

    private fun fetchOrdersAndRoute() {
        serviceScope.launch {
            try {
                getAllOrdersUseCase.invoke { ordersResult ->
                    val allOrders = ordersResult ?: emptyList()

                    val todayStr =
                        LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault())
                            .toInstant().toEpochMilli().toStringDate()

                    todaysOrders = allOrders.filter { it.deliveryDate == todayStr }
                    if (todaysOrders.isNotEmpty()) {
                        serviceScope.launch {
                            try {
                                val addresses = todaysOrders.map { it.customerAddress }

                                optimizedRouteWaypoints =
                                    getOptimizedDeliveryUseCase.invoke(addresses, todaysOrders)

                                if (optimizedRouteWaypoints.optimizedRoute.isNotEmpty()) {
                                    startLocationTracking()
                                } else {
                                    updateNotification(
                                        "Aucun itinéraire optimisé trouvé.",
                                        "Vérifiez les adresses des commandes."
                                    )
                                }
                            } catch (e: CancellationException) {
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
                    if (optimizedRouteWaypoints.optimizedOrders.isNotEmpty() && optimizedRouteWaypoints.optimizedRoute.isNotEmpty()) {
                        val nextStopOrder = determineNextDeliveryStopUseCase.invoke(
                            currentLocation,
                            optimizedRouteWaypoints
                        )

                        if (nextStopOrder != null) {
                            val customerName = nextStopOrder.customerName
                            val orderContent = formatOrderContent(nextStopOrder)
                            updateNotification("Prochain arrêt: $customerName", orderContent)
                        } else {
                            updateNotification(
                                "Dernière livraison effectuée ou itinéraire terminé.",
                                "À bientôt !"
                            )
                            stopSelf()
                        }
                    } else {
                        stopSelf()
                    }
                }
        }
    }

    private fun createInitialNotification(): Notification {
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
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags =
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlags)
        val smallIconRes = R.drawable.appicon_simpler
        val stopUpdateIntent = Intent(this, NotificationBroadcastReceiver::class.java).apply {
            action = ACTION_STOP_UPDATE
            putExtra(EXTRA_NOTIFICATION_ID, 0)
        }
        val stopUpdateIntentFlags =
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val stopUpdatingPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(this, 0, stopUpdateIntent, stopUpdateIntentFlags)

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setSmallIcon(smallIconRes)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                drawable.outline_cancel_24,
                "Arrêter le suivi",
                stopUpdatingPendingIntent
            )
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        // Vérifier la version d'Android (Oreo et plus)
        val channelName = "Suivi de Livraison"
        val channelDescription = "Notifications pour le suivi des livraisons en cours"
        val importance = NotificationManager.IMPORTANCE_HIGH

        val channel =
            NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, importance).apply {
                description = channelDescription
            }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun formatOrderContent(order: Order): String {
        return order.products.map { (productName, quantity) ->
            "$quantity x $productName"
        }.joinToString("\n")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}