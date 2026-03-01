package io.github.doughawley.monoreporelease.git

import io.github.doughawley.monorepocore.git.GitCommandExecutor
import io.github.doughawley.monoreporelease.domain.SemanticVersion
import io.github.doughawley.monoreporelease.domain.TagPattern
import java.io.File

/**
 * Scans git tags to find version information for a project.
 *
 * Version scanning uses the remote (authoritative released versions).
 * Tag existence checks use local tags (fast pre-flight before attempting tag creation).
 */
class GitTagScanner(
    private val rootDir: File,
    private val executor: GitCommandExecutor
) {

    /**
     * Returns the highest released version for the given project by querying the remote,
     * or null if no tags matching the project's prefix exist on the remote.
     */
    fun findLatestVersion(globalPrefix: String, projectPrefix: String): SemanticVersion? {
        val refPattern = "refs/tags/$globalPrefix/$projectPrefix/v*"
        val lines = executor.executeForOutput(rootDir, "ls-remote", "--tags", "--refs", "origin", refPattern)
        return lines
            .mapNotNull { parseTagFromLsRemoteLine(it, globalPrefix, projectPrefix) }
            .maxOrNull()
    }

    /**
     * Returns the highest patch version within the given major.minor line by querying the remote,
     * or null if no tags for that version line exist on the remote.
     */
    fun findLatestVersionInLine(
        globalPrefix: String,
        projectPrefix: String,
        major: Int,
        minor: Int
    ): SemanticVersion? {
        val refPattern = "refs/tags/$globalPrefix/$projectPrefix/v$major.$minor.*"
        val lines = executor.executeForOutput(rootDir, "ls-remote", "--tags", "--refs", "origin", refPattern)
        return lines
            .mapNotNull { parseTagFromLsRemoteLine(it, globalPrefix, projectPrefix) }
            .maxOrNull()
    }

    /**
     * Returns true if the given tag exists in the local repository.
     * Uses local tag lookup only — a tag that exists on the remote but has not been fetched
     * locally will return false.
     */
    fun tagExists(tag: String): Boolean {
        val output = executor.executeForOutput(rootDir, "tag", "-l", tag)
        return output.isNotEmpty()
    }

    private fun parseTagFromLsRemoteLine(line: String, globalPrefix: String, projectPrefix: String): SemanticVersion? {
        // ls-remote output format: "<sha>\trefs/tags/<tagname>"
        val tagName = line.substringAfter("refs/tags/").trim()
        return TagPattern.parseVersionFromTag(tagName, globalPrefix, projectPrefix)
    }
}
