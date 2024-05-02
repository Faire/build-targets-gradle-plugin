plugins {
  alias(libs.plugins.kotlin) apply false
  alias(libs.plugins.dependency.analysis)
}

dependencyAnalysis {
  issues {
    all {
      onAny {
        severity("fail")
      }
    }
  }
}
