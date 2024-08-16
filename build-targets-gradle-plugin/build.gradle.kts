plugins {
  `kotlin-dsl`

  id("faire.detekt")
  id("faire.gradle-plugin")
}

group = "com.faire.gradle"
version = "0.0.0"

if (!providers.environmentVariable("RELEASE").isPresent) {
  val gitSha = providers.environmentVariable("GITHUB_SHA")
    .orElse(
      provider {
        // nest the provider, we don't want to invalidate the config cache for this
        providers.exec { commandLine("git", "rev-parse", "--short", "HEAD") }
          .standardOutput
          .asText
          .map { it.trim().take(10) }
          .get()
      },
    )
    .get()

  version = "$version-$gitSha-SNAPSHOT"
}

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
