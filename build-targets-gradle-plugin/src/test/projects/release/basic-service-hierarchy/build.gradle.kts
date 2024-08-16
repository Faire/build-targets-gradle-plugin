plugins {
  alias(libs.plugins.kotlin) apply false
  id("com.faire.build-targets")
}

allprojects {
  repositories {
    mavenCentral()
  }
}
