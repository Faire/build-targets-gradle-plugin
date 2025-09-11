// This rule keeps removing `import org.gradle.kotlin.dsl.assign` even though it is used.
@file:Suppress("NoUnusedImports")

package com.faire.gradle.release

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.base.TestingExtension
import org.jetbrains.annotations.VisibleForTesting

/**
 * Small plugin for adding [ShowBuildTargetsForChangeStatusTask] to a project. This is applied as part of Faire's
 * services.
 */
class ShowBuildTargetsForChangePlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val rootProject = project.rootProject
    val computeSourceFolders = if (project == rootProject) {
      rootProject.tasks.register<ComputeSourceFoldersTask>(COMPUTE_SOURCE_FOLDERS_TASK) {}
    } else {
      rootProject.tasks.named<ComputeSourceFoldersTask>(COMPUTE_SOURCE_FOLDERS_TASK)
    }

    if (project != rootProject) {
      // Only apply this plugin if and only if we are not the root project, and the project is an `application`, i.e.
      // we are packaging this a service.
      project.plugins.withId("application") {
        val computeRuntimeClasspathDependentProjects = project.tasks.register<ComputeDependentProjectsTask>(
            COMPUTE_RUNTIME_CLASSPATH_DEPENDENT_PROJECTS_TASK,
        ) {
          val runtimeClasspath = project.configurations.named("runtimeClasspath")

          rootComponent = runtimeClasspath.flatMap {
            it.incoming.resolutionResult.rootComponent
          }

          rootVariant = runtimeClasspath.flatMap {
            it.incoming.resolutionResult.rootVariant
          }
        }

        val showTargetsTask = project.tasks.register<ShowBuildTargetsForChangeStatusTask>(SHOW_BUILD_TARGETS_TASK) {
          sourceFilesJsonFile = computeSourceFolders.flatMap { it.jsonFile }
          projectDependencyPathsFiles.from(
            computeRuntimeClasspathDependentProjects.flatMap { it.dependentProjectsListFile },
          )
        }

        // Add test dependencies if includeTests is enabled
        val extension = rootProject.extensions.getByType(ShowBuildTargetsForChangeExtension::class.java)
        if (extension.includeTests.getOrElse(false)) {
          project.allprojects {
            val testDependencyTask = tasks.withType<ComputeDependentProjectsTask>()
            showTargetsTask.configure {
              projectDependencyPathsFiles.from(testDependencyTask.map { it.dependentProjectsListFile })
            }
          }
        }
      }
    } else {
      val extension = rootProject.extensions.create("showBuildTargets", ShowBuildTargetsForChangeExtension::class)
      rootProject.afterEvaluate {
        if (extension.includeTests.getOrElse(false)) {
          createTestDependencyTasks(rootProject)
        }
      }
    }
  }

  private fun createTestDependencyTasks(applicationProject: Project) {
    applicationProject.allprojects {
      project.plugins.withId("jvm-test-suite") {
        the<TestingExtension>().suites.withType<JvmTestSuite> {
          createTestDependencyTaskForSuite(project, this@withType)
        }
      }
    }
  }

  private fun createTestDependencyTaskForSuite(
    project: Project,
    testSuite: JvmTestSuite,
  ) {
    val testSuiteName = testSuite.name
    val testConfig = project.configurations.findByName(testSuite.sources.runtimeClasspathConfigurationName)
    val taskName = "compute${testSuiteName.replaceFirstChar { it.uppercaseChar() }}DependentProjects"
    if (testConfig != null) {
      project.tasks.register<ComputeDependentProjectsTask>(taskName) {
        rootComponent = project.providers.provider {
          testConfig.incoming.resolutionResult.rootComponent.get()
        }

        rootVariant = project.providers.provider {
          testConfig.incoming.resolutionResult.rootVariant.get()
        }

        dependentProjectsListFile.set(
          project.layout.buildDirectory.file("release/${testSuiteName}DependentProjects.list"),
        )
      }
    }
  }

  companion object {
    @VisibleForTesting
    internal const val COMPUTE_SOURCE_FOLDERS_TASK = "computeSourceFolders"

    @VisibleForTesting
    internal const val SHOW_BUILD_TARGETS_TASK = "showBuildTargetsForChange"

    @VisibleForTesting
    internal const val COMPUTE_RUNTIME_CLASSPATH_DEPENDENT_PROJECTS_TASK = "computeRuntimeClasspathDependentProjects"
  }
}
