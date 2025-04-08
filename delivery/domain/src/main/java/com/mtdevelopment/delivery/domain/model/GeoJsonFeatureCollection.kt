package com.mtdevelopment.delivery.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GeoJsonFeatureCollection(val type: String, val features: List<GeoJsonFeature>)

@Serializable
data class GeoJsonFeature(val type: String, val geometry: Geometry, val properties: Properties)

@Serializable
data class Geometry(val type: String, val coordinates: List<List<Double>>)

@Serializable
data class Properties(val segments: List<Segment>, val summary: Summary)

@Serializable
data class Segment(val distance: Double, val duration: Double)

@Serializable
data class Summary(val distance: Double, val duration: Double)