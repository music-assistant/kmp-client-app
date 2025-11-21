plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

// Custom task to run checks without iOS (since Xcode SDK may not be available)
// Simply run: ./gradlew :composeApp:desktopTest :composeApp:testDebugUnitTest :composeApp:testReleaseUnitTest
// The above command is more reliable than a wrapper task
