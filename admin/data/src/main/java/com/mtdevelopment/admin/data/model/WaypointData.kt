package com.mtdevelopment.admin.data.model


import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class WaypointData(
    @SerialName("address")
    val address: String,
    /**
     * Indique que l'emplacement de ce point de cheminement doit être prioritaire pour que le
     * véhicule s'arrête à un côté de la route en particulier. Lorsque vous définissez cette valeur,
     * l'itinéraire passe par l'emplacement afin que le véhicule puisse s'arrêter sur le côté de la
     * route vers lequel l'emplacement est orienté à partir du centre. Cette option ne fonctionne
     * que pour DRIVE et TWO_WHEELER RouteTravelMode.
     */
    @SerialName("sideOfRoad")
    val sideOfRoad: Boolean = false,
    /**
     * Indique que le point de cheminement est destiné aux véhicules, et que l'intention est de
     * monter ou de descendre. Lorsque vous définissez cette valeur, l'itinéraire calculé n'inclut
     * pas les points de cheminement autres que via sur les routes non adaptées aux lieux de prise
     * en charge et de dépose. Cette option ne fonctionne que pour les modes de transport DRIVE et
     * TWO_WHEELER, et lorsque la valeur de locationType est Location.
     */
    @SerialName("vehicleStopover")
    val vehicleStopover: Boolean = false,
    /**
     * Marque ce point de cheminement comme un jalon plutôt que comme un point d'arrêt.
     * Pour chaque point de cheminement non via de la requête, la réponse ajoute une entrée au
     * tableau legs afin de fournir les détails des arrêts de cette section du trajet. Définissez
     * cette valeur sur "true" si vous souhaitez que l'itinéraire passe par ce point de cheminement
     * sans s'arrêter. Les points de cheminement via des points de cheminement n'entraînent pas
     * l'ajout d'une entrée au tableau legs, mais ils guident le trajet par le point de cheminement.
     * Vous ne pouvez définir cette valeur que sur des points de cheminement intermédiaires. La
     * requête échoue si vous définissez ce champ sur les points de cheminement terminaux. Si
     * ComputeRoutesRequest.optimize_waypoint_order est défini sur "true", ce champ ne peut pas être
     * défini sur "true". Sinon, la requête échoue.
     */
    @SerialName("via")
    val via: Boolean
)