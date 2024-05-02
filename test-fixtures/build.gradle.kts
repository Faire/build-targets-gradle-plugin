plugins {
  `kotlin-dsl`
  id("faire.kotlin")
}

dependencies {
  implementation(gradleTestKit())

  implementation(libs.junit.jupiter.api)
  implementation(libs.navatwo.gradle.better.testing.junit5)
}
