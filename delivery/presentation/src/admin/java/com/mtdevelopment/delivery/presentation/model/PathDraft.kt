package com.mtdevelopment.delivery.presentation.model

import com.mtdevelopment.admin.presentation.model.AdminUiDeliveryPath
import com.mtdevelopment.core.domain.move
import com.mtdevelopment.core.domain.normalizeCityName
import com.mtdevelopment.core.model.DeliveryCity
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * The path being edited on [com.mtdevelopment.delivery.presentation.screen.PathEditScreen], before
 * it is saved.
 *
 * Separate from [AdminUiDeliveryPath] for two reasons: it knows whether it is a creation or an edit
 * (the list screen offers both through the same card carousel), and it is `@Serializable` so the
 * whole draft survives a rotation or a process death through `rememberSaveable`. The edit screen
 * has no ViewModel of its own; a Koin definition would be a runtime crash waiting to happen for
 * state that is purely local to one screen.
 *
 * Every mutation below is a pure function returning a new draft, so the reordering and street rules
 * are unit-testable without Compose.
 *
 * @property isNew Drives the delete affordance, which must not appear while creating a path.
 */
@Serializable
data class PathDraft(
    val id: String,
    val name: String,
    val cities: List<DeliveryCity> = emptyList(),
    val deliveryDay: String = "",
    val deliveryFrequency: String = "WEEKLY",
    val isNew: Boolean
)

enum class MoveDirection { UP, DOWN }

/** Name a brand-new path starts with, so the field is never empty on arrival. */
const val NEW_PATH_DEFAULT_NAME = "Nouveau Parcours"

/**
 * Opens an existing path for editing, or starts a new one when [this] is null.
 *
 * The id of a new path is a placeholder: Firestore assigns the real document id on `add()` and the
 * read side overwrites this field with it.
 */
fun AdminUiDeliveryPath?.toDraft(): PathDraft = if (this == null) {
    PathDraft(
        id = UUID.randomUUID().toString(),
        name = NEW_PATH_DEFAULT_NAME,
        isNew = true
    )
} else {
    PathDraft(
        id = id,
        name = name,
        cities = cities,
        deliveryDay = deliveryDay,
        deliveryFrequency = deliveryFrequency,
        isNew = false
    )
}

fun PathDraft.toAdminUiDeliveryPath(): AdminUiDeliveryPath = AdminUiDeliveryPath(
    id = id,
    name = name,
    cities = cities,
    deliveryDay = deliveryDay,
    deliveryFrequency = deliveryFrequency
)

/**
 * A path with no name, no city or no delivery day cannot serve anyone, and the shop would have no
 * way to tell it apart in the carousel.
 */
val PathDraft.canBeSaved: Boolean
    get() = name.isNotBlank() && cities.isNotEmpty() && deliveryDay.isNotBlank()

/** Adding a city already on the path is a no-op rather than a duplicate stop. */
fun PathDraft.withCityAdded(city: DeliveryCity): PathDraft {
    val alreadyThere = cities.any {
        it.name.normalizeCityName() == city.name.normalizeCityName() && it.postcode == city.postcode
    }
    return if (alreadyThere) this else copy(cities = cities + city)
}

fun PathDraft.withCityRemovedAt(index: Int): PathDraft =
    if (index !in cities.indices) this
    else copy(cities = cities.filterIndexed { i, _ -> i != index })

/**
 * Moves a city one step. The order is load-bearing — it is the order the van drives, and the
 * coordinates cached alongside the path are aligned with it positionally — so an out-of-bounds move
 * must leave the list untouched rather than wrap around.
 */
fun PathDraft.withCityMoved(index: Int, direction: MoveDirection): PathDraft {
    val target = if (direction == MoveDirection.UP) index - 1 else index + 1
    if (index !in cities.indices || target !in cities.indices) return this
    val reordered = cities.toMutableList()
    reordered.move(index, target)
    return copy(cities = reordered)
}

fun PathDraft.withStreetsAt(index: Int, streets: List<String>): PathDraft =
    if (index !in cities.indices) this
    else copy(cities = cities.mapIndexed { i, city ->
        if (i == index) city.copy(streets = streets) else city
    })

/**
 * Appends a street typed by the shop, trimmed, ignoring blanks and anything already on the list.
 * The comparison is the same normalization the customer matcher uses, so "Rue du Moulin" and
 * "rue du moulin" cannot both end up stored.
 */
fun List<String>.plusStreet(raw: String): List<String> {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return this
    val normalized = trimmed.normalizeCityName()
    if (normalized.isEmpty()) return this
    if (any { it.normalizeCityName() == normalized }) return this
    return this + trimmed
}
