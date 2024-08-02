package com.mtdevelopment.core.presentation.theme.ui

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import com.mtdevelopment.core.presentation.R

// TODO: FIX FONT VARIABLE
@OptIn(ExperimentalTextApi::class)
val bodyFontFamily =
    FontFamily(
        Font(
            R.font.switzer_variable,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(100),
                FontVariation.width(30f),
                FontVariation.slant(0f),
            )
        ),
        Font(
            R.font.switzer_variable,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(300),
                FontVariation.width(30f),
                FontVariation.slant(0f)
            )
        ),
        Font(
            R.font.switzer_variable,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(400),
                FontVariation.width(30f),
                FontVariation.slant(0f)
            )
        )
    )

@OptIn(ExperimentalTextApi::class)
val numberFontFamily =
    FontFamily(
        Font(
            R.font.noto_sans_thin,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(100),
                FontVariation.width(30f),
                FontVariation.slant(0f),
            )
        )
    )


@OptIn(ExperimentalTextApi::class)
val titleFontFamily =
    FontFamily(
        Font(
            R.font.clash_grotesk,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(700),
                FontVariation.width(30f),
                FontVariation.slant(0f),
            )
        )
    )

