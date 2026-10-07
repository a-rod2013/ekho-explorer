plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":fsql-compiler"))
    implementation(project(":fsql-plan"))
    compileOnly(libs.agp.api)
    testImplementation(gradleTestKit())
    testImplementation(libs.kotlin.test.junit)
}

gradlePlugin {
    plugins {
        create("fsql") {
            id = "com.fsql.compile"
            implementationClass = "com.fsql.gradle.FsqlPlugin"
        }
    }
}

tasks.test { useJUnit() }

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}