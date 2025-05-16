package com.example.superutils.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun BackdropBlur(
    tintColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        // Blurred backdrop with tint
        Box(
            modifier = Modifier
//                .width(IntrinsicSize.Min)
//                .height(IntrinsicSize.Min)
                .blur(10.dp)
                .background(tintColor)
                .zIndex(0f) // behind the content
        )

        // Foreground content, not blurred
        Box(
            modifier = Modifier
                .matchParentSize()
                .zIndex(1f)
        ) {
            content()
        }
    }
}
