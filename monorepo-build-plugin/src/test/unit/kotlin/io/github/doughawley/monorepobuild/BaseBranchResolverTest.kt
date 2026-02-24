package io.github.doughawley.monorepobuild

import io.github.doughawley.monorepobuild.git.GitCommandExecutor
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.nio.file.Files

class BaseBranchResolverTest : FunSpec({

    fun makeLogger() = ProjectBuilder.builder().build().logger
    fun makeResolver(logger: org.gradle.api.logging.Logger) =
        BaseBranchResolver(logger, GitCommandExecutor(logger))

    fun initRepo(dir: File) {
        executeGitCommand(dir, "init")
        executeGitCommand(dir, "config", "user.email", "test@example.com")
        executeGitCommand(dir, "config", "user.name", "Test User")
        File(dir, "initial.txt").writeText("initial")
        executeGitCommand(dir, "add", "initial.txt")
        executeGitCommand(dir, "commit", "-m", "initial commit")
    }

    test("returns baseBranch directly when it already starts with origin/ and the ref exists") {
        // given
        val tempDir = Files.createTempDirectory("test-already-origin").toFile()
        try {
            initRepo(tempDir)
            // Create a remote tracking ref manually so rev-parse --verify origin/main succeeds
            executeGitCommand(tempDir, "update-ref", "refs/remotes/origin/main", "HEAD")
            val resolver = makeResolver(makeLogger())

            // when
            val result = resolver.resolve(tempDir, "origin/main")

            // then
            result shouldBe "origin/main"
        } finally {
            tempDir.deleteRecursively()
        }
    }

    test("returns null when baseBranch starts with origin/ but the ref does not exist") {
        // given
        val tempDir = Files.createTempDirectory("test-origin-missing").toFile()
        try {
            initRepo(tempDir)
            val resolver = makeResolver(makeLogger())

            // when
            val result = resolver.resolve(tempDir, "origin/nonexistent-branch")

            // then
            result shouldBe null
        } finally {
            tempDir.deleteRecursively()
        }
    }

    test("returns remote tracking ref when origin/<baseBranch> exists") {
        // given
        val tempDir = Files.createTempDirectory("test-remote-tracking").toFile()
        try {
            initRepo(tempDir)
            executeGitCommand(tempDir, "update-ref", "refs/remotes/origin/develop", "HEAD")
            val resolver = makeResolver(makeLogger())

            // when
            val result = resolver.resolve(tempDir, "develop")

            // then
            result shouldBe "origin/develop"
        } finally {
            tempDir.deleteRecursively()
        }
    }

    test("falls back to local branch when remote tracking ref does not exist") {
        // given
        val tempDir = Files.createTempDirectory("test-local-fallback").toFile()
        try {
            initRepo(tempDir)
            executeGitCommand(tempDir, "branch", "feature-x")
            val resolver = makeResolver(makeLogger())

            // when — no origin/feature-x, but local feature-x exists
            val result = resolver.resolve(tempDir, "feature-x")

            // then
            result shouldBe "feature-x"
        } finally {
            tempDir.deleteRecursively()
        }
    }

    test("returns null when neither remote tracking ref nor local branch exists") {
        // given
        val tempDir = Files.createTempDirectory("test-no-ref").toFile()
        try {
            initRepo(tempDir)
            val resolver = makeResolver(makeLogger())

            // when
            val result = resolver.resolve(tempDir, "ghost-branch")

            // then
            result shouldBe null
        } finally {
            tempDir.deleteRecursively()
        }
    }

    test("prefers remote tracking ref over local branch when both exist") {
        // given
        val tempDir = Files.createTempDirectory("test-remote-preferred").toFile()
        try {
            initRepo(tempDir)
            executeGitCommand(tempDir, "branch", "shared")
            executeGitCommand(tempDir, "update-ref", "refs/remotes/origin/shared", "HEAD")
            val resolver = makeResolver(makeLogger())

            // when
            val result = resolver.resolve(tempDir, "shared")

            // then — remote tracking ref takes priority
            result shouldBe "origin/shared"
        } finally {
            tempDir.deleteRecursively()
        }
    }
})

private fun executeGitCommand(directory: File, vararg command: String) {
    val fullCommand = arrayOf("git") + command
    val process = ProcessBuilder(*fullCommand)
        .directory(directory)
        .redirectErrorStream(true)
        .start()
    process.waitFor()
    if (process.exitValue() != 0) {
        val error = process.inputStream.bufferedReader().readText()
        throw RuntimeException("Git command failed: ${command.joinToString(" ")}\n$error")
    }
}
