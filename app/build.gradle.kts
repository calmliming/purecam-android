import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // AGP 9 已内置 Kotlin 支持，不需要再应用 org.jetbrains.kotlin.android 插件
    alias(libs.plugins.kotlin.compose)
}

// 发布签名信息放在项目根目录的 keystore.properties 里（含密码，已被 .gitignore 忽略）。
// 没有这个文件时（比如刚克隆的项目）照常构建 release 包，只是不签名。
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.reader().use { load(it) }
}

fun keystoreProperty(name: String): String =
    keystoreProperties.getProperty(name) ?: error("keystore.properties is missing \"$name\"")

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

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProperty("storeFile"))
                storePassword = keystoreProperty("storePassword")
                keyAlias = keystoreProperty("keyAlias")
                keyPassword = keystoreProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
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
