package com.aethernet.aethercontrol.ui.theme

// =============================================================================
// Type.kt — Tipografía Material3 | 6º Semestre UTP
// Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
// Experiencia: 2 años HTML/CSS/JS/React (CSS font, MUI typography), 1 año C
// Analogía React: este archivo es como `createTheme({typography: {bodyLarge: {fontSize: 16, lineHeight: 24}}})` en MUI React
// — define estilos de texto para toda la app (ver Theme.kt:55 typography = Typography).
// FOSS: Compose Material3 (AndroidX, Apache 2.0) — RNF-3.1.
// Origen: Template Android Studio — tipografía default bodyLarge 16sp, no personalizada Sprint 1.
// Uso: `Text(text = "LED Local", style = MaterialTheme.typography.titleMedium)` en LedStatusCard.kt:78 — como `typography.titleMedium` en MUI React.
// =============================================================================

import androidx.compose.material3.Typography // Typography — como `typography` en MUI React
import androidx.compose.ui.text.TextStyle // TextStyle — como `CSSProperties` en React (fontFamily, fontWeight, fontSize)
import androidx.compose.ui.text.font.FontFamily // FontFamily — como `fontFamily: 'Roboto'` en CSS
import androidx.compose.ui.text.font.FontWeight // FontWeight — como `fontWeight: 400` en CSS
import androidx.compose.ui.unit.sp // sp — como `px` pero scaled (accesibilidad, como `rem` en CSS)

// Set of Material typography styles to start with — como `typography` en MUI React (bodyLarge, titleLarge, labelSmall...)
val Typography = Typography(
    bodyLarge = TextStyle( // bodyLarge — como `body1` en MUI React (texto principal)
        fontFamily = FontFamily.Default, // Default — como `fontFamily: 'Roboto'` en CSS (sistema)
        fontWeight = FontWeight.Normal, // Normal 400 — como `fontWeight: 400` en CSS
        fontSize = 16.sp, // 16sp — como `fontSize: 16px` en CSS pero scaled (accesibilidad)
        lineHeight = 24.sp, // 24sp — como `lineHeight: 24px` en CSS
        letterSpacing = 0.5.sp // 0.5sp — como `letterSpacing: 0.5px` en CSS
    )
    /* Other default text styles to override — como `typography.h1`, `typography.caption` en MUI React */
    /* titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)
