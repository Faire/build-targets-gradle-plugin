package com.faire.gradle.release

import com.google.gson.reflect.TypeToken
import org.gradle.api.DefaultTask
import org.gradle.api.Transformer
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.property
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

/**
 * Computes the changed files based on the git history and the source files that are compiled per project. This is
 * expected to be executed as a cli task, rather than depended on. See [Option] annotated parameters for configuration.
 *
 * @see com.faire.gradle.release.ShowServiceChangePluginTest for usage examples.
 */
@CacheableTask
internal abstract class ShowBuildTargetsForChangeStatusTask @Inject constructor(
    objects: ObjectFactory,
    private val execOperations: ExecOperations,
) : DefaultTask() {

  private val parseRevTransformer: Transformer<Provider<String>, String> = Transformer { param ->
    project.providers.exec {
      commandLine("git", "rev-parse", param)
    }.standardOutput.asText.map { it.trim() }
  }

  @Option(option = "currentCommitRef", description = "Current commit ref to compare against")
  @Internal // internal because we use the computed ref as @Input
  val providedCurrentCommitRef = objects.property<String>()

  @Input
  val currentCommitRef: Provider<String> = objects.property<String>()
      .value(providedCurrentCommitRef.orElse("HEAD"))
      .flatMap(parseRevTransformer)

  @Option(option = "previousCommitRef", description = "Previous commit ref to compare against")
  @Internal // internal because we use the computed ref as @Input
  val providedPreviousCommitRef = objects.property<String>()

  @Input
  val previousCommitRef: Provider<String> = objects.property<String>()
      .value(providedPreviousCommitRef.orElse(currentCommitRef.map { "$it~1" }))
      .flatMap(parseRevTransformer)

  @InputFile
  @PathSensitive(PathSensitivity.RELATIVE)
  val sourceFilesJsonFile: RegularFileProperty = objects.fileProperty()

  @InputFile
  @PathSensitive(PathSensitivity.RELATIVE)
  val projectDependencyPathsFile: RegularFileProperty = objects.fileProperty()

  // Note: This uses `Property<String>` rather than `DirectoryProperty` as it will default resolve to be a directory
  //       relative to `project`.
  @field:Option(option = "outputDirectory", description = "Directory to write the output file")
  @Internal
  val rootOutputDirectory: Property<String> = objects.property()

  @OutputFile
  val outputFile: Provider<File> = rootOutputDirectory.map { providedOutputDirectory ->
    val rootProjectDirectory = project.rootProject.layout.projectDirectory.asFile
    rootProjectDirectory.resolve(providedOutputDirectory)
  }.map { outputDirectory ->
    outputDirectory.resolve("${project.name}.status")
  }

  @Internal
  val currentProjectPath: Property<String> = objects.property<String>()
      .value(project.path)

  @Internal
  val rootBuildFilePath: Property<String> = objects.property<String>()
      .value(project.rootProject.layout.projectDirectory.file("build.gradle.kts").asFile.absolutePath)

  @Internal
  val rootProjectPath: Property<String> = objects.property<String>()
      .value(project.rootProject.layout.projectDirectory.asFile.absolutePath)

  @TaskAction
  fun execute() {
    val projectToSourceDirectories = readSourceFilesPerProjectPath()

    val projectDependencyPaths = projectDependencyPathsFile.asFile.get().readLines().toSet()

    val pathsToDiff = buildSet {
      addAll(
          projectDependencyPaths.flatMap { projectPath ->
            projectToSourceDirectories.getValue(projectPath)
          },
      )

      add(File(rootBuildFilePath.get()))
      addAll(projectToSourceDirectories.getValue(currentProjectPath.get()))
    }

    val diffsFound = ByteArrayOutputStream().use { stdout ->
      // Executes git and shows a single line if and only if `pathsToDiff` files have changed
      execOperations.exec {
        commandLine(
            "git",
            "log",
            "--oneline",
            "${previousCommitRef.get()}..${currentCommitRef.get()}",
        )

        args(pathsToDiff)

        standardOutput = stdout
      }.assertNormalExitValue()

      stdout.toString().isNotBlank()
    }

    val outputFile = outputFile.get()
    outputFile.parentFile.mkdirs()
    outputFile.writeText("$diffsFound")
  }

  private fun readSourceFilesPerProjectPath(): Map<String, Set<File>> {
    val rootDirectory = File(rootProjectPath.get())

    val stringifiedMap = sourceFilesJsonFile.asFile.get().bufferedReader().use {
      GsonUtils.gson.fromJson<Map<String, Set<String>>>(
          it,
          object : TypeToken<Map<String, Set<String>>>() {}.type,
      )
    }
    return stringifiedMap.mapValues { (_, files) -> files.mapTo(mutableSetOf()) { rootDirectory.resolve(it) } }
  }
}
