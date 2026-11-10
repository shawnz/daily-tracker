import com.android.build.api.variant.HasUnitTest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.room3)
}

android {
    namespace = "org.shawnz.dailytracker"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "org.shawnz.dailytracker"
        minSdk = 26
        targetSdk = 37
        versionCode = 12
        versionName = "1.11"
    }

    buildTypes {
        release {
            // Allow testing of release builds with the debug key.
            signingConfig = signingConfigs.getByName("debug")
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            // The protobuf library includes the .proto files for its standard types. The lite
            // runtime doesn't read them.
            excludes += "**/*.proto"
        }
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

protobuf {
    protoc {
        artifact =
            libs.protobuf.protoc
                .get()
                .toString()
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") { option("lite") }
                create("kotlin")
            }
        }
    }
}

android {
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    // The migration tests read the exported schemas as assets. Unit tests use the debug assets.
    sourceSets {
        getByName("debug") {
            assets.directories.add("$projectDir/schemas")
        }
    }
}

// Unit tests run on JUnit 6. The vintage engine runs the few that need Robolectric, which
// has a JUnit 4 runner and no Jupiter equivalent.
tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    // Silences a JDK 24+ warning: https://github.com/robolectric/robolectric/issues/11470
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

// The Android build of the bundled SQLite driver has native code for Android only. The JVM build
// is used for unit tests instead, because it has native code for the machine the tests run on.
val bundledSqlite =
    libs.androidx.sqlite.bundled
        .asProvider()
        .get()
        .module
        .toString()
val bundledSqliteJvm =
    libs.androidx.sqlite.bundled.jvm
        .get()
        .toString()
androidComponents {
    onVariants { variant ->
        (variant as HasUnitTest).unitTest?.let { unitTest ->
            with(unitTest.runtimeConfiguration.resolutionStrategy.dependencySubstitution) {
                substitute(module(bundledSqlite)).using(module(bundledSqliteJvm))
            }
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.room3.runtime)
    implementation(libs.androidx.sqlite.bundled)
    ksp(libs.androidx.room3.compiler)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.datastore)
    implementation(libs.protobuf.kotlin.lite)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.androidx.room3.testing)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
