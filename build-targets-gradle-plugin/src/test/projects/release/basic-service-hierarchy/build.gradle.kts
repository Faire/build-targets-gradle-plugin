import com.faire.gradle.release.ShowBuildTargetsForChangeExtension

plugins {
  alias(libs.plugins.kotlin) apply false
  id("com.faire.build-targets")
}

allprojects {
  repositories {
    mavenCentral()
  }
}
