package com.purecam.app.camera

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
private val ALBUM_PATH = "${Environment.DIRECTORY_PICTURES}/$ALBUM_NAME"

/** 左下角缩略图对应的照片：在系统相册里的地址，以及缩略图 */
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
        put(MediaStore.MediaColumns.RELATIVE_PATH, ALBUM_PATH)
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

/**
 * 查询 Pictures/PureCam 里最新的一张照片，一张都没有时结果为 null，查询出错时返回失败。
 * Android 10 起不需要读取相册的权限，但只能查到本应用保存的照片：
 * 卸载重装或清除应用数据后，以前拍的照片就不算本应用的了。
 */
suspend fun queryLatestPhoto(context: Context): Result<Uri?> = withContext(Dispatchers.IO) {
    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val queryArgs = Bundle().apply {
        // MediaStore 保存的相对路径末尾带 /，不带的写法也一起匹配，以防个别系统不一致
        putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.Images.Media.RELATIVE_PATH} IN (?, ?)")
        putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf("$ALBUM_PATH/", ALBUM_PATH))
        putString(
            ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
            "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC",
        )
        putInt(ContentResolver.QUERY_ARG_LIMIT, 1)
    }
    runCatching {
        context.contentResolver.query(collection, arrayOf(MediaStore.Images.Media._ID), queryArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) ContentUris.withAppendedId(collection, cursor.getLong(0)) else null
        }
    }
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
