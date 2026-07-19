package com.d13.xgym.ui.theme

import androidx.compose.ui.graphics.Color

// Superficies (modo oscuro)
val Background = Color(0xFF121212)
val Surface = Color(0xFF1E1E1E)      // Cards: ligeramente más claras que el fondo
val SurfaceVariant = Color(0xFF2A2A2A)
val Outline = Color(0xFF3A3A42)

// Texto
val OnBackground = Color(0xFFECECEC)     // texto principal (WCAG AAA sobre #121212)
val OnSurface = Color(0xFFECECEC)
val OnSurfaceVariant = Color(0xFFA0A0A8) // texto secundario

// Acento (cian eléctrico)
val Primary = Color(0xFF00E5FF)
val OnPrimary = Color(0xFF00363D)        // texto oscuro legible sobre cian

// Error
val ErrorRed = Color(0xFFFF5252)
val OnError = Color(0xFF000000)

// Colores semánticos de fase de entrenamiento
val PhaseExercising = Color(0xFF4CAF50)  // verde: ejercicio en curso
val PhaseResting = Color(0xFF2196F3)     // azul: descanso
