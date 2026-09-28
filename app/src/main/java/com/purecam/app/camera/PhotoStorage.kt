package com.purecam.app.camera

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

private const val ALBUM_NAME = "PureCam"

/** 刚拍的照片：在系统相册里的地址，以及左下角显示用的缩略图 */
data class CapturedPhoto(val uri: Uri, val thumbnail: ImageBitmap?)

/** 拍一张照片，保存到系统相册的 Pictures/PureCam 目录 */
fun CameraController.takePhotoToGallery(
    context: Context,
    onSuccess: (Uri) -> Unit,
    onFailure: (ImageCaptureException) -> Unit,
) {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(System.currentTimeMillis())
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "${ALBUM_NAME}_$timestamp")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM_NAME")
    }
    val options = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        values,
    ).build()

    takePicture(
        options,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                outputFileResults.savedUri?.let(onSuccess)
            }

            override fun onError(exception: ImageCaptureException) {
                onFailure(exception)
            }
        },
    )
}

/** 读取系统生成的缩略图（已按 EXIF 方向转正），失败时返回 null */
suspend fun loadThumbnail(context: Context, uri: Uri): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver.loadThumbnail(uri, Size(256, 256), null).asImageBitmap()
    }.getOrNull()
}

/** 用系统相册打开照片；设备上没有能看图的应用时什么也不做 */
fun openInGallery(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, "image/*")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { context.startActivity(intent) }
}
