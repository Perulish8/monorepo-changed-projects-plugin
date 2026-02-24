package io.github.doughawley.monorepobuild

import io.github.doughawley.monorepobuild.git.GitCommandExecutor
import org.gradle.api.logging.Logger
import java.io.File

/**
 * Resolves a base branch name to a concrete git ref that exists in the repository.
 *
 * Preference order:
 *  1. If the caller already supplied a remote ref (e.g. "origin/main"), use it directly.
 *  2. Try the remote tracking ref "origin/<baseBranch>".
 *  3. Fall back to the local branch <baseBranch>.
 *
 * Returns null if none of the candidates exist, allowing the caller to degrade gracefully.
 */
class BaseBranchResolver(
    private val logger: Logger,
    private val gitExecutor: GitCommandExecutor
) {

    fun resolve(gitDir: File, baseBranch: String): String? {
        if (baseBranch.startsWith("origin/")) {
            return if (refExists(gitDir, baseBranch)) baseBranch else null
        }
        val remoteRef = "origin/$baseBranch"
        if (refExists(gitDir, remoteRef)) return remoteRef
        if (refExists(gitDir, baseBranch)) return baseBranch
        return null
    }

    private fun refExists(gitDir: File, ref: String): Boolean {
        return gitExecutor.execute(gitDir, "rev-parse", "--verify", ref).success
    }
}
