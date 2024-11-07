package com.mtdevelopment.core.model

enum class DeliveryPath(val pathName: String, val mapStyle: String, vararg val availableCities: String) {
    PATH_META(pathName = "Le Haut",
        mapStyle = "mapbox://styles/marchaldevelopment/cm1te6xn5018j01qphimw2wuz",
        "Labergement", "Métabief", "Saint-Antoine", "Rochejean"),
    PATH_SALIN(pathName = "Le Bas",
        mapStyle = "mapbox://styles/marchaldevelopment/cm1te6tb700om01pl70i6avlk",
        "Salin", "Levier"),
    PATH_PON(pathName = "Pontarlier",
        mapStyle = "mapbox://styles/marchaldevelopment/cm1teahes00xi01qrghgr91ku",
        "Frasne", "Pontarlier", "Les Granges Narboz")
}