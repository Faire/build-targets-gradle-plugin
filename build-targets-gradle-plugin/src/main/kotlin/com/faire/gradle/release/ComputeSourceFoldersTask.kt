package com.faire.gradle.release

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.setProperty
import org.gradle.kotlin.dsl.the
import java.io.File
import java.util.stream.Collectors
import javax.inject.Inject

/**
 * Computes the source files for each project in the root.
 */
@CacheableTask
internal abstract class ComputeSourceFoldersTask @Inject constructor(
    objects: ObjectFactory,
) : DefaultTask() {

  @Suppress("UNUSED") // This is an input to the task, acting as a proxy for figuring out source files.
  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  val buildFiles: ConfigurableFileCollection = objects.fileCollection()
      .from(
          project.rootProject.layout.projectDirectory.asFileTree.filter {
            it.name == "build.gradle.kts" &&
                "/test/" !in it.path // exclude test projects
          },
      )

  /**
   * Patterns to use to exclude source files from consideration.
   */
  @Input
  internal val sourceSetPathExcludePatterns: SetProperty<Regex> = objects.setProperty<Regex>()
      .convention(
          project.rootProject.the<ShowBuildTargetsForChangeExtension>().sourceSetPathExcludePatterns.map { patterns ->
            patterns.map { Regex(it) }.toSet()
          },
      )

  @OutputFile
  val jsonFile: RegularFileProperty = objects.fileProperty()
      .convention(project.layout.buildDirectory.file("faire-release/sourceFolders.json"))

  @Internal
  val rootProjectDirectory: DirectoryProperty = objects.directoryProperty()
      .convention(project.rootProject.layout.projectDirectory)

  // Done this way to avoid using this in the implementation, making it configuration-cache safe.
  // If this is too slow in the future, we can consider making this task run _per project_ and do a map-reduce
  // style to collect the results of this task per project within the `ShowServiceChangeStatusTask`. This would
  // parallelize this, letting it run "faster" in the future.
  @Input
  val projectToSourceDirectories: MapProperty<String, Set<File>> = objects.mapProperty<String, Set<File>>()
      .value(
          project.provider {
            project.rootProject.subprojects.parallelStream()
                .collect(Collectors.toMap({ it.path }, ::computeWatchedFiles))
          },
      )
      .apply {
        finalizeValueOnRead()
      }

  @TaskAction
  fun execute() {
    val outputFile = jsonFile.asFile.get().apply {
      parentFile.mkdirs()
    }

    val gson = GsonUtils.gson
    val rootDirectory = rootProjectDirectory.asFile.get()

    val json = gson.toJson(
        projectToSourceDirectories.get().mapValues { (_, files) ->
          files.map { it.relativeTo(rootDirectory).path }
        },
    )

    outputFile.writeText(json)
  }

  private fun computeWatchedFiles(project: Project): Set<File> {
    // Collect all the directories/files that we know are part of each project's compilation. We use
    // directories for source files to reduce the number of entries in this set.
    val buildDir = project.layout.buildDirectory.get().asFile.toPath()
    val sourceDirectories = project.the<JavaPluginExtension>().sourceSets
        .asSequence()
        .flatMap { it.allSource.sourceDirectories.files }
        .filter { f -> !f.toPath().startsWith(buildDir) }
        .filter { f -> !sourceSetPathExcludePatterns.get().any { f.path.matches(it) } }

    val buildKts = project.layout.projectDirectory.file("build.gradle.kts").asFile

    return (sourceDirectories + buildKts).toSet()
  }
}
