# build-targets

[![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/com.faire.build-targets)](https://plugins.gradle.org/plugin/com.faire.build-targets)
[![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/Faire/build-targets-gradle-plugin/ci.yml)](https://github.com/Faire/build-targets-gradle-plugin/actions/workflows/ci.yml)

Generate build "targets" based on Git changes between different git refs. This plugin generates a mapping of dependent projects. After this is generated 
and cached, the code uses the project mapping to generate per-file mapping that can correlate against diffs.

Currently, this plugin works against `application` projects. 


## Usage

In the root `build.gradle.kts` file, apply the plugin:

```kotlin
plugins {
  id("com.faire.build-targets") version "1.0.+"
}
```

For all projects with `application` applied, each has a task `showServiceChangeStatus` created. 

By running:

```shell
./gradlew showServiceChangeStatus \
    # Location where `${gradle_project_name}.status` files are created \
    --outputDirectory=build/application_statuses \
    # Commit ref to diff against, e.g. branch name, hash \
    --currentCommitRef='deadbeef' \
    # Optional: Defaults to '${currentCommitSha}~1` \
    # --previousCommitRef='deadbeef~4'
```

A directory will be created at `outputDirectory` with a file for each project. The file contains a simple `true` or 
`false` if the project has changed between the specified references.

## Releasing

1. Create and land a PR bumping the version in `build.gradle.kts`, e.g. https://github.com/Faire/build-targets-gradle-plugin/pull/74
2. Create a [new release](https://github.com/Faire/build-targets-gradle-plugin/releases)
3. Verify published in [Gradle portal](https://plugins.gradle.org/plugin/com.faire.build-targets) 
