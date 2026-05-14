package com.faire.gradle.release

import com.google.gson.reflect.TypeToken
import org.gradle.api.DefaultTask
import org.gradle.api.Transformer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
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
import java.nio.file.Path
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

  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  val projectDependencyPathsFiles: ConfigurableFileCollection = objects.fileCollection()

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

    val projectDependencyPaths = projectDependencyPathsFiles.flatMap { it.readLines() }.toSet()

    val pathsToDiff = buildSet {
      addAll(
          projectDependencyPaths.flatMap { projectPath ->
            projectToSourceDirectories.getValue(projectPath)
          },
      )

      add(File(rootBuildFilePath.get()))
      addAll(projectToSourceDirectories.getValue(currentProjectPath.get()))
    }

    // We previously invoked `git log -- <every path>` with `pathsToDiff` as argv, which blew past OS argv limits on
    // large modules. Instead we list every changed file in the range once (no path arguments) and intersect in-process.
    val diffsFound = anyChangedFileMatches(pathsToDiff)

    val outputFile = outputFile.get()
    outputFile.parentFile.mkdirs()
    outputFile.writeText("$diffsFound")
  }

  private fun anyChangedFileMatches(pathsToDiff: Set<File>): Boolean {
    if (pathsToDiff.isEmpty()) {
      return false
    }

    val targets: Set<Path> = pathsToDiff.mapTo(mutableSetOf()) { it.toPath().toAbsolutePath().normalize() }
    val repoRoot = File(rootProjectPath.get()).toPath().toAbsolutePath().normalize()

    return listChangedFiles().any { relativePath ->
      val changed = repoRoot.resolve(relativePath).normalize()
      targets.any { target -> changed.startsWith(target) }
    }
  }

  private fun listChangedFiles(): Sequence<String> {
    val stdout = ByteArrayOutputStream().use { out ->
      execOperations.exec {
        commandLine(
            "git",
            "diff",
            "--name-only",
            "-z",
            "${previousCommitRef.get()}..${currentCommitRef.get()}",
        )
        standardOutput = out
      }.assertNormalExitValue()
      out.toByteArray()
    }
    return String(stdout, Charsets.UTF_8)
        .splitToSequence('\u0000')
        .filter { it.isNotEmpty() }
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
