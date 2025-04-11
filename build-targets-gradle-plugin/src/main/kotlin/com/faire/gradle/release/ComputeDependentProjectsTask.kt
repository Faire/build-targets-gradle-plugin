package com.faire.gradle.release

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedVariantResult
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.property
import java.util.TreeSet
import javax.inject.Inject

/**
 * Computes the project dependencies for a given project.
 *
 * @see com.faire.gradle.release.ShowServiceChangePluginTest for usage examples.
 */
@CacheableTask
internal abstract class ComputeDependentProjectsTask @Inject constructor(
    objects: ObjectFactory,
) : DefaultTask() {
  @get:Input
  val rootComponent: Property<ResolvedComponentResult> = objects.property()

  @get:Input
  val rootVariant: Property<ResolvedVariantResult> = objects.property()

  @Option(option = "dependentProjectsOutputFile", description = "Directory to write the output file")
  @OutputFile
  val dependentProjectsListFile: RegularFileProperty = objects.fileProperty()
      .convention(project.layout.buildDirectory.file("release/dependentProjects.list"))

  @TaskAction
  fun execute() {
    val dependentProjectPaths = TreeSet<String>()
    DependencyGraph.traverseGraph(
        rootComponent = rootComponent.get(),
        rootVariant = rootVariant.get(),
        nodeCallback = { node ->
          val projectPath = getProjectPath(node)
          if (projectPath != null) {
            dependentProjectPaths.add(projectPath)
          }
        },
    )

    dependentProjectsListFile.asFile.get().apply {
      parentFile.mkdirs()
      writeText(dependentProjectPaths.joinToString("\n"))
    }
  }

  private fun getProjectPath(variant: ResolvedVariantResult): String? {
    val owner = variant.owner
    return if (owner is ProjectComponentIdentifier) {
      owner.projectPath
    } else {
      null
    }
  }
}

