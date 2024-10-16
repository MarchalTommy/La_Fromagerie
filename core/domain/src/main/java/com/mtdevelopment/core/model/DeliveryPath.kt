package com.mtdevelopment.core.model

enum class DeliveryPath(val pathName: String, vararg val availableCities: String) {
    PATH_META(pathName = "Le Haut", "Labergement", "Métabief", "Saint-Antoine", "Rochejean"),
    PATH_SALIN(pathName = "Le Bas", "Salin", "Levier"),
    PATH_PON(pathName = "Pontarlier","Frasne", "Pontarlier", "Les Granges Narboz")
}