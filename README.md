# built-targets

Generate build "targets" based on Git changes between different git refs. This plugin generates a mapping of dependent projects. After this is generated 
and cached, the code uses the project mapping to generate per-file mapping that can correlate against diffs.

Currently, this plugin works against `application` projects. 


## Usage

In the root `build.gradle.kts` file, apply the plugin:

```kotlin
plugins {
  id("faire.build-targets") version "1.0.+"
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

TODO
