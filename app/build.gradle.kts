plugins { id("com.android.application") }

android {
    namespace = "cn.edu.ecjtu"
    compileSdk = 36
    defaultConfig {
        applicationId = "cn.edu.ecjtu"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
    }
    signingConfigs {
        create("fixed") {
            storeFile = rootProject.file("114514.jks")
            storePassword = System.getenv("ECJTU_KEYSTORE_PASSWORD")
            keyAlias = "114514"
            keyPassword = System.getenv("ECJTU_KEY_PASSWORD")
        }
    }
    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("fixed")
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("fixed")
        }
    }
    configurations.all {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    constraints {
        implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.22")
    }
}
