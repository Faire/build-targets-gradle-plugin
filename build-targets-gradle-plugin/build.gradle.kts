plugins {
  `kotlin-dsl`

  id("faire.detekt")
  id("faire.gradle-plugin")
}

group = "com.faire"
version = "1.0.0"

dependencies {
  api(kotlin("stdlib"))

  implementation(gradleKotlinDsl())
  implementation(libs.gson)

  // only use junit plugin
  testRuntimeOnly(project(":test-fixtures"))
}

gradlePlugin {
  plugins {
    create("faire.build-targets") {
      id = "faire.build-targets"
      displayName = "Show the impacted targets for a project"
      implementationClass = "com.faire.gradle.release.ShowBuildTargetsForChangePlugin"
    }
  }
}
