@file:Suppress("unused")

object Versions {

    // Release info
    const val minSdk = 16
    const val targetSdk = 27
    const val compileSdk = 27
    const val buildTools = "27.0.3"

    // Build tools and languages
    const val androidPlugin = "3.0.1"
    const val kotlin = "1.2.30"
    const val googleServicesPlugin = "3.1.2"
    const val coveralls = "2.8.1"
    const val buildProperties = "0.3"

    // Support Libraries
    const val supportLibs = "27.1.0"
    const val googleServices = "11.8.0"
    const val constraintLayout = "1.0.2"
    const val supportTesting = "1.0.1"
    const val multidex = "1.0.2"

    // Networking, RxJava
    const val retrofit = "2.3.0"
    const val okHttp = "3.9.0"
    const val dagger = "2.14.1"
    const val rxJava = "2.1.10"
    const val rxAndroid = "2.0.2"
    const val rxBinding = "2.1.1"
    const val rxFingerprint = "2.2.1"

    // Utils, Ethereum
    const val web3j = "2.2.1"
    // Keep at 1.3 to match Android
    const val commonsCodec = "1.3"
    const val commonsLang = "3.4"
    const val urlBuilder = "2.0.8"
    const val yearclass = "2.0.0"
    const val protobuf = "2.6.1"
    const val findbugs = "2.0.1"
    const val guava = "24.0-android"

    // Custom Views
    const val charts = "3.0.3"
    const val calligraphy = "2.2.0"
    const val circleIndicator = "1.2.2"
    const val bottomNav = "2.2.0"
    const val countryPicker = "1.1.7"
    const val zxing = "3.3.0"

    // Logging
    const val timber = "4.6.0"
    const val slf4j = "1.7.20"
    const val crashlytics = "2.9.1"
    const val fabricTools = "1.24.4"

    // Testing
    const val mockito = "2.8.47"
    const val mockitoKotlin = "1.5.0"
    const val kluent = "1.19"
    const val hamcrestJunit = "2.0.0.0"
    const val junit = "4.12"
    const val robolectric = "3.7.1"
    const val json = "20140107"
    const val espresso = "3.0.1"

}

object Libs {

