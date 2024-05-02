import io.gitlab.arturbosch.detekt.Detekt

plugins {
  id("io.gitlab.arturbosch.detekt")

  java
  kotlin("jvm")
}

repositories {
  // Used for faire-detekt-rules detektPlugin
  maven {
    url = uri("https://jitpack.io")
  }
}

val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
  detektPlugins(libs.findLibrary("detekt.formatting").get())
  detektPlugins(libs.findLibrary("detekt.kotlin.compiler.wrapper").get())
  detektPlugins(libs.findLibrary("faire.detekt.rules").get())
}

val projectSources = files(
    project.sourceSets.main.map { it.kotlin.srcDirs },
    project.sourceSets.test.map { it.kotlin.srcDirs },
    project.file("build.gradle.kts"),
)

project.plugins.withId("java-test-fixtures") {
  projectSources.from(project.sourceSets.named("testFixtures").map { it.kotlin.srcDirs })
}

detekt {
  parallel = true
  allRules = true
  buildUponDefaultConfig = true

  source.from(
      projectSources.filter { it.extension in setOf("kt", "kts") }
  )

  config.from(rootProject.file("./detekt.yaml"), rootProject.file("./gradle-build.detekt.yaml"))
}

tasks.withType<Detekt>().configureEach {
  autoCorrect = true
  reports.sarif.required = true
}
