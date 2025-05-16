package com.example.superutils.ui.components

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.superutils.data.ClipboardUtils
import com.example.superutils.data.DownloadUtils
import com.example.superutils.data.MimeHelper
import com.example.superutils.data.ShareUtils
import com.example.superutils.data.StorageManager
import java.io.File

//for that weird double fake delete bug, gpt prompt this
//figure out the following bug:
//I have the following 3 files in my kotlin/compose app, and i get the error:
//the file not found error

@Composable
fun FileCard(givenFile: File) {
    var showOptions by remember { mutableStateOf(false) }
    var parentOffset by remember { mutableStateOf(Offset.Zero) }

    var showRename by remember { mutableStateOf(false) }
    var file by remember { mutableStateOf(givenFile) }
    val mimeType = remember(file) { MimeHelper.getMimeType(file.name) }

    val context = LocalContext.current;

    val clickModifier = Modifier
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    showOptions = true
                },
                onLongPress = {
                    // Empty long press handler
                }
            )
        }
        .onGloballyPositioned { coordinates ->
            val positionInWindow = coordinates.positionInWindow()
            parentOffset = positionInWindow
        }

    when {
        mimeType.startsWith("image") -> {
            val bitmap = remember(file) {
                if(file.exists()){
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } else {
                    null
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = file.name,
                    modifier = Modifier
                        .padding(4.dp)
                        .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .then(clickModifier)
                )
            }
        }

        mimeType.startsWith("text") -> {
            val textContent = remember(file) {
                if (file.exists()){
                    file.readText().take(200)
                } else {
                    "FILE_DOES_NOT_EXIST"
                }
            }
            if (textContent != "FILE_DOES_NOT_EXIST"){
                Card(
                    shape = RoundedCornerShape(16.dp),
//                border = BorderStroke(2.dp, Color.White),
                    modifier = Modifier
                        .padding(4.dp)
                        .sizeIn(
                            maxHeight = 400.dp
                        )
                        .fillMaxWidth()
                        .then(clickModifier)
                ) {
                    Text(
                        textContent,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = true,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        else -> {
            Card(
                shape = RoundedCornerShape(24.dp),
//                border = BorderStroke(2.dp, Color.White),
//                colors = CardDefaults.cardColors(containerColor = Color.Gray)
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth()
                    .height(160.dp)
                    .border(
                        border = BorderStroke(width = 2.dp, color = Color.Gray),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .then(clickModifier),
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    Text(text = mimeType, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = file.name, fontSize = 12.sp, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    if (showOptions) {
        Popup(
//            offset = IntOffset(clickOffset.x.toInt(), clickOffset.y.toInt()),
            offset = IntOffset(parentOffset.x.toInt(), parentOffset.y.toInt()),
            properties = PopupProperties(focusable = true),
            onDismissRequest = { showOptions = false },
        ) {
            Box(
                modifier = Modifier
                    .shadow(elevation = 5.dp, shape = RoundedCornerShape(16.dp))
                    .width(200.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(color = Color.DarkGray)
                        .border(
                            border = BorderStroke(width = 2.dp, color = Color.Gray),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                    ) {
                        OptionItem(Icons.Default.ContentCopy, "Copy to Clipboard") {
                            ClipboardUtils.copyFileToClipboard(context, file)
                        }
                        OptionItem(Icons.Default.Share, "Share") {
                            ShareUtils.shareFile(context, file);
                        }
                        OptionItem(Icons.Default.Download, "Download") {
                            DownloadUtils.copyFileToDownloads(context, file)
                        }
                        OptionItem(Icons.Default.DriveFileRenameOutline, "Rename") {
                            showRename = true
                        }
                        OptionItem(Icons.Default.Delete, "Delete") {
                            StorageManager.instance.deleteFileByName(
                                file.name,
                                MimeHelper.getMimeType(file.name),
                                context
                            )
                        }
                    }
                }
            }
//            if(
//                showOptions
//                enter = fadeIn(),
//                exit = fadeOut()
//            ) {
//            }
        }
    }

    if (showRename) {
        RenameModal(
            initialText = file.name,
            onRename = { newName ->
                val renameResult = StorageManager.instance.renameFile(
                    context,
                    newName,
                    file.name,
                    MimeHelper.getMimeType(newName)
                );
                file = when (renameResult) {
                    is com.example.superutils.data.Result.Success -> renameResult.data
                    is com.example.superutils.data.Result.Failure -> file
                }
                showRename = false;
            },
            onDismiss = { showRename = false }
        )
    }
}

@Composable
fun OptionItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null
        )
        Text(
            text,
            color = Color.White,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        )
    }
}


//    var clickOffset by remember { mutableStateOf(Offset.Zero) }

//    Log.d("FileCard", clickOffset.toString())
//    Log.d("FileCard", parentOffset.toString())

