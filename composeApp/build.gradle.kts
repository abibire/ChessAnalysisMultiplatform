import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(21)
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }

        // Configure cinterop for Stockfish bridge
        iosTarget.compilations.getByName("main") {
            cinterops {
                val stockfishBridge by creating {
                    defFile(project.file("src/nativeInterop/cinterop/StockfishBridge.def"))
                    packageName("stockfish")
                    includeDirs(project.file("src/nativeInterop/cinterop/headers"))
                }
            }
        }
    }

    jvm()

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation("io.ktor:ktor-client-okhttp:3.0.2")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("io.github.cvb941:kchesslib:1.0.3")
            implementation("io.coil-kt.coil3:coil-compose:3.3.0")
            implementation("io.coil-kt.coil3:coil-network-ktor3:3.3.0")
            implementation("io.coil-kt.coil3:coil-svg:3.3.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            implementation(compose.materialIconsExtended)
            implementation("io.ktor:ktor-client-core:3.0.2")
            implementation("io.ktor:ktor-client-content-negotiation:3.0.2")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.2")
            implementation(libs.multiplatform.settings.noarg)
            implementation("dev.carlsen.flagkit:flagkit:1.1.0")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation("io.ktor:ktor-client-okhttp:3.0.2")
        }
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:3.0.2")
        }
    }
}

android {
    namespace = "com.andrewbibire.chessanalysis"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.andrewbibire.chessanalysis"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/androidMain/jniLibs")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.andrewbibire.chessanalysis.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.andrewbibire.chessanalysis"
            packageVersion = "1.0.0"

            // Set app icon for all desktop platforms
            macOS {
                iconFile.set(project.file("src/jvmMain/resources/app-icon.icns"))
            }
            windows {
                iconFile.set(project.file("src/jvmMain/resources/app-icon.png"))
            }
            linux {
                iconFile.set(project.file("src/jvmMain/resources/app-icon.png"))
            }
        }
    }
}