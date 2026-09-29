package com.purecam.app.settings

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.purecam.app.camera.FlashMode
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

private const val TAG = "PureCam"

// 设置文件损坏时换成空设置，全部回到默认值
private val Context.settingsDataStore by preferencesDataStore(
    name = "camera_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

// 键名就是保存在手机上的格式，改名会让用户的设置丢失
private val USE_FRONT_CAMERA = booleanPreferencesKey("use_front_camera")
private val FLASH_MODE = stringPreferencesKey("flash_mode")

/** 重新打开应用后要保留的拍摄设置 */
data class CameraSettings(
    val useFrontCamera: Boolean = false,
    /** 后置镜头的闪光灯模式；前置镜头没有闪光灯 */
    val flashMode: FlashMode = FlashMode.OFF,
)

/** 拍摄设置的读写。读写失败只记日志，按默认设置继续拍照 */
class CameraSettingsStore(private val dataStore: DataStore<Preferences>) {
    constructor(context: Context) : this(context.settingsDataStore)

    suspend fun load(): CameraSettings {
        val preferences = dataStore.data
            .catch { e ->
                if (e !is IOException) throw e
                Log.w(TAG, "Failed to read camera settings", e)
                emit(emptyPreferences())
            }
            .first()
        return CameraSettings(
            useFrontCamera = preferences[USE_FRONT_CAMERA] ?: false,
            // 认不出的值（比如被新版本写入的档位）按默认处理
            flashMode = FlashMode.entries.find { it.name == preferences[FLASH_MODE] } ?: FlashMode.OFF,
        )
    }

    /** 保存全部设置。和已保存的一样时 DataStore 不会重复写文件 */
    suspend fun save(settings: CameraSettings) {
        try {
            dataStore.edit {
                it[USE_FRONT_CAMERA] = settings.useFrontCamera
                it[FLASH_MODE] = settings.flashMode.name
            }
        } catch (e: IOException) {
            Log.w(TAG, "Failed to save camera settings", e)
        }
    }
}
