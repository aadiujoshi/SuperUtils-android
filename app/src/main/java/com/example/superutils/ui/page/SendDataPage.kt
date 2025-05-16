package com.example.superutils.ui.page

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.superutils.data.MimeHelper
import com.example.superutils.data.Result
import com.example.superutils.data.UriUtils
import com.example.superutils.network.SuperParcel
import com.example.superutils.network.TcpConnectionService
import com.example.superutils.ui.components.StockTextField
import com.example.superutils.ui.components.ScreenUtils
import com.example.superutils.ui.components.SelectFilesCard
import com.example.superutils.ui.components.SelectImagesCard
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File


val TAG = "SendDataPage"

@Composable
fun Header() {
    // Sticky Header
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp + ScreenUtils.getTopPadding() + 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp + ScreenUtils.getTopPadding() + 8.dp)
                .background(Color.Black.copy(alpha = 0.25f))
                .blur(16.dp),
        )
        Box(
            modifier = Modifier
                .padding(top = ScreenUtils.getTopPadding())
        ) {
            Text(
                "Send To Laptop",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun SelectedFile(file: File) {
    val mimeType = MimeHelper.getMimeType(file.name);

    if (mimeType.startsWith("image")) {
        val imageBitmap = remember(file.absolutePath) {
            if (file.exists()) {
                Log.d("SendDataPage", "REMADE selected image")
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                bitmap?.asImageBitmap()
            } else {
                null
            }
        }

        if (imageBitmap != null) {
            val aspectRatio = (imageBitmap.width / imageBitmap.height).toFloat()

            Image(
                bitmap = imageBitmap,
                contentDescription = "Selected Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(if (aspectRatio > 0) aspectRatio else 1f)
                    .clip(RoundedCornerShape(16.dp))
            )
        } else {
            Text("Image not found", color = Color.Red)
        }
    } else {
        Column(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.LightGray),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
            )
            Text(
                text = MimeHelper.getExtension(mimeType),
                textAlign = TextAlign.Center
            )
            Text(
                text = file.name,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
fun SelectedText(text: String) {
//        Log.d("SendDataPage", "Rebuilt selected text")
    Surface(
//            color = Color.Black.copy(alpha = 0.5f),
//            tonalElevation = 4.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .sizeIn(
                minWidth = 100.dp,
                maxWidth = 200.dp,
            )
            .clip(RoundedCornerShape(16.dp)),
    ) {
        Box(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(text)
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun SendDataPage() {
    fun handleImageSelect(context: Context, superParcel: SuperParcel, uriList: List<Uri>) {
        uriList.forEach {
            superParcel.addItem(UriUtils.copyUriToCacheAndCreateParcelItem(context, it))
        }
    }

    fun handleFileSelect(context: Context, superParcel: SuperParcel, uriList: List<Uri>) {
        uriList.forEach {
            Log.d(TAG, it.path.toString());
            superParcel.addItem(UriUtils.copyUriToCacheAndCreateParcelItem(context, it))
        }
    }

    fun handleTextSelect(superParcel: SuperParcel, text: String) {
        if (text.isEmpty()) {
            return
        }
        superParcel.addItem(MimeHelper.getMimeType(MimeHelper.TXT), text)
    }

    fun removeFilesFromSelectedData(superParcel: SuperParcel): SuperParcel{
        val newParcel = SuperParcel();

        for (item in superParcel.getAllItems()){
            if (item.mimeType == MimeHelper.getMimeType(MimeHelper.TXT)){
                newParcel.addItem(item);
            }
        }
        return newParcel;
    }

    fun removeTextFromSelectedData(superParcel: SuperParcel): SuperParcel{
        val newParcel = SuperParcel();

        for (item in superParcel.getAllItems()){
            if (item.mimeType != MimeHelper.getMimeType(MimeHelper.TXT)){
                newParcel.addItem(item);
            }
        }
        return newParcel;
    }

    fun sendSelectedData(superParcel: SuperParcel) {
        GlobalScope.launch(Dispatchers.IO) {
            val result = TcpConnectionService.instance.sendParcelAsync(superParcel.getFullByteParcel())

            when (result){
                is Result.Success -> Log.d(TAG, "Send success: ${result.data}")
                is Result.Failure -> Log.d(TAG, "Send failed: ${result.error}")
            }
        }
    }

    val context = LocalContext.current

    var text by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()
    var superParcel by remember { mutableStateOf(SuperParcel()) }

    val itemList = superParcel.getAllItems()

    Box {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, top = 80.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Select Image Card
            Box(modifier = Modifier.padding(horizontal = 32.dp)) {
                Row {
                    Box(modifier = Modifier.weight(1.0f)) {
                        SelectImagesCard {
                            handleImageSelect(context, superParcel, it)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1.0f)){
                        SelectFilesCard {
                            handleFileSelect(context, superParcel, it)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // TextField row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.TextFields,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                StockTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.width(ScreenUtils.getScreenSize().width * 0.65f)
                )
                IconButton(onClick = { handleTextSelect(superParcel, text) }) {
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // IMAGE selection label
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Selected Files:", fontSize = 16.sp)
                IconButton(
                    onClick = {
                        superParcel = removeFilesFromSelectedData(superParcel);
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color.Red
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Image Scroll Row
            Row(
                modifier = Modifier
                    .height(150.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                var count = 0

                itemList.forEach {
                    if (it.mimeType != MimeHelper.getMimeType(MimeHelper.TXT)) {
                        SelectedFile(it.rawData as File)
                        Spacer(modifier = Modifier.width(12.dp))
                        count += 1
                    }
                }

                if (count == 0) {
                    Text("No Selected Files", style = TextStyle(fontStyle = FontStyle.Italic))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // TEXT selection label
            Row (
                verticalAlignment = Alignment.CenterVertically
            ){
                Text("Selected Text:", fontSize = 16.sp)
                IconButton(
                    onClick = {
                        superParcel = removeTextFromSelectedData(superParcel);
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color.Red
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Text Scroll Row
            Row(
                modifier = Modifier
                    .height(150.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                var count = 0
                itemList.forEach {
                    if (it.mimeType == MimeHelper.getMimeType(MimeHelper.TXT)) {
                        SelectedText(it.rawData as String)
                        Spacer(modifier = Modifier.width(12.dp))
                        count += 1
                    }
                }
                if (count == 0) {
                    Text("No Selected Text", style = TextStyle(fontStyle = FontStyle.Italic))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Send Button
            Box(
                modifier = Modifier
                    .height(60.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Surface(
                    onClick = { sendSelectedData(superParcel) },
                    shape = RoundedCornerShape(20.dp),
//                    tonalElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("Send to Laptop", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(200.dp))
        }

        Header()
    }
}

//@Composable
//fun RandomTextCard() {
//    val sampleText = "Lorem ipsum dolor sit amet..."
//    Card(
//        shape = RoundedCornerShape(16.dp),
//        border = BorderStroke(2.dp, Color.White),
//        modifier = Modifier
//            .padding(8.dp)
//            .widthIn(min = 50.dp, max = 250.dp)
//    ) {
//        Text(
//            sampleText,
//            overflow = TextOverflow.Ellipsis,
//            softWrap = false,
//            modifier = Modifier.padding(8.dp)
//        )
//    }
//}
//
//@Composable
//fun RandomImage() {
//    val width = remember { 100 + Random.nextInt(151) }
//    val height = remember { 100 + Random.nextInt(151) }
//
//    Box(
//        modifier = Modifier
//            .padding(horizontal = 8.dp)
//            .size(width.dp, height.dp)
//            .clip(RoundedCornerShape(16.dp))
//            .background(Color.DarkGray),
//        contentAlignment = Alignment.Center
//    ) {
//        Text(
//            text = "$width×$height",
//            color = Color.White,
//            fontSize = 14.sp
//        )
//    }
//}


//        Log.d("FINAL PARCEL", String(superParcel.getFullByteParcel(), Charsets.UTF_8))
//        val reparseResult = SuperParcel.fromParcelBytes(superParcel.getFullByteParcel())

//        when (reparseResult){
//            is Result.Success -> {
//                reparseResult.data.getAllItems().forEach{
//                    var v = it
//                    Log.d("SendDataPage", "${v.mimeType}|${v.rawData}")
//                }
//            }
//            is Result.Failure -> {
//                Log.d("SendDataPage", reparseResult.error)
//            }
//        }

//
//@Composable
//fun OpenFilesButton(context: Context) {
//    Box(
//        modifier = Modifier
//            .height(60.dp)
//            .fillMaxWidth()
//            .padding(horizontal = 24.dp)
//    ) {
//        Surface(
//            onClick = {
//                try {
//                    // Create or reference a dummy file inside filesDir
//                    val file = File(context.filesDir, "example.txt")
//                    if (!file.exists()) {
//                        file.writeText("Hello from SuperUtils!")
//                    }
//
//                    val uri = FileProvider.getUriForFile(
//                        context,
//                        "${context.packageName}.fileprovider",
//                        file
//                    )
//
//                    val intent = Intent(Intent.ACTION_VIEW).apply {
//                        setDataAndType(uri, "text/plain")
//                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
//                    }
//
//                    context.startActivity(Intent.createChooser(intent, "Open with"))
//                } catch (e: Exception) {
//                    Toast.makeText(context, "No app can handle this request", Toast.LENGTH_SHORT).show()
//                }
//            },
//            shape = RoundedCornerShape(20.dp),
//        ) {
//            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
//                Text("Open Files", fontSize = 24.sp, fontWeight = FontWeight.Bold)
//            }
//        }
//    }
//}
