plugins {
  kotlin("jvm")
  java

  id("faire.detekt")
}

repositories {
  mavenCentral()
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
