plugins {
  `kotlin-dsl`

  id("faire.detekt")
  id("faire.gradle-plugin")
}

group = "com.faire"
version = "0.0.1"

dependencies {
  api(kotlin("stdlib"))

  implementation(gradleKotlinDsl())
  implementation(libs.gson)

  // only use junit plugin
  testRuntimeOnly(testFixtures(project(":test-fixtures")))
}

gradlePlugin {
  website = "https://github.com/Faire/build-targets-gradle-plugin"
  vcsUrl = "https://github.com/Faire/build-targets-gradle-plugin.git"

  plugins {
    create("faire.build-targets") {
      id = "com.faire.build-targets"
      displayName = "faire.build-targets"
      description = "Show the impacted targets for a project"
      implementationClass = "com.faire.gradle.release.ShowBuildTargetsForChangePlugin"
      tags = listOf("ci", "continuous-integration", "merge-queue", "release")
    }
  }
}
