package com.faire.gradle.release

import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.ResolvedVariantResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult

/**
 * Provides tools for walking the configuration graph.
 *
 * **Source:** https://docs.gradle.org/current/userguide/dependency_graph_resolution.html
 */
internal object DependencyGraph {
  fun traverseGraph(
      rootComponent: ResolvedComponentResult,
      rootVariant: ResolvedVariantResult,
      nodeCallback: (ResolvedVariantResult) -> Unit = { _ -> },
      edgeCallback: (ResolvedVariantResult, ResolvedVariantResult) -> Unit = { _, _ -> },
  ) {
    val seen = mutableSetOf(rootVariant)
    nodeCallback(rootVariant)

    val queue = ArrayDeque(listOf(rootVariant to rootComponent))
    while (queue.isNotEmpty()) {
      val (variant, component) = queue.removeFirst()

      // Traverse this variant's dependencies
      component.getDependenciesForVariant(variant).forEach { dependency ->
        val resolved = when (dependency) {
          is ResolvedDependencyResult -> dependency
          is UnresolvedDependencyResult -> throw dependency.failure
          else -> error("Unknown dependency type: $dependency")
        }

        if (!resolved.isConstraint) {
          val toVariant = resolved.resolvedVariant

          if (seen.add(toVariant)) {
            nodeCallback(toVariant)
            queue.addLast(toVariant to resolved.selected)
          }

          edgeCallback(variant, toVariant)
        }
      }
    }
  }
}
