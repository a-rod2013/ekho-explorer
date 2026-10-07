plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.fsql.data"
    compileSdk { version = release(37) }

    defaultConfig {
        minSdk { version = release(24) }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    api(platform(libs.firebase.bom))
    api(libs.firebase.firestore)
    api(libs.firebase.auth)
    api(libs.kotlinx.coroutines.core)
    implementation("com.fsql:fsql-plan:1.0.0")
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
