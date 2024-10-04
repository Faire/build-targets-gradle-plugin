package com.faire.gradle.release

import com.faire.gradle.release.ShowBuildTargetsForChangePlugin.Companion.COMPUTE_RUNTIME_CLASSPATH_DEPENDENT_PROJECTS_TASK
import com.faire.gradle.release.ShowBuildTargetsForChangePlugin.Companion.COMPUTE_SOURCE_FOLDERS_TASK
import com.faire.gradle.release.ShowBuildTargetsForChangePlugin.Companion.SHOW_BUILD_TARGETS_TASK
import net.navatwo.gradle.testkit.assertj.task
import net.navatwo.gradle.testkit.junit5.GradleProject
import net.navatwo.gradle.testkit.junit5.GradleTestKitConfiguration
import net.navatwo.gradle.testkit.junit5.GradleTestKitConfiguration.BuildDirectoryMode.PRISTINE
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.TimeUnit

@GradleTestKitConfiguration(
    projectsRoot = "src/test/projects/release",
    buildDirectoryMode = PRISTINE,
)
internal class ShowServiceChangePluginTest {
  @Test
  @GradleProject("basic-service-hierarchy")
  fun `execute with no changes does not find changes`(
      @GradleProject.Runner runner: GradleRunner,
      @GradleProject.Root root: File,
      @TempDir outputDirectory: File,
  ) {
    gitInit(root)

    // Add an empty commit so the shas can diff against the same value
    gitEmptyCommit(root)

    val result = runner
        .withArguments(
            SHOW_BUILD_TARGETS_TASK,
            "--outputDirectory",
            outputDirectory.toString(),
            "--stacktrace",
        )
        .build()

    assertThat(result).task(":service-project:$COMPUTE_RUNTIME_CLASSPATH_DEPENDENT_PROJECTS_TASK").isSuccess()
    assertThat(result).task(":service-project:$SHOW_BUILD_TARGETS_TASK").isSuccess()
    assertThat(result).task(":service-project-2:$COMPUTE_RUNTIME_CLASSPATH_DEPENDENT_PROJECTS_TASK").isSuccess()
    assertThat(result).task(":service-project-2:$SHOW_BUILD_TARGETS_TASK").isSuccess()

    assertProjectStatuses(outputDirectory, project1 = false, project2 = false)

    val secondResult = runner.build()
    assertThat(secondResult).task(":service-project:$COMPUTE_RUNTIME_CLASSPATH_DEPENDENT_PROJECTS_TASK").isUpToDate()
    assertThat(secondResult).task(":service-project:$SHOW_BUILD_TARGETS_TASK").isUpToDate()
    assertThat(secondResult).task(":service-project-2:$COMPUTE_RUNTIME_CLASSPATH_DEPENDENT_PROJECTS_TASK").isUpToDate()
    assertThat(secondResult).task(":service-project-2:$SHOW_BUILD_TARGETS_TASK").isUpToDate()

    assertProjectStatuses(outputDirectory, project1 = false, project2 = false)
  }

