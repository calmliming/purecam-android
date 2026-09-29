package com.purecam.app.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.purecam.app.camera.FlashMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CameraSettingsStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var store: CameraSettingsStore

    @Before
    fun setUp() {
        dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            File(folder.root, "camera_settings.preferences_pb")
        }
        store = CameraSettingsStore(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun loadsDefaultsWhenNothingIsSaved() = runBlocking {
        assertEquals(CameraSettings(useFrontCamera = false, flashMode = FlashMode.OFF), store.load())
    }

    @Test
    fun loadsWhatWasSaved() = runBlocking {
        val settings = CameraSettings(useFrontCamera = true, flashMode = FlashMode.AUTO)
        store.save(settings)
        assertEquals(settings, store.load())
    }

    // 键名和取值就是手机上已保存的格式，改了会让用户的设置丢失
    @Test
    fun readsTheStoredFormat() = runBlocking {
        dataStore.edit {
            it[booleanPreferencesKey("use_front_camera")] = true
            it[stringPreferencesKey("flash_mode")] = "ON"
        }
        assertEquals(CameraSettings(useFrontCamera = true, flashMode = FlashMode.ON), store.load())
    }

    @Test
    fun unknownFlashModeFallsBackToOff() = runBlocking {
        dataStore.edit { it[stringPreferencesKey("flash_mode")] = "TORCH" }
        assertEquals(FlashMode.OFF, store.load().flashMode)
    }
}
