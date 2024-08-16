plugins {
  `kotlin-dsl`
  id("faire.kotlin")
  `java-test-fixtures`
}

dependencies {
  testFixturesImplementation(gradleTestKit())

  testFixturesImplementation(libs.junit.jupiter.api)
  testFixturesImplementation(libs.navatwo.gradle.better.testing.junit5)
}
