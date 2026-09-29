plugins {
    alias(libs.plugins.android.application)
    // AGP 9 已内置 Kotlin 支持，不需要再应用 org.jetbrains.kotlin.android 插件
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.purecam.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.purecam.app"
        // Android 10+：保存照片到相册不需要存储权限，也能直接用系统缩略图接口
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // 在 Compose 里读取 CameraX 的对焦、变焦状态（LiveData）
    implementation(libs.androidx.compose.runtime.livedata)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
}
