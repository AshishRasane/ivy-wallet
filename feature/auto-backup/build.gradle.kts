plugins {
    id("ivy.module")
}

android {
    namespace = "com.ivy.autobackup"
}

dependencies {
    implementation(projects.shared.base)
    implementation(projects.shared.data.core)
    implementation(projects.shared.domain)
    implementation(projects.temp.legacyCode)

    implementation(libs.androidx.work)
    implementation(libs.androidx.documentfile)
    implementation(libs.datastore)
}
