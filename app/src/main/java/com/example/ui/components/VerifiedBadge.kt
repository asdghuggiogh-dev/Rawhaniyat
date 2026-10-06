package com.example.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val VerifiedBlueColor = Color(0xFF1E88E5) // Professional Verification Blue

@Composable
fun VerifiedBadge(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    contentDescription: String = "حساب موثق 🔵"
) {
    Icon(
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = contentDescription,
        tint = VerifiedBlueColor,
        modifier = modifier
            .size(size)
            .padding(horizontal = 1.dp)
    )
}
