package com.faire.gradle.release

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.setProperty
import javax.inject.Inject

/**
 * Provides extensions for configuring the plugin.
 */
open class ShowBuildTargetsForChangeExtension @Inject constructor(
    objects: ObjectFactory,
) {
  /**
   * Regex patterns to **exclude** source set folders. If **any** pattern is matched, a source is excluded.
   *
   * Patterns are applied to the full path of the source sets.
   */
  val sourceSetPathExcludePatterns: SetProperty<String> = objects.setProperty()
}
