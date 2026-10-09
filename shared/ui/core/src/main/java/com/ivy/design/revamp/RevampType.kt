package com.ivy.design.revamp

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ivy.ui.R

// FontVariation is stable in later Compose versions; the variable font needs it to pick a weight.
@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: FontWeight): Font = Font(
    resId = R.font.plus_jakarta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Plus Jakarta Sans (SIL Open Font License, see docs/licenses/PlusJakartaSans-OFL.txt). */
val PlusJakartaSans: FontFamily = FontFamily(
    jakarta(FontWeight.Normal),
    jakarta(FontWeight.Medium),
    jakarta(FontWeight.SemiBold),
    jakarta(FontWeight.Bold),
    jakarta(FontWeight.ExtraBold),
)

/** Digits of equal width so amounts line up in lists. */
private const val TabularNumbers = "tnum"

/**
 * Text styles of the revamped UI. Colors are applied by the caller from [RevampColors].
 */
object RevampType {
    /** The big balance on Home. */
    val display = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        letterSpacing = (-1).sp,
        fontFeatureSettings = TabularNumbers,
    )

    /** Screen titles. */
    val headline = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        letterSpacing = (-0.5).sp,
    )

    /** Section titles. */
    val title = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 18.sp)

    /** Amounts in tiles. */
    val amount = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        fontFeatureSettings = TabularNumbers,
    )

    /** Main text of list rows. */
    val bodyStrong = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        fontFeatureSettings = TabularNumbers,
    )

    val body = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)

    /** Secondary lines and labels. */
    val label = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium, fontSize = 13.sp)

    /** Small bold captions, e.g. day headers and bottom bar labels. */
    val caption = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 12.sp)
}
