plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val assistantProxyUrl = providers.gradleProperty("assistantProxyUrl").orElse("").get()
val assistantAppToken = providers.gradleProperty("assistantToken").orElse("").get()

android {
    namespace = "uz.carlcare.salesassistant"
    compileSdk = 35

    defaultConfig {
        applicationId = "uz.carlcare.salesassistant"
        minSdk = 29
        targetSdk = 35
        versionCode = 11
        versionName = "1.7.0"

        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "ASSISTANT_PROXY_URL", "\"$assistantProxyUrl\"")
        buildConfigField("String", "ASSISTANT_APP_TOKEN", "\"$assistantAppToken\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:design"))

    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("io.coil-kt:coil-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
