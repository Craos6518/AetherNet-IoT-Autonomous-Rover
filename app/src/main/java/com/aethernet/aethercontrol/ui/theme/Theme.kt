package com.aethernet.aethercontrol.ui.theme

// =============================================================================
// Theme.kt — Tema Material3 | 6º Semestre UTP
// Autor: Andres Felipe Martinez Henao
// Experiencia: 2 años HTML/CSS/JS/React (ThemeProvider, CSS variables), 1 año C
// Analogía React: este archivo es como `ThemeProvider` en React/MUI o `createTheme({palette: {primary: Purple40}})` en MUI —
// define DarkColorScheme/LightColorScheme y AetherControlTheme que envuelve la app (ver MainActivity.kt:19).
// FOSS: Compose Material3 (AndroidX, Apache 2.0) — RNF-3.1.
// Origen: Template Android Studio — dynamicColor en Android 12+ (Material You), fallback a Purple/Pink si no.
// Uso: MainActivity.kt:19 `AetherControlTheme { NavGraph(...) }` — como `<ThemeProvider theme={theme}><App /></ThemeProvider>` en React.
// =============================================================================

import android.app.Activity // para Activity si necesitas
import android.os.Build // para SDK_INT >= S (Android 12) — como `navigator.userAgent` en JS pero para tema
import androidx.compose.foundation.isSystemInDarkTheme // isSystemInDarkTheme — como `prefers-color-scheme: dark` en CSS media query
import androidx.compose.material3.MaterialTheme // MaterialTheme — como `ThemeProvider` en MUI React
import androidx.compose.material3.darkColorScheme // darkColorScheme — como `createTheme({palette: {mode: 'dark'}})` en MUI
import androidx.compose.material3.dynamicDarkColorScheme // dynamicDarkColorScheme — como Material You en Android 12+ (colores del wallpaper)
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable // @Composable — como `function Component()` en React
import androidx.compose.ui.platform.LocalContext // LocalContext — como `useContext` en React para obtener Context

// Esquema oscuro — como `createTheme({palette: {mode: 'dark', primary: Purple80}})` en MUI React
private val DarkColorScheme = darkColorScheme(
    primary = Purple80, // primary — morado claro en dark
    secondary = PurpleGrey80,
    tertiary = Pink80
)

// Esquema claro — como `createTheme({palette: {mode: 'light', primary: Purple40}})` en MUI React
private val LightColorScheme = lightColorScheme(
    primary = Purple40, // primary — morado oscuro en light
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override — como `palette.background`, `palette.surface` en MUI React */
    /* background = Color(0xFFFFFBFE), */
    /* surface = Color(0xFFFFFBFE), */
    /* onPrimary = Color.White, */
    /* onSecondary = Color.White, */
    /* onTertiary = Color.White, */
    /* onBackground = Color(0xFF1C1B1F), */
    /* onSurface = Color(0xFF1C1B1F), */
)

@Composable
fun AetherControlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // dark por sistema — como `prefers-color-scheme: dark` en CSS (auto)
    // Dynamic color is available on Android 12+ — como Material You (colores del wallpaper del usuario)
    dynamicColor: Boolean = true, // true activa dynamicColor si SDK >= S (Android 12)
    content: @Composable () -> Unit // contenido — como `children` en React `<ThemeProvider>{children}</ThemeProvider>`
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> { // Android 12+ — usa colores del sistema (Material You)
            val context = LocalContext.current // context — como `useContext` en React
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context) // dynamic — como `createTheme({palette: {mode: dark ? 'dark' : 'light', dynamic: true}})` en MUI
        }

        darkTheme -> DarkColorScheme // dark estático — como `dark: {primary: Purple80}` en MUI
        else -> LightColorScheme // light estático
    }

    MaterialTheme(
        colorScheme = colorScheme, // esquema — como `theme.palette` en MUI React
        typography = Typography, // tipografía — ver Type.kt:10 Typography (como `theme.typography` en MUI)
        content = content // contenido — como `{children}` en React ThemeProvider
    )
}
