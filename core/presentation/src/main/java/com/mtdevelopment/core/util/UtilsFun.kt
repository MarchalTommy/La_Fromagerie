package com.mtdevelopment.core.util

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.navigation.NavType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
Thanks to this article for that thing :
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