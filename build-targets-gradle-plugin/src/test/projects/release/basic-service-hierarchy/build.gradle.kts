plugins {
  alias(libs.plugins.kotlin) apply false
  id("faire.build-targets")
}

allprojects {
  repositories {
    mavenCentral()
  }
}
