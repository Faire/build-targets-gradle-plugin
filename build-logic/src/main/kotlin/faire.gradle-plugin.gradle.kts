plugins {
  id("faire.kotlin")

  `java-gradle-plugin`

  id("com.gradle.plugin-publish")
}

val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

repositories {
  gradlePluginPortal()
}

dependencies {
  compileOnly(gradleApi())

  implementation(gradleKotlinDsl())

  testImplementation(gradleTestKit())

  testImplementation(libs.findLibrary("assertj").get())
  testImplementation(libs.findLibrary("navatwo-gradle-better-testing-junit5").get())
  testImplementation(libs.findLibrary("navatwo-gradle-better-testing-asserts").get())

  testImplementation(libs.findLibrary("junit-jupiter-api").get())
  testRuntimeOnly(libs.findLibrary("junit-jupiter-engine").get())
}

tasks.test {
  useJUnitPlatform()

  systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
  systemProperty("net.navatwo.gradle.testkit.junit5.testKitDirectory", rootProject.projectDir.resolve(".gradle/testKits").toString())

  inputs.files(fileTree("src/test/projects").exclude("**/build/**"))
}
