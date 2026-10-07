plugins {
    id("ivy.module")
}

android {
    namespace = "com.ivy.smstransactions"
}

dependencies {
    implementation(projects.shared.base)
    implementation(projects.shared.domain)
    implementation(projects.shared.ui.core)
    implementation(projects.temp.legacyCode)
}
