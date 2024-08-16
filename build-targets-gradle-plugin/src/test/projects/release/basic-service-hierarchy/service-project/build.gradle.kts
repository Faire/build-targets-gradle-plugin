plugins {
  id("com.faire.build-targets")
  kotlin("jvm")
  application
}

dependencies {
  implementation(project(":dependency-project"))
}