  @Test
  @GradleProject("basic-service-hierarchy")
  fun `execute with changes to service project finds changes`(
      @GradleProject.Runner runner: GradleRunner,
      @GradleProject.Root root: File,
      @TempDir outputDirectory: File,
  ) {
    gitInit(root)

    val result = runner
        .withArguments(
            SHOW_BUILD_TARGETS_TASK,
            "--stacktrace",
            "--outputDirectory",
            outputDirectory.toString(),
        )
        .build()

    assertThat(result).task(":$COMPUTE_SOURCE_FOLDERS_TASK").isSuccess()
    assertThat(result).task(":service-project:$COMPUTE_RUNTIME_CLASSPATH_DEPENDENT_PROJECTS_TASK").isSuccess()
    assertThat(result).task(":service-project:$SHOW_BUILD_TARGETS_TASK").isSuccess()
    assertThat(result).task(":service-project-2:$COMPUTE_RUNTIME_CLASSPATH_DEPENDENT_PROJECTS_TASK").isSuccess()
    assertThat(result).task(":service-project-2:$SHOW_BUILD_TARGETS_TASK").isSuccess()

    assertProjectStatuses(outputDirectory, project1 = false, project2 = false)

    updateAndCommitNewFileToProject(root, "service-project")

    val secondResult = runner.build()
    assertThat(result).task(":$COMPUTE_SOURCE_FOLDERS_TASK").isSuccess()
    assertThat(secondResult).task(":service-project:$COMPUTE_RUNTIME_CLASSPATH_DEPENDENT_PROJECTS_TASK").isUpToDate()
    assertThat(secondResult).task(":service-project:$SHOW_BUILD_TARGETS_TASK").isSuccess()
    assertThat(secondResult).task(":service-project-2:$COMPUTE_RUNTIME_CLASSPATH_DEPENDENT_PROJECTS_TASK").isUpToDate()
    assertThat(secondResult).task(":service-project-2:$SHOW_BUILD_TARGETS_TASK").isSuccess()

    assertProjectStatuses(outputDirectory, project1 = true, project2 = false)
  }

  @Test
  @GradleProject("basic-service-hierarchy")
  fun `execute with changes to dependency-project finds changes`(
      @GradleProject.Runner runner: GradleRunner,
      @GradleProject.Root root: File,
      @TempDir outputDirectory: File,
  ) {
    gitInit(root)

    val result = runner
        .withArguments(
            SHOW_BUILD_TARGETS_TASK,
            "--outputDirectory",
            outputDirectory.toString(),
        )
        .build()

    assertThat(result).task(":$COMPUTE_SOURCE_FOLDERS_TASK").isSuccess()
    assertThat(result).task(":service-project:$COMPUTE_RUNTIME_CLASSPATH_DEPENDENT_PROJECTS_TASK").isSuccess()
    assertThat(result).task(":service-project:$SHOW_BUILD_TARGETS_TASK").isSuccess()
    assertThat(result).task(":service-project-2:$COMPUTE_RUNTIME_CLASSPATH_DEPENDENT_PROJECTS_TASK").isSuccess()
    assertThat(result).task(":service-project-2:$SHOW_BUILD_TARGETS_TASK").isSuccess()

    assertProjectStatuses(outputDirectory, project1 = false, project2 = false)

    updateAndCommitNewFileToProject(root, "dependency-project")

    val secondResult = runner.build()
    assertThat(secondResult).task(":$COMPUTE_SOURCE_FOLDERS_TASK").isUpToDate()
    assertThat(secondResult).task(":service-project:$COMPUTE_RUNTIME_CLASSPATH_DEPENDENT_PROJECTS_TASK").isUpToDate()
    assertThat(secondResult).task(":service-project:$SHOW_BUILD_TARGETS_TASK").isSuccess()
    assertThat(secondResult).task(":service-project-2:$COMPUTE_RUNTIME_CLASSPATH_DEPENDENT_PROJECTS_TASK").isUpToDate()
    assertThat(secondResult).task(":service-project-2:$SHOW_BUILD_TARGETS_TASK").isSuccess()

    assertProjectStatuses(outputDirectory, project1 = true, project2 = true)
  }

