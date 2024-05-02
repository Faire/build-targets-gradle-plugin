rootProject.name = "build-logic"

val javaVersionContents = rootDir.resolve(".java-version").readText()
val devJavaVersion = JavaVersion.toVersion(javaVersionContents)
if (JavaVersion.current() != devJavaVersion) {
  throw GradleException("You're using java version ${JavaVersion.current()} but $devJavaVersion is required.")
}

plugins {
    id("com.gradle.develocity") version("3.17.2")
}

develocity {
  buildScan {
      termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
      termsOfUseAgree.set("yes")
  }
}

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}
