import org.gradle.api.tasks.PathSensitivity

buildscript {
    dependencies {
        classpath("com.fsql:fsql-gradle-plugin:1.0.0")
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

apply(plugin="com.fsql.compile")

android {
    namespace = "com.itsethanhook.firebasedata"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.itsethanhook.firebasedata"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
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
    implementation(project(":firebase-data"))
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.kotlin.test.junit)
}

// forces unit tests to depend on compiled execution plans
tasks.withType<Test>().configureEach {
    inputs.files(tasks.named("compileFsql"))
        .withPropertyName("fsqlPlans")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}