  @Test
  @GradleProject("basic-service-hierarchy")
  fun `changing project build files re-runs computeSourceFolders`(
      @GradleProject.Runner runner: GradleRunner,
      @GradleProject.Root root: File,
      @TempDir outputDirectory: File,
  ) {
    gitInit(root)

    val result = runner
        .withArguments(
            SHOW_BUILD_TARGETS_TASK,
            "--outputDirectory",
            outputDirectory.toString(),
        )
        .build()

    assertThat(result).task(":$COMPUTE_SOURCE_FOLDERS_TASK").isSuccess()
    assertThat(result).task(":service-project:$COMPUTE_RUNTIME_CLASSPATH_DEPENDENT_PROJECTS_TASK").isSuccess()
    assertThat(result).task(":service-project:$SHOW_BUILD_TARGETS_TASK").isSuccess()
    assertThat(result).task(":service-project-2:$COMPUTE_RUNTIME_CLASSPATH_DEPENDENT_PROJECTS_TASK").isSuccess()
    assertThat(result).task(":service-project-2:$SHOW_BUILD_TARGETS_TASK").isSuccess()

    assertProjectStatuses(outputDirectory, project1 = false, project2 = false)

    root.resolve("build.gradle.kts").appendText("\n// Comment to force re-run\n")

    val secondResult = runner.build()
    assertThat(secondResult).task(":$COMPUTE_SOURCE_FOLDERS_TASK").isSuccess()
    assertThat(secondResult).task(":service-project:$COMPUTE_RUNTIME_CLASSPATH_DEPENDENT_PROJECTS_TASK").isUpToDate()
    assertThat(secondResult).task(":service-project:$SHOW_BUILD_TARGETS_TASK").isUpToDate()
    assertThat(secondResult).task(":service-project-2:$COMPUTE_RUNTIME_CLASSPATH_DEPENDENT_PROJECTS_TASK").isUpToDate()
    assertThat(secondResult).task(":service-project-2:$SHOW_BUILD_TARGETS_TASK").isUpToDate()

    assertProjectStatuses(outputDirectory, project1 = false, project2 = false)
  }

  @Test
  @GradleProject("basic-service-hierarchy")
  fun `if output directory is relative, uses root projectDir as relative root`(
      @GradleProject.Runner runner: GradleRunner,
      @GradleProject.Root root: File,
  ) {
    gitInit(root)

    val outputDirectory = File("build/release")

    val result = runner
        .withArguments(
            SHOW_BUILD_TARGETS_TASK,
            "--outputDirectory",
            outputDirectory.path,
        )
        .build()

    assertThat(result).task(":$COMPUTE_SOURCE_FOLDERS_TASK").isSuccess()
    assertThat(result).task(":service-project:$COMPUTE_RUNTIME_CLASSPATH_DEPENDENT_PROJECTS_TASK").isSuccess()
    assertThat(result).task(":service-project:$SHOW_BUILD_TARGETS_TASK").isSuccess()
    assertThat(result).task(":service-project-2:$COMPUTE_RUNTIME_CLASSPATH_DEPENDENT_PROJECTS_TASK").isSuccess()
    assertThat(result).task(":service-project-2:$SHOW_BUILD_TARGETS_TASK").isSuccess()

    assertProjectStatuses(root.resolve(outputDirectory), project1 = false, project2 = false)
  }

  @Test
  @GradleProject("basic-service-hierarchy")
  fun `emptying a folder does not throw`(
      @GradleProject.Runner runner: GradleRunner,
      @GradleProject.Root root: File,
      @TempDir outputDirectory: File,
  ) {
    gitInit(root)

    runner
        .withArguments(
            SHOW_BUILD_TARGETS_TASK,
            "--outputDirectory",
            outputDirectory.toString(),
        )
        .build()

    val initialResourcesHash = getCommitHash(root)

    updateAndCommitNewResourcesToProject(root, "dependency-project")
    removeAndCommitResourcesToProject(root, "dependency-project")

    val deletedResourcesHash = getCommitHash(root)

    // verify does not throw
    runner
        .withArguments(
            SHOW_BUILD_TARGETS_TASK,
            "--outputDirectory",
            outputDirectory.toString(),
            "--currentCommitRef=$deletedResourcesHash",
            "--previousCommitRef=$initialResourcesHash",
        )
        .build()
  }

