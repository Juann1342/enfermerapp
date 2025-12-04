package com.chifuz.enfermerapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Configuración de la tipografía para alta legibilidad

val Typography = Typography(
    // Titulares y textos de resultado grandes
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp, // Aumentado
        lineHeight = 44.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp, // Aumentado
        lineHeight = 36.sp
    ),
    // Texto de las tarjetas de resultado
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, // Aumentado
        lineHeight = 26.sp
    ),
    // Texto normal, campos de input, y botones
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp, // Aumentado
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Aquí se pueden definir otros estilos como labelLarge o bodySmall si es necesario */
)