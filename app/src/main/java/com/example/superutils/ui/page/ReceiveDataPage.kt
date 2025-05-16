package com.example.superutils.ui.page

import android.graphics.BitmapFactory
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.sharp.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.superutils.data.DownloadUtils
import com.example.superutils.data.StorageManager
import com.example.superutils.ui.components.FileCard
import com.example.superutils.ui.components.ScreenUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ReceivedDataPage() {
    val context = LocalContext.current
    val allFiles = remember { mutableStateListOf<File>() }
    var refreshKey by remember { mutableIntStateOf(0) }
    val internalStorageRefresh by StorageManager.instance.storageUpdateKey.collectAsState()

    LaunchedEffect(internalStorageRefresh) {
        val types = listOf("images", "text", "videos", "others")
        val files = withContext(Dispatchers.IO) {
            types.flatMap { StorageManager.instance.listFilesByType(it, context) }
        }
        allFiles.clear()
        allFiles.addAll(files)
    }

    val col1 = allFiles.filterIndexed { index, _ -> index % 2 == 0 }
    val col2 = allFiles.filterIndexed { index, _ -> index % 2 != 0 }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        key(refreshKey, internalStorageRefresh) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(top = 120.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.Start) {
                    Column(modifier = Modifier.weight(1f)) {
                        col1.forEach { FileCard(it) }
                        Spacer(modifier = Modifier.height(200.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        col2.forEach { FileCard(it) }
                        Spacer(modifier = Modifier.height(200.dp))
                    }
                }
            }
        }
        // Sticky Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp + ScreenUtils.getTopPadding() + 8.dp)
                .align(Alignment.TopCenter),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp + ScreenUtils.getTopPadding() + 8.dp)
                    .background(Color.Black.copy(alpha = 0.25f))
//                    .blur(16.dp),
            )
            Box(
                modifier = Modifier
                    .padding(top = ScreenUtils.getTopPadding())
            ) {
                Text(
                    "Received From Laptop",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            IconButton(
                onClick = { refreshKey += 1 },
                modifier = Modifier
                    .background(color = Color.Transparent)
                    .align(Alignment.TopEnd)
                    .padding(top = ScreenUtils.getTopPadding() + 8.dp, end = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
            }

            IconButton(
                onClick = { DownloadUtils.launchDefaultFileManager(context) },
                modifier = Modifier
                    .background(color = Color.Transparent)
                    .align(Alignment.TopStart)
                    .padding(top = ScreenUtils.getTopPadding() + 8.dp, start = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Filter,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

//@Composable
//fun ReceivedDataPage() {
//    val context = LocalContext.current
//    val allFiles = remember { mutableStateListOf<File>() }
//
//    LaunchedEffect(Unit) {
//        val types = listOf("images", "text", "videos", "others")
//        val files = withContext(Dispatchers.IO) {
//            types.flatMap { StorageManager.instance.listFilesByType(it, context) }
//        }
//        allFiles.clear()
//        allFiles.addAll(files)
//    }
//
//    val col1 = allFiles.filterIndexed { index, _ -> index % 2 == 0 }
//    val col2 = allFiles.filterIndexed { index, _ -> index % 2 != 0 }
//
//    Box {
//        Column(
//            modifier = Modifier
//                .verticalScroll(rememberScrollState())
//                .padding(top = 80.dp, start = 16.dp, end = 16.dp)
//        ) {
//            Row(horizontalArrangement = Arrangement.Start) {
//                Column(modifier = Modifier.weight(1f)) {
//                    col1.forEach { com.example.superutils.FileCard(it) }
//                }
//                Column(modifier = Modifier.weight(1f)) {
//                    col2.forEach { com.example.superutils.FileCard(it) }
//                }
//            }
//        }
//
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(60.dp)
//                .background(Color.Transparent)
//                .blur(8.dp),
//            contentAlignment = Alignment.Center
//        ) {
//            Text(
//                "Received From Laptop",
//                fontSize = 24.sp,
//                fontWeight = FontWeight.Bold,
//                color = Color.White
//            )
//        }
//    }
//}
