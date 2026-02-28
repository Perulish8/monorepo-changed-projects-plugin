package io.github.doughawley.monoreporelease.domain

object TagPattern {

    private val RELEASE_BRANCH_REGEX = Regex("""^release/.+/v\d+\.\d+\.x$""")

    fun formatTag(globalPrefix: String, projectPrefix: String, version: SemanticVersion): String {
        return "$globalPrefix/$projectPrefix/v$version"
    }

    fun formatReleaseBranch(projectPrefix: String, version: SemanticVersion): String {
        return "release/$projectPrefix/v${version.major}.${version.minor}.x"
    }

    fun deriveProjectTagPrefix(gradlePath: String): String {
        return gradlePath.trimStart(':').replace(':', '-')
    }

    fun parseVersionFromTag(tag: String, globalPrefix: String, projectPrefix: String): SemanticVersion? {
        val expectedPrefix = "$globalPrefix/$projectPrefix/v"
        if (!tag.startsWith(expectedPrefix)) return null
        val versionStr = tag.removePrefix(expectedPrefix)
        return SemanticVersion.parse(versionStr)
    }

    fun isReleaseBranch(branch: String): Boolean {
        return RELEASE_BRANCH_REGEX.matches(branch)
    }

    fun parseVersionLineFromBranch(branch: String): Pair<Int, Int> {
        // Expected format: release/<prefix>/v<major>.<minor>.x
        val parts = branch.split("/")
        // Last part is like "v1.2.x"
        val versionPart = parts.last()
        val cleaned = versionPart.trimStart('v').removeSuffix(".x")
        val nums = cleaned.split(".")
        val major = nums[0].toInt()
        val minor = nums[1].toInt()
        return Pair(major, minor)
    }
}
