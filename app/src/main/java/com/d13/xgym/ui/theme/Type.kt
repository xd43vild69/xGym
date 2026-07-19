package com.d13.xgym.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Jerarquía tipográfica sobre la familia por defecto (Roboto, sans-serif moderna).
// Se refuerzan los pesos para dar peso visual a los cronómetros y títulos.
val XGymTypography = Typography().run {
    copy(
        // Cronómetro de fase gigante
        displayLarge = displayLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 64.sp
        ),
        // Cronómetro total prominente
        displaySmall = displaySmall.copy(
            fontWeight = FontWeight.Bold
        ),
        // Nombre de ejercicio
        headlineMedium = headlineMedium.copy(
            fontWeight = FontWeight.SemiBold
        ),
        // Títulos de card
        titleMedium = titleMedium.copy(
            fontWeight = FontWeight.Medium
        )
    )
}
