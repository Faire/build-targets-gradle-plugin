rootProject.name = "faire-build-targets-gradle-plugin"

pluginManagement {
  includeBuild("./build-logic")

  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

include(
    ":build-targets-gradle-plugin",
    ":test-fixtures",
)

plugins {
    id("com.gradle.develocity") version("3.17.2")
}

develocity {
  buildScan {
      termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
      termsOfUseAgree.set("yes")
  }
}
