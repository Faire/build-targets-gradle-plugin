package com.faire.gradle.test

import net.navatwo.gradle.testkit.junit5.GradleProject
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.ReflectiveInvocationContext
import java.io.File
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit

/**
 * Configures the test gradle project to use the repo root version catalog (libs.versions.toml) and .java-version
 * NOTE: this extension is autoloaded when included in the classpath due to the resources/META-INF/services entry:
 * https://junit.org/junit5/docs/current/user-guide/#extensions-registration-automatic
 */
internal class UseRootProjectGradleDependencyConfigurationTestExtension : InvocationInterceptor, BeforeAllCallback {
  override fun beforeAll(context: ExtensionContext) {
    val testClass = context.requiredTestClass
    if (!testClass.methods.any { it.isAnnotationPresent(GradleProject::class.java) }) {
      // This class is only useful when there are @GradleProject tests to run
      return
    }

    val process = ProcessBuilder("git", "rev-parse", "--show-toplevel")
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()
        .apply { waitFor(10, TimeUnit.SECONDS) }
    val gitRoot = File(process.inputStream.bufferedReader().readText().trim())

    context.getStore(ExtensionContext.Namespace.GLOBAL).put(REPO_ROOT_ABSOLUTE_PATH, gitRoot)
  }

  @Suppress("ForbiddenVoid") // Java override!
  override fun interceptTestMethod(
      invocation: InvocationInterceptor.Invocation<Void>,
      invocationContext: ReflectiveInvocationContext<Method>,
      context: ExtensionContext,
  ) {
    // since this class is loaded unconditionally, we need to check if the beforeAll() did anything first
    val gitRoot = context.getStore(ExtensionContext.Namespace.GLOBAL)
        .getOrDefault(REPO_ROOT_ABSOLUTE_PATH, File::class.java, null)

    if (gitRoot != null) {
      // to find the temporary test gradle project directory, we need to first figure out which parameter is the
      // GradleRunner
      val runnerParamIndex = context.requiredTestMethod.parameters.indexOfLast {
          it.isAnnotationPresent(GradleProject.Runner::class.java)
      }
      if (runnerParamIndex >= 0) {
        // then the invocationContext contains the actual parameter value
        val testProjectRoot = (invocationContext.arguments[runnerParamIndex] as GradleRunner).projectDir
        copyFilesToTestProject(gitRoot, testProjectRoot)
      }
    }

    invocation.proceed()
  }

  private fun copyFilesToTestProject(gitRoot: File, testProjectRoot: File) {
    for (f in FILES_TO_COPY) { gitRoot.resolve(f).copyTo(testProjectRoot.resolve(f), overwrite = true) }
  }

  companion object {
    private const val REPO_ROOT_ABSOLUTE_PATH = "repoRootAbsolutePath"
    private val FILES_TO_COPY = listOf(
      "gradle/libs.versions.toml",
      ".java-version",
    )
  }
}
