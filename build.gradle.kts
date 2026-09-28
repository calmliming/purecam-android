// 顶层构建文件：只声明插件版本，具体配置写在各模块里
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
