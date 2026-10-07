plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    api(project(":fsql-plan"))
    implementation(libs.jsqlparser) {
        // jsqlparser depends on jmh-core (a benchmarking tool) and also ships some of the same
        // resource files inside its own jar. We never run benchmarks, so drop it entirely.
        exclude(group = "org.openjdk.jmh")
    }
    testImplementation(libs.kotlin.test.junit)
}

tasks.test { useJUnit() }

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}