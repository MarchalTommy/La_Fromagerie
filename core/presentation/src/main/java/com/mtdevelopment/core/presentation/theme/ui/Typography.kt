package com.mtdevelopment.core.presentation.theme.ui

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.mtdevelopment.core.presentation.R

@OptIn(ExperimentalTextApi::class)
val bodyLightFontFamily =
    FontFamily(
        Font(
            R.font.switzer_variable,
            weight = FontWeight.Thin,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(100),
                FontVariation.width(30f),
                FontVariation.slant(0f),
            )
        )
    )


@OptIn(ExperimentalTextApi::class)
val bodyMediumFontFamily =
    FontFamily(
        Font(
            R.font.switzer_variable,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(300),
                FontVariation.width(30f),
                FontVariation.slant(0f)
            )
        )
    )

@OptIn(ExperimentalTextApi::class)
val bodyLargeFontFamily =
    FontFamily(
        Font(
            R.font.switzer_variable,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(400),
                FontVariation.width(30f),
                FontVariation.slant(0f)
            )
        )
    )

val numberFontFamily =
    FontFamily(
        Font(
            R.font.noto_sans_light
        )
    )


@OptIn(ExperimentalTextApi::class)
val titleFontFamily =
    FontFamily(
        Font(
            R.font.clash_grotesk,
            weight = FontWeight.Bold,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(700),
                FontVariation.width(30f),
                FontVariation.slant(0f),
            )
        )
    )

