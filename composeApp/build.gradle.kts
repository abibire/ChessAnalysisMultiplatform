import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.language.jvm.tasks.ProcessResources

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

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: "release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = if (System.getenv("KEYSTORE_FILE") != null) {
                signingConfigs.getByName("release")
            } else {
                null
            }
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

// Check if building for App Store
val isAppStoreBuild = true

compose.desktop {
    application {
        mainClass = "com.andrewbibire.chessanalysis.MainKt"

        nativeDistributions {
            // Add Pkg format for App Store, keep existing formats for regular distribution
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Pkg)
            packageName = "Game Review"
            packageVersion = "1.0.0"

            macOS {
                iconFile.set(project.file("src/jvmMain/resources/app-icon.icns"))
                bundleID = "com.andrewbibire.chessanalysismac"
                appCategory = "public.app-category.board-games"

                // Minimum system version - 12.0 required for arm64-only builds per Apple


                // Package versions

                dmgPackageVersion = "1.0.0"
                packageBuildVersion = "1.0.0"

                // Use different entitlements based on build type
                if (isAppStoreBuild) {
                    packageVersion = "1.0.1"
                    minimumSystemVersion = "12.0"
                    buildTypes.release.proguard {
                        isEnabled.set(false)
                    }
                    // App Store build - use sandboxed entitlements
                    entitlementsFile.set(project.file("entitlements-appstore.plist"))
                    runtimeEntitlementsFile.set(project.file("entitlements-appstore.plist"))

                    // Provisioning profiles - disabled for manual signing
                    // provisioningProfile.set(file("/Users/a/Downloads/game-review/appstorecerts/Game_Review_Mac_Appstore.provisionprofile"))
                    // runtimeProvisioningProfile.set(file("/Users/a/Downloads/game-review/appstorecerts/Chess_Analysis_Mac_JVM_Runtime.provisionprofile"))
                } else {
                    // Regular DMG build - use current entitlements
                    entitlementsFile.set(project.file("entitlements.plist"))
                    runtimeEntitlementsFile.set(project.file("runtime-entitlements.plist"))
                }

                // Disable automatic signing - we'll sign manually
                signing {
                    sign.set(false)
                }
            }
            windows {
                iconFile.set(project.file("src/jvmMain/resources/app-icon.ico"))
            }
            linux {
                iconFile.set(project.file("src/jvmMain/resources/app-icon-linux.png"))
            }
        }
    }
}

tasks.named("jvmProcessResources", ProcessResources::class) {
    val osName = System.getProperty("os.name").lowercase()

    when {
        osName.contains("windows") -> {
            exclude("stockfish/macos-*/**", "stockfish/linux-*/**")
        }
        osName.contains("mac") || osName.contains("darwin") -> {
            exclude("stockfish/windows-*/**", "stockfish/linux-*/**")
        }
        osName.contains("linux") -> {
            exclude("stockfish/windows-*/**", "stockfish/macos-*/**")
        }
    }
}
