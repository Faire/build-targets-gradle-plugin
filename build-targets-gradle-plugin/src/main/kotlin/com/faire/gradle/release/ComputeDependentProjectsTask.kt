package com.faire.gradle.release

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import org.gradle.kotlin.dsl.withType
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

/**
 * Computes the project dependencies for a given project.
 *
 * @see com.faire.gradle.release.ShowServiceChangePluginTest for usage examples.
 */
@DisableCachingByDefault(because = "This task walks the project configurations requiring access to `project`.")
internal abstract class ComputeDependentProjectsTask @Inject constructor(
    objects: ObjectFactory,
) : DefaultTask() {
  @Input
  val projectDependencyPaths: SetProperty<String> = objects.setProperty<String>()
      .value(project.provider { getAllDependenciesForProjects(setOf(project)).map { it.path } })
      .apply { finalizeValueOnRead() }

  @field:Option(option = "configuration", description = "Name of configuration")
  @Input
  val configurationName = objects.property<String>()

  @Option(option = "dependentProjectsOutputFile", description = "Directory to write the output file")
  @OutputFile
  val dependentProjectsListFile: RegularFileProperty = objects.fileProperty()
      .convention(project.layout.buildDirectory.file("release/dependentProjects.list"))

  @TaskAction
  fun execute() {
    val projectDependencyPaths = projectDependencyPaths.get().sorted()
    dependentProjectsListFile.asFile.get().apply {
      parentFile.mkdirs()
      writeText(projectDependencyPaths.joinToString("\n"))
    }
  }

  /** Gets all the project dependencies for a project, including transitive ones. */
  private fun getAllDependenciesForProjects(
      startingProjects: Set<Project>,
      visitedProjects: Set<Project> = setOf(),
  ): Set<Project> {
    if (startingProjects.isEmpty()) {
      return setOf()
    }

    val runtimeProjectDependencies = startingProjects.asSequence()
        .flatMap { project ->
          val runtimeClasspath = project.configurations.named(configurationName.get()).get()
          runtimeClasspath.allDependencies.withType<ProjectDependency>()
              .map { it.dependencyProject }
        }
        .toSet()

    return runtimeProjectDependencies + getAllDependenciesForProjects(
        startingProjects = runtimeProjectDependencies - visitedProjects,
        visitedProjects = visitedProjects + startingProjects,
    )
  }
}
