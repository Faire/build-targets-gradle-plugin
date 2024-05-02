package com.faire.gradle.kotlin

import net.navatwo.gradle.testkit.assertj.task
import net.navatwo.gradle.testkit.junit5.GradleProject
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import java.io.File
import java.util.zip.ZipFile

class FaireKotlinGradleTest {
  @Test
  @GradleProject("faire-kotlin-apply-and-compile")
  fun `compile simple project`(
      @GradleProject.Runner gradleRunner: GradleRunner,
      @GradleProject.Root root: File,
  ) {
    val build = gradleRunner
        .withArguments("build")
        .build()

    assertThat(build).task(":compileKotlin").isSuccess()
    assertThat(build).task(":test").isNoSource()
    assertThat(build).task(":assemble").isSuccess()
    assertThat(build).task(":build").isSuccess()

    val jarFile = root.resolve("build/libs/test-project.jar")
    assertThat(jarFile).exists()

    ZipFile(jarFile).use { jar ->
      assertThat(jar.getEntry("com/faire/test/TestClass.class")).isNotNull()
    }
  }
}