  @Test
  @GradleProject("basic-service-hierarchy")
  fun `changing an excluded project file does not affect project files`(
      @GradleProject.Runner runner: GradleRunner,
      @GradleProject.Root root: File,
      @TempDir outputDirectory: File,
  ) {
    gitInit(root)

    // Ignore the `src/resources` folder
    with(root.resolve("build.gradle.kts")) {
      appendText(
          """
          
          showBuildTargets {
            sourceSetPathExcludePatterns.add(".+/src/main/?.*")
          }
        """.trimIndent(),
      )
    }

    gitCommitAll(root, "update root build")

    val initialResourcesHash = getCommitHash(root)

    updateAndCommitNewFolderToProject(root, "main/kotlin", "dependency-project")
    updateAndCommitNewFolderToProject(root, "main/kotlin", "dependency-project-2")

    val addedResourcesHash = getCommitHash(root)

    val result = runner
        .withArguments(
            SHOW_BUILD_TARGETS_TASK,
            "--outputDirectory",
            outputDirectory.toString(),
            "--currentCommitRef=$addedResourcesHash",
            "--previousCommitRef=$initialResourcesHash",
        )
        .build()

    assertThat(result).task(":$COMPUTE_SOURCE_FOLDERS_TASK").isSuccess()
    assertThat(result).task(":service-project:$COMPUTE_RUNTIME_CLASSPATH_DEPENDENT_PROJECTS_TASK").isSuccess()
    assertThat(result).task(":service-project:$SHOW_BUILD_TARGETS_TASK").isSuccess()
    assertThat(result).task(":service-project-2:$COMPUTE_RUNTIME_CLASSPATH_DEPENDENT_PROJECTS_TASK").isSuccess()
    assertThat(result).task(":service-project-2:$SHOW_BUILD_TARGETS_TASK").isSuccess()

    assertProjectStatuses(root.resolve(outputDirectory), project1 = false, project2 = false)
  }

  private fun assertProjectStatuses(outputDirectory: File, project1: Boolean, project2: Boolean) {
    assertThat(outputDirectory)
        .isDirectoryContaining("glob:**.status")
    assertThat(outputDirectory.resolve("service-project.status")).hasContent(project1.toString())
    assertThat(outputDirectory.resolve("service-project-2.status")).hasContent(project2.toString())
  }

  private fun gitInit(root: File) {
    git(root, "init")
    git(root, "config", "user.email", "test@example.com")
    git(root, "config", "user.name", "test")
    gitCommitAll(root, "Initial commit")
    gitEmptyCommit(root)
  }

  private fun gitCommitAll(root: File, message: String) {
    git(root, "add", "-A", ".")
    git(root, "commit", "-am", message)
  }

  private fun gitEmptyCommit(root: File) {
    git(root, "commit", "--allow-empty", "-m", "Empty")
  }

  private fun updateAndCommitNewFileToProject(root: File, project: String) {
    with(root.resolve("$project/src/main/kotlin/NewFile.kt")) {
      parentFile.mkdirs()
      writeText("class NewFile{}")
    }

    gitCommitAll(root, "'change project: $project'")
  }

  private fun updateAndCommitNewResourcesToProject(root: File, project: String) {
    with(root.resolve("$project/src/main/resources/test.txt")) {
      parentFile.mkdirs()
      writeText("Test file content")
    }

    gitCommitAll(root, "'change add resource: $project'")
  }

  private fun removeAndCommitResourcesToProject(root: File, project: String) {
    root.resolve("$project/src/main/resources").deleteRecursively()

    gitCommitAll(root, "'delete resources: $project'")
  }

  private fun updateAndCommitNewFolderToProject(root: File, folderName: String, project: String) {
    with(root.resolve("$project/src/$folderName/test.txt")) {
      parentFile.mkdirs()
      writeText("Test file content")
    }

    gitCommitAll(root, "'change add resource: $project'")
  }

  private fun getCommitHash(root: File): String = git(root, "show-ref", "-s")

  private fun git(root: File, vararg command: String): String {
    val process = ProcessBuilder()
        .directory(root)
        .command("git", *command)
        .start()

    assertThat(process.waitFor(5, TimeUnit.SECONDS))
        .`as` { "Failed to execute: `git ${command.joinToString(" ")}` in $root" }
        .isTrue()

    val output = process.inputReader().use { it.readText().trim() }

    assertThat(process.exitValue())
        .`as` { "Failed to execute: `git ${command.joinToString(" ")}` in $root:\n$output" }
        .isZero()

    return output
  }
}
