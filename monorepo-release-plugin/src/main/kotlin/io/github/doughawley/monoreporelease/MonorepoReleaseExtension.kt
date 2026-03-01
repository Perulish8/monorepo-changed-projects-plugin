package io.github.doughawley.monoreporelease

open class MonorepoReleaseExtension {
    var globalTagPrefix: String = "release"
    var releaseBranchPatterns: List<String> = listOf("^main$", "^release/.*")
    var primaryBranchScope: String = "minor"
}
