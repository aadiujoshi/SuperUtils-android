package com.example.superutils.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize

object ScreenUtils {

    // Function to get the top padding (status bar height)
    @Composable
    fun getTopPadding(): Dp {
        val insets = WindowInsets.safeDrawing
        return insets.asPaddingValues().calculateTopPadding()
    }

    // Function to get the bottom padding (navigation bar height)
    @Composable
    fun getBottomPadding(): Dp {
        val insets = WindowInsets.safeDrawing
        return insets.asPaddingValues().calculateBottomPadding()
    }

    // Function to get the full screen size in Dp
    @Composable
    fun getScreenSize(): DpSize {
        val context = LocalContext.current
        val density = LocalDensity.current

        val displayMetrics = context.resources.displayMetrics
        return with(density) {
            DpSize(
                width = displayMetrics.widthPixels.toDp(),
                height = displayMetrics.heightPixels.toDp()
            )
        }
    }
}


//@Composable
//fun SafeArea(
//    modifier: Modifier = Modifier,
//    content: @Composable BoxScope.() -> Unit
//) {
//    Box(
//        modifier = modifier
//            .padding(WindowInsets.safeDrawing.asPaddingValues())
//    ) {
//        content()
//    }
//}
