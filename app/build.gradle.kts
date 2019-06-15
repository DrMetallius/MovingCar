plugins {
	id("com.android.application")
	kotlin("android")
	kotlin("android.extensions")
}

val kotlinVersion: String by project

android {
	compileSdkVersion(28)
	defaultConfig {
		applicationId = "com.malcolmsoft.movingcar"
		minSdkVersion(21)
		targetSdkVersion(28)
		versionCode = 1
		versionName = "1.0"
	}
}

dependencies {
	implementation(kotlin("stdlib-jdk7", kotlinVersion))

	implementation("androidx.core:core-ktx:1.0.2")
	implementation("androidx.fragment:fragment-ktx:1.0.0")
	implementation("androidx.appcompat:appcompat:1.0.2")
	implementation("androidx.lifecycle:lifecycle-extensions:2.0.0")
	implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.0.0")
	implementation("androidx.constraintlayout:constraintlayout:1.1.3")
	implementation("com.google.android.material:material:1.0.0")
}
