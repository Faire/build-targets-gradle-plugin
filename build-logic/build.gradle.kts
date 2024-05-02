plugins {
  alias(libs.plugins.kotlin)

  // Do not specify a version for kotlin-dsl:
  // https://docs.gradle.org/current/userguide/kotlin_dsl.html#sec:kotlin-dsl_plugin
  `kotlin-dsl`
}

repositories {
  mavenCentral()
  gradlePluginPortal()
}

val javaVersionText = providers.fileContents(
    rootProject.layout.projectDirectory.file(".java-version"),
).asText.map { it.trim().substringBefore('.') }

kotlin {
  jvmToolchain {
    languageVersion.set(javaVersionText.map { JavaLanguageVersion.of(it) })
  }
}

java {
  toolchain {
    languageVersion.set(javaVersionText.map { JavaLanguageVersion.of(it) })
  }
}

dependencies {
  implementation(libs.pluginz.detekt)
  implementation(libs.pluginz.kotlin)

  testImplementation(gradleTestKit())

  testImplementation(libs.navatwo.gradle.better.testing.junit5)
  testImplementation(libs.navatwo.gradle.better.testing.asserts)
  testImplementation(libs.junit.jupiter.api)

  testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.test {
  useJUnitPlatform()

  systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
  systemProperty("net.navatwo.gradle.testkit.junit5.testKitDirectory", rootProject.projectDir.resolve(".gradle/testKits").toString())

  inputs.files(fileTree("src/test/projects").exclude("**/build/**"))
}
