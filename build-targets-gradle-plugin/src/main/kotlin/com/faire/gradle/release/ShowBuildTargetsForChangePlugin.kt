// This rule keeps removing `import org.gradle.kotlin.dsl.assign` even though it is used.
@file:Suppress("NoUnusedImports")

package com.faire.gradle.release

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.jetbrains.annotations.VisibleForTesting

/**
 * Small plugin for adding [ShowBuildTargetsForChangeStatusTask] to a project. This is applied as part of Faire's
 * services.
 */
class ShowBuildTargetsForChangePlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val rootProject = project.rootProject
    val computeSourceFolders = if (project == rootProject) {
      rootProject.tasks.register<ComputeSourceFoldersTask>(COMPUTE_SOURCE_FOLDERS_TASK)
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
          configurationName = "runtimeClasspath"
        }

        // For CLI usage
        project.tasks.register<ComputeDependentProjectsTask>("computeDependentProjects")

        project.tasks.register<ShowBuildTargetsForChangeStatusTask>(SHOW_BUILD_TARGETS_TASK) {
          sourceFilesJsonFile = computeSourceFolders.flatMap { it.jsonFile }
          projectDependencyPathsFile = computeRuntimeClasspathDependentProjects.flatMap { it.dependentProjectsListFile }
        }
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
