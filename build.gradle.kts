// Top-level build file: declare plugins for all modules without applying them here.
plugins {
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.kotlin.android).apply(false)
    alias(libs.plugins.kotlin.compose).apply(false)
    alias(libs.plugins.kotlin.kapt).apply(false)
    alias(libs.plugins.hilt).apply(false)
    alias(libs.plugins.kover).apply(false)
}
