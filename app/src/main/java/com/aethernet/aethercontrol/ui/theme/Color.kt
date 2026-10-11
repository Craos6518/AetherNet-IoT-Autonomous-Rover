package com.aethernet.aethercontrol.ui.theme

// =============================================================================
// Color.kt — Paleta Material3 | 6º Semestre UTP
// Autor: Andres Felipe Martinez Henao
// Experiencia: 2 años HTML/CSS/JS/React (CSS color, Material UI), 1 año C
// Analogía React: este archivo es como `colors.js` con `export const Purple80 = '#D0BCFF'` en React/MUI
// — define paleta Material3 para Theme.kt (como theme.palette en MUI React).
// Analogía CSS: cada `Color(0xFFD0BCFF)` es como `color: #D0BCFF` en CSS — ARGB hex (FF alpha + RGB).
// FOSS: Compose Material3 (AndroidX, Apache 2.0) — RNF-3.1.
// Origen: Template Android Studio con Compose — paleta default Purple/Pink, no personalizada Sprint 1.
// LED local usa colores propios en LedStatusCard.kt:49 (0xFF4CAF50 verde etc.), no estos Purple.
// =============================================================================

import androidx.compose.ui.graphics.Color // Color — como `color` en CSS pero ARGB 0xFFRRGGBB

// Colores claros (dark theme) — como `palette.dark` en MUI React (usados en DarkColorScheme en Theme.kt:14)
val Purple80 = Color(0xFFD0BCFF) // morado claro 80 — como `#D0BCFF` en CSS
val PurpleGrey80 = Color(0xFFCCC2DC) // gris morado 80
val Pink80 = Color(0xFFEFB8C8) // rosa claro 80

// Colores oscuros (light theme) — como `palette.light` en MUI React (usados en LightColorScheme en Theme.kt:20)
val Purple40 = Color(0xFF6650a4) // morado 40 — primary en light theme
val PurpleGrey40 = Color(0xFF625b71) // gris morado 40 — secondary
val Pink40 = Color(0xFF7D5260) // rosa 40 — tertiary
