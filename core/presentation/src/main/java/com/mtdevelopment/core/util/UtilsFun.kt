package com.mtdevelopment.core.util

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.compose.currentKoinScope
import java.text.NumberFormat
import java.util.Locale

fun Double.toCentsLong(): Long {
    return (this * 100).toLong()
}

fun Long.toPriceDouble(): Double {
    return this.toDouble() / 100
}

fun Long.toStringPrice(): String {
    val tempDouble = this / 100.0
    val format = NumberFormat.getCurrencyInstance(Locale.FRANCE)
    return format.format(tempDouble)
}

fun String.toLongPrice(): Long {
    return (this.replace("€", "").replace(",", ".").trim().toDouble() * 100).toLong()
}

fun vibratePhoneClick(context: Context) {
    if (Build.VERSION.SDK_INT < 34) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 30) {
            vibrator.vibrate(
                VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK).compose()
            )
        } else {
            vibrator.vibrate(50)
        }
    } else {
        val vibratorManager: VibratorManager by lazy {
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        }

        vibratorManager.vibrate(
            CombinedVibration.createParallel(
                VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK).compose()
            )
        )
    }
}

fun vibratePhoneClickBig(context: Context) {
    if (Build.VERSION.SDK_INT < 34) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 29) {
            vibrator.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            )
        } else {
            vibrator.vibrate(100)
        }
    } else {
        val vibratorManager: VibratorManager by lazy {
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        }

        vibratorManager.vibrate(
            CombinedVibration.createParallel(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            )
        )
    }
}

/*
Thanks to this article for that method :
https://medium.com/mercadona-tech/type-safety-in-navigation-compose-23c03e3d74a5
 */
inline fun <reified T : Any> serializableType(
    isNullableAllowed: Boolean = false,
    json: Json = Json,
) = object : NavType<T>(isNullableAllowed = isNullableAllowed) {
    override fun get(bundle: Bundle, key: String) =
        bundle.getString(key)?.let<String, T>(json::decodeFromString)

    override fun parseValue(value: String): T = json.decodeFromString(value)

    override fun serializeAsValue(value: T): String = json.encodeToString(value)

    override fun put(bundle: Bundle, key: String, value: T) {
        bundle.putString(key, json.encodeToString(value))
    }
}

@Composable
inline fun <reified T : ViewModel> koinViewModel(): T {
    val scope = currentKoinScope()
    return viewModel {
        scope.get<T>()
    }
}