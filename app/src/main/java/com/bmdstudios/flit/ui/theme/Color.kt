package com.bmdstudios.flit.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

// Single seed color - Material 3 will generate the entire palette from this
val SeedColor = Color(0xFF0074B7)

/**
 * HSL color representation for easier color manipulation.
 */
private data class HSL(
    val h: Float, // Hue: 0-360
    val s: Float, // Saturation: 0-1
    val l: Float  // Lightness: 0-1
)

/**
 * Converts RGB Color to HSL color space.
 */
private fun Color.toHSL(): HSL {
    val r = red
    val g = green
    val b = blue
    
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    
    val l = (max + min) / 2f
    
    val s = when {
        delta == 0f -> 0f
        l > 0.5f -> delta / (2f - max - min)
        else -> delta / (max + min)
    }
    
    val h = when {
        delta == 0f -> 0f
        max == r -> {
            val rawH = ((g - b) / delta) % 6f
            (rawH * 60f).let { if (it < 0) it + 360f else it }
        }
        max == g -> ((b - r) / delta + 2f) * 60f
        else -> ((r - g) / delta + 4f) * 60f
    }
    
    return HSL(h, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
}

/**
 * Converts HSL color space to RGB Color.
 */
private fun HSL.toColor(): Color {
    val h = this.h / 360f
    val s = this.s.coerceIn(0f, 1f)
    val l = this.l.coerceIn(0f, 1f)
    
    val q = when {
        l < 0.5f -> l * (1f + s)
        else -> l + s - l * s
    }
    val p = 2f * l - q
    
    fun hueToRgb(t: Float): Float {
        val t2 = when {
            t < 0f -> t + 1f
            t > 1f -> t - 1f
            else -> t
        }
        return when {
            t2 < 1f / 6f -> p + (q - p) * 6f * t2
            t2 < 1f / 2f -> q
            t2 < 2f / 3f -> p + (q - p) * (2f / 3f - t2) * 6f
            else -> p
        }
    }
    
    val r = hueToRgb(h + 1f / 3f).coerceIn(0f, 1f)
    val g = hueToRgb(h).coerceIn(0f, 1f)
    val b = hueToRgb(h - 1f / 3f).coerceIn(0f, 1f)
    
    return Color(r, g, b)
}

/**
 * Adjusts the lightness of a color.
 */
private fun Color.adjustLightness(factor: Float): Color {
    val hsl = this.toHSL()
    return HSL(hsl.h, hsl.s, (hsl.l * factor).coerceIn(0f, 1f)).toColor()
}

/**
 * Adjusts the saturation (chroma) of a color.
 */
private fun Color.adjustSaturation(factor: Float): Color {
    val hsl = this.toHSL()
    return HSL(hsl.h, (hsl.s * factor).coerceIn(0f, 1f), hsl.l).toColor()
}

/**
 * Shifts the hue of a color by the specified degrees.
 */
private fun Color.shiftHue(degrees: Float): Color {
    val hsl = this.toHSL()
    val newH = (hsl.h + degrees) % 360f
    return HSL(if (newH < 0) newH + 360f else newH, hsl.s, hsl.l).toColor()
}

/**
 * Sets the lightness of a color to a specific value.
 */
private fun Color.setLightness(targetLightness: Float): Color {
    val hsl = this.toHSL()
    return HSL(hsl.h, hsl.s, targetLightness.coerceIn(0f, 1f)).toColor()
}

/**
 * Sets the saturation of a color to a specific value.
 */
private fun Color.setSaturation(targetSaturation: Float): Color {
    val hsl = this.toHSL()
    return HSL(hsl.h, targetSaturation.coerceIn(0f, 1f), hsl.l).toColor()
}

/**
 * Calculates the relative luminance of a color (for contrast calculation).
 */
private fun Color.luminance(): Float {
    fun toLinear(c: Float): Float {
        return if (c <= 0.03928f) {
            c / 12.92f
        } else {
            ((c + 0.055f) / 1.055f).pow(2.4f)
        }
    }
    
    val r = toLinear(red)
    val g = toLinear(green)
    val b = toLinear(blue)
    
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

private fun Color.blend(color2: Color, ratio: Float): Color {
    return Color(
        red * ratio + color2.red * (1 - ratio),
        green * ratio + color2.green * (1 - ratio),
        blue * ratio + color2.blue * (1 - ratio)
    )
}

/**
 * Calculates contrast ratio between two colors.
 */
private fun contrastRatio(color1: Color, color2: Color): Float {
    val l1 = color1.luminance() + 0.05f
    val l2 = color2.luminance() + 0.05f
    return if (l1 > l2) l1 / l2 else l2 / l1
}

/**
 * Determines the best on-color (white or black) for a given background color.
 */
private fun getOnColor(backgroundColor: Color): Color {
    val whiteContrast = contrastRatio(backgroundColor, Color.White)
    val blackContrast = contrastRatio(backgroundColor, Color.Black)
    return if (whiteContrast > blackContrast) Color.White else Color.Black
}

/**
 * Generates a light ColorScheme from a seed color using Material3 principles.
 * All colors are programmatically generated from the seed color.
 */
fun lightColorSchemeFromSeed(seedColor: Color): ColorScheme {
    val seedHSL = seedColor.toHSL()
    
    // Primary colors - use seed with appropriate lightness for light theme
    val primary = seedColor.setLightness(0.45f) // Darker for light theme
    val onPrimary = getOnColor(primary)
    
    // Primary container - lighter, desaturated variant
    val primaryContainer = seedColor
        .setLightness(0.90f)
        .setSaturation(seedHSL.s * 0.3f)
    val onPrimaryContainer = seedColor.setLightness(0.15f)
    
    // Secondary color - hue shifted by ~135 degrees (complementary-analogous)
    val secondary = seedColor
        .shiftHue(135f)
        .setLightness(0.35f)
        .setSaturation(seedHSL.s * 0.6f)
    val onSecondary = getOnColor(secondary)
    
    // Secondary container - lighter, desaturated variant
    val secondaryContainer = secondary
        .setLightness(0.88f)
        .setSaturation(seedHSL.s * 0.25f)
    val onSecondaryContainer = secondary.setLightness(0.12f)
    
    // Tertiary color - hue shifted by ~60 degrees (accent)
    val tertiary = seedColor
        .shiftHue(60f)
        .setLightness(0.40f)
        .setSaturation(seedHSL.s * 0.7f)
    val onTertiary = getOnColor(tertiary)
    
    // Tertiary container - lighter, desaturated variant
    val tertiaryContainer = tertiary
        .setLightness(0.92f)
        .setSaturation(seedHSL.s * 0.3f)
    val onTertiaryContainer = tertiary.setLightness(0.18f)
    
    // Error colors (standard Material red - semantic, not seed-based)
    val error = Color(0xFFBA1A1A)
    val onError = Color.White
    val errorContainer = Color(0xFFFFDAD6)
    val onErrorContainer = Color(0xFF410002)
    
    // Neutral colors - use seed hue with very low saturation for subtle tinting
    val neutralHue = seedHSL.h
    val background = HSL(neutralHue, 0.02f, 0.99f).toColor()
    val onBackground = HSL(neutralHue, 0.05f, 0.10f).toColor()
    val surface = background.blend(seedColor, 0.5f)
    val onSurface = onBackground
    val surfaceVariant = HSL(neutralHue, 0.05f, 0.88f).toColor()
    val onSurfaceVariant = HSL(neutralHue, 0.08f, 0.27f).toColor()
    val outline = HSL(neutralHue, 0.06f, 0.45f).toColor()
    val outlineVariant = HSL(neutralHue, 0.04f, 0.77f).toColor()
    
    // Inverse colors - calculated from base colors
    val inverseSurface = HSL(neutralHue, 0.05f, 0.18f).toColor()
    val inverseOnSurface = HSL(neutralHue, 0.02f, 0.94f).toColor()
    val inversePrimary = primary.setLightness(0.75f)
    
    val scrim = Color.Black
    val surfaceTint = primary
    
    return lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = outlineVariant,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        inversePrimary = inversePrimary,
        scrim = scrim,
        surfaceTint = surfaceTint
    )
}

/**
 * Generates a dark ColorScheme from a seed color using Material3 principles.
 * All colors are programmatically generated from the seed color.
 */
fun darkColorSchemeFromSeed(seedColor: Color): ColorScheme {
    val seedHSL = seedColor.toHSL()
    
    // Primary colors - lighter variant for dark theme
    val primary = seedColor.setLightness(0.75f)
    val onPrimary = seedColor.setLightness(0.20f)
    
    // Primary container - darker, desaturated variant
    val primaryContainer = seedColor
        .setLightness(0.25f)
        .setSaturation(seedHSL.s * 0.4f)
    val onPrimaryContainer = primary.setLightness(0.85f)
    
    // Secondary color - hue shifted, lighter for dark theme
    val secondary = seedColor
        .shiftHue(135f)
        .setLightness(0.75f)
        .setSaturation(seedHSL.s * 0.5f)
    val onSecondary = secondary.setLightness(0.20f)
    
    // Secondary container - darker, desaturated variant
    val secondaryContainer = secondary
        .setLightness(0.22f)
        .setSaturation(seedHSL.s * 0.3f)
    val onSecondaryContainer = secondary.setLightness(0.85f)
    
    // Tertiary color - hue shifted, lighter for dark theme
    val tertiary = seedColor
        .shiftHue(60f)
        .setLightness(0.80f)
        .setSaturation(seedHSL.s * 0.6f)
    val onTertiary = tertiary.setLightness(0.25f)
    
    // Tertiary container - darker, desaturated variant
    val tertiaryContainer = tertiary
        .setLightness(0.30f)
        .setSaturation(seedHSL.s * 0.35f)
    val onTertiaryContainer = tertiary.setLightness(0.90f)
    
    // Error colors (standard Material red - semantic, not seed-based)
    val error = Color(0xFFFFB4AB)
    val onError = Color(0xFF690005)
    val errorContainer = Color(0xFF93000A)
    val onErrorContainer = Color(0xFFFFDAD6)
    
    // Neutral colors - use seed hue with very low saturation for subtle tinting
    val neutralHue = seedHSL.h
    val background = HSL(neutralHue, 0.05f, 0.10f).toColor()
    val onBackground = HSL(neutralHue, 0.02f, 0.89f).toColor()
    val surface = background.blend(seedColor, 0.5f)
    val onSurface = onBackground
    val surfaceVariant = HSL(neutralHue, 0.08f, 0.27f).toColor()
    val onSurfaceVariant = HSL(neutralHue, 0.04f, 0.77f).toColor()
    val outline = HSL(neutralHue, 0.06f, 0.55f).toColor()
    val outlineVariant = HSL(neutralHue, 0.08f, 0.27f).toColor()
    
    // Inverse colors - calculated from base colors
    val inverseSurface = HSL(neutralHue, 0.02f, 0.89f).toColor()
    val inverseOnSurface = HSL(neutralHue, 0.05f, 0.18f).toColor()
    val inversePrimary = seedColor.setLightness(0.40f)
    
    val scrim = Color.Black
    val surfaceTint = primary
    
    return darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = outlineVariant,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        inversePrimary = inversePrimary,
        scrim = scrim,
        surfaceTint = surfaceTint
    )
}
