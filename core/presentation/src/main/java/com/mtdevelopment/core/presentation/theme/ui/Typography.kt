package com.mtdevelopment.core.presentation.theme.ui

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.mtdevelopment.core.presentation.R

/**
 * The face secondary body copy is set in.
 *
 * Weight 300, not the 100 it used to be. Switzer at weight 100 renders as hairlines, and every
 * caller of `bodySmall` pairs it with `onSurfaceVariant` -- a muted colour on an almost absent
 * stroke. The pickup point's address, which is the one thing a collecting customer has to be
 * able to read, was the report that surfaced it. 300 matches [bodyMediumFontFamily]; the
 * hierarchy between them is carried by colour and placement, which is where it was doing the
 * work anyway.
 */
@OptIn(ExperimentalTextApi::class)
val bodyLightFontFamily =
    FontFamily(
        Font(
            R.font.switzer_variable,
            weight = FontWeight.Light,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(300),
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