    // Build tools and languages
    const val androidGradlePlugin = "com.android.tools.build:gradle:${Versions.androidPlugin}"
    const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val kotlin = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}"
    const val kotlinAllOpen = "org.jetbrains.kotlin:kotlin-allopen:${Versions.kotlin}"
    const val coveralls = "org.kt3k.gradle.plugin:coveralls-gradle-plugin:${Versions.coveralls}"
    const val googleServicesPlugin =
            "com.google.gms:google-services:${Versions.googleServicesPlugin}"
    const val buildProperties = "com.novoda:gradle-build-properties-plugin:${Versions.buildProperties}"

    // Support Libraries
    const val appCompat = "com.android.support:appcompat-v7:${Versions.supportLibs}"
    const val recyclerView = "com.android.support:recyclerview-v7:${Versions.supportLibs}"
    const val cardView = "com.android.support:cardview-v7:${Versions.supportLibs}"
    const val gridLayout = "com.android.support:gridlayout-v7:${Versions.supportLibs}"
    const val design = "com.android.support:design:${Versions.supportLibs}"
    const val v13 = "com.android.support:support-v13:${Versions.supportLibs}"
    const val v14 = "com.android.support:preference-v14:${Versions.supportLibs}"
    const val dynamicAnims = "com.android.support:support-dynamic-animation:${Versions.supportLibs}"
    const val annotations = "com.android.support:support-annotations:${Versions.supportLibs}"
    const val constraintLayout =
            "com.android.support.constraint:constraint-layout:${Versions.constraintLayout}"
    const val dataBindingKapt = "com.android.databinding:compiler:${Versions.androidPlugin}"
    const val multidex = "com.android.support:multidex:${Versions.multidex}"

    // Google & Firebase
    const val firebaseMessaging =
            "com.google.firebase:firebase-messaging:${Versions.googleServices}"
    const val googlePlayServicesBase =
            "com.google.android.gms:play-services-base:${Versions.googleServices}"

    // Networking, RxJava
    const val retrofit = "com.squareup.retrofit2:retrofit:${Versions.retrofit}"
    const val retrofitJacksonConverter =
            "com.squareup.retrofit2:converter-jackson:${Versions.retrofit}"
    const val retrofitRxJavaAdapter = "com.squareup.retrofit2:adapter-rxjava2:${Versions.retrofit}"
    const val okHttp = "com.squareup.okhttp3:okhttp:${Versions.okHttp}"
    const val okHttpInterceptor = "com.squareup.okhttp3:logging-interceptor:${Versions.okHttp}"
    const val dagger = "com.google.dagger:dagger:${Versions.dagger}"
    const val daggerKapt = "com.google.dagger:dagger-compiler:${Versions.dagger}"
    const val rxJava = "io.reactivex.rxjava2:rxjava:${Versions.rxJava}"
    const val rxAndroid = "io.reactivex.rxjava2:rxandroid:${Versions.rxAndroid}"
    const val rxBinding = "com.jakewharton.rxbinding2:rxbinding-support-v4:${Versions.rxBinding}"
    const val rxFingerprint = "com.mtramin:rxfingerprint:${Versions.rxFingerprint}"

    // Utils, Ethereum
    const val web3j = "org.web3j:core-android:${Versions.web3j}"
    const val commonsCodec = "commons-codec:commons-codec:${Versions.commonsCodec}"
    const val commonsLang = "org.apache.commons:commons-lang3:${Versions.commonsLang}"
    const val urlBuilder = "io.mikael:urlbuilder:${Versions.urlBuilder}"
    const val yearclass = "com.facebook.device.yearclass:yearclass:${Versions.yearclass}"
    const val protobuf = "com.google.protobuf:protobuf-java:${Versions.protobuf}"
    const val findbugs = "com.google.code.findbugs:jsr305:${Versions.findbugs}"
    const val guava = "com.google.guava:guava:${Versions.guava}"

    // Custom Views
    const val charts = "com.github.PhilJay:MPAndroidChart:v${Versions.charts}"
    const val calligraphy = "uk.co.chrisjenx:calligraphy:${Versions.calligraphy}"
    const val circleIndicator = "me.relex:circleindicator:${Versions.circleIndicator}@aar"
    const val bottomNav = "com.aurelhubert:ahbottomnavigation:${Versions.bottomNav}"
    const val countryPicker =
            "com.github.mukeshsolanki:country-picker-android:${Versions.countryPicker}"
    const val zxing = "com.google.zxing:core:${Versions.zxing}"

    // Logging
    const val timber = "com.jakewharton.timber:timber:${Versions.timber}"
    const val slf4j = "org.slf4j:slf4j-simple:${Versions.slf4j}"
    const val slf4jNoOp = "org.slf4j:slf4j-nop:${Versions.slf4j}"
    const val crashlytics = "com.crashlytics.sdk.android:crashlytics:${Versions.crashlytics}@aar"
    const val fabricTools = "io.fabric.tools:gradle:${Versions.fabricTools}"

    // Testing
    const val mockito = "org.mockito:mockito-core:${Versions.mockito}"
    const val mockitoKotlin = "com.nhaarman:mockito-kotlin:${Versions.mockitoKotlin}"
    const val kluent = "org.amshove.kluent:kluent:${Versions.kluent}"
    const val kotlinJunit = "org.jetbrains.kotlin:kotlin-test-junit:${Versions.kotlin}"
    const val hamcrestJunit = "org.hamcrest:hamcrest-junit:${Versions.hamcrestJunit}"
    const val junit = "junit:junit:${Versions.junit}"
    const val robolectric = "org.robolectric:robolectric:${Versions.robolectric}"
    const val json = "org.json:json:${Versions.json}"
    const val testRules = "com.android.support.test:rules:${Versions.supportTesting}"
    const val testRunner = "com.android.support.test:runner:${Versions.supportTesting}"
    const val espresso = "com.android.support.test.espresso:espresso-core:${Versions.espresso}"

}