rootProject.name = "test-project"

include(
  ":dependency-project",
  // This emptydir parent directory structure ensures the plugin doesn't have trouble with empty projects.
  ":emptydir:lib-project",
  ":service-project",
  ":service-project-2",
)
