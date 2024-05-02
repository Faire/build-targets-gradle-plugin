plugins {
  id("faire.build-targets")
  kotlin("jvm")
  application
}

dependencies {
  implementation(project(":dependency-project"))
}
