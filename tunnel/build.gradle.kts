@file:Suppress("UnstableApiUsage")

import org.gradle.api.tasks.testing.logging.TestLogEvent

val pkg: String = providers.gradleProperty("amneziawgPackageName").get()
val cmakeAndroidPackageName: String = providers.environmentVariable("ANDROID_PACKAGE_NAME").getOrElse(pkg)

plugins {
    alias(libs.plugins.android.library)
}

val purgeLegacyWgGoArtifacts by tasks.registering(Delete::class) {
    delete(fileTree(layout.buildDirectory) {
        include("**/libwg-go.so")
        include("**/libwg.so")
        include("**/libwg-quick.so")
    })
}

android {
    compileSdk = 36
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    namespace = "${pkg}.tunnel"
    externalNativeBuild {
        cmake {
            path("tools/CMakeLists.txt")
        }
    }
    testOptions.unitTests.all {
        it.testLogging { events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED) }
    }
    buildTypes {
        all {
            externalNativeBuild {
                cmake {
                    targets("libamneziawg_go.so", "libamneziawg.so", "libamneziawg_quick.so")
                    arguments("-DGRADLE_USER_HOME=${project.gradle.gradleUserHomeDir}")
                }
            }
        }
        release {
            externalNativeBuild {
                cmake {
                    arguments("-DANDROID_PACKAGE_NAME=${cmakeAndroidPackageName}")
                }
            }
        }
        debug {
            externalNativeBuild {
                cmake {
                    arguments("-DANDROID_PACKAGE_NAME=${cmakeAndroidPackageName}.debug")
                }
            }
        }
    }
    lint {
        disable += "LongLogTag"
        disable += "NewApi"
    }
}

tasks.named("preBuild") {
    dependsOn(purgeLegacyWgGoArtifacts)
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.collection)
    compileOnly(libs.jsr305)
    testImplementation(libs.junit)
}
