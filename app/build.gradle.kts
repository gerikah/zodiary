plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun readLocalValue(vararg names: String): String {
    names.firstNotNullOfOrNull { name ->
        providers.environmentVariable(name).orNull?.takeIf(String::isNotBlank)
    }?.let { return it }
    val envFile = rootProject.file(".env")
    if (!envFile.isFile) return ""
    val lines = envFile.useLines { it.toList() }
    return names.firstNotNullOfOrNull { name ->
        lines.firstNotNullOfOrNull { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || !line.startsWith("$name=")) {
                null
            } else {
                line.substringAfter('=').trim().removeSurrounding("\"").removeSurrounding("'")
            }
        }
    } ?: ""
}

fun String.asBuildConfigString(): String = "\"" +
    replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n") + "\""

val supabaseUrl = readLocalValue("NEXT_PUBLIC_SUPABASE_URL", "SUPABASE_URL")
val supabasePublishableKey = readLocalValue("NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY", "SUPABASE_PUBLISHABLE_KEY")

android {
    namespace = "com.zodiary.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.zodiary.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "SUPABASE_URL", supabaseUrl.asBuildConfigString())
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", supabasePublishableKey.asBuildConfigString())
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation(platform("io.github.jan-tennert.supabase:bom:3.9.0"))
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.ktor:ktor-client-android:3.4.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20260814")
}
