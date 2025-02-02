plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.aura_demo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.aura_demo"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.glide)
    implementation(libs.firebase.database)
    implementation(libs.swiperefreshlayout)
    annotationProcessor(libs.compiler)
    implementation("cn.leancloud:realtime-android:8.2.28") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }

    // 排除 Firebase 引入的 protobuf-javalite
    implementation(libs.firebase.firestore) {
        exclude(group = "com.google.protobuf", module = "protobuf-javalite")
    }
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.firebase.auth)
//    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("pub.devrel:easypermissions:3.0.0")
    implementation("com.github.yalantis:ucrop:2.2.7")
//    implementation("cn.leancloud.android:leancloud-android-sdk:4.4.0")
//    implementation 'com.github.yalantis:ucrop:2.2.6'

//    implementation("com.theartofdev.edmodo:android-image-cropper:2.8.0")

//    implementation ("cn.leancloud:android-sdk:3.7.5")
}
configurations.all {
    resolutionStrategy {
        force("com.google.protobuf:protobuf-java:3.16.3") // 强制使用 protobuf-java 版本
//        force("com.google.protobuf:protobuf-javalite:3.25.1")
    }
}
