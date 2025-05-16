package com.example.superutils;

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.superutils.data.StorageManager
import com.example.superutils.network.TcpConnectionService
import com.example.superutils.ui.components.NetworkStatusView
import com.example.superutils.ui.components.ScreenUtils
import com.example.superutils.ui.page.ReceivedDataPage
import com.example.superutils.ui.page.SendDataPage
import com.example.superutils.ui.theme.SuperUtilsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        TcpConnectionService.instance
        StorageManager.instance
        setContent {
            SuperUtilsTheme {
                HomePage()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        TcpConnectionService.instance.dispose()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePage() {
    val pagerState = rememberPagerState(pageCount = {2});

    Box() {
        HorizontalPager(beyondBoundsPageCount = 2, state = pagerState) { page ->
            when (page) {
                0 -> SendDataPage()
                1 -> ReceivedDataPage()
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(color = Color.Black.copy(alpha = 0.75f))
                    .padding(bottom = 0.dp)
                    .clipToBounds()
            ) {
                Box(modifier = Modifier.padding(top = 20.dp, bottom = 24.dp + ScreenUtils.getBottomPadding())) {
                    NetworkStatusView()
                }
            }
        }
    }
}


//        BackdropBlur(
//            Color.Black.copy(alpha = 0.5f), Modifier
//                .height(75.dp)
//                .width(Double.POSITIVE_INFINITY.dp)
//        ) {
//
//        }