package io.github.doughawley.monoreporelease.git

import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import java.io.File
import java.nio.file.Files

class TempGitRepo {
    private val tempDir: File = Files.createTempDirectory("git-integration-test").toFile()
    val remoteDir: File = File(tempDir, "remote.git")
    val localDir: File = File(tempDir, "local")

    fun initialize() {
        localDir.mkdirs()
        runGit(localDir, "init")
        runGit(localDir, "config", "user.email", "test@test.com")
        runGit(localDir, "config", "user.name", "Test User")

        File(localDir, "README.md").writeText("Initial commit")
        runGit(localDir, "add", ".")
        runGit(localDir, "commit", "-m", "Initial commit")

        runGit(tempDir, "init", "--bare", remoteDir.absolutePath)
        runGit(localDir, "remote", "add", "origin", remoteDir.absolutePath)
        runGit(localDir, "push", "-u", "origin", "HEAD")
    }

    fun pushTag(tag: String) {
        runGit(localDir, "tag", tag)
        runGit(localDir, "push", "origin", tag)
    }

    fun createLocalTag(tag: String) {
        runGit(localDir, "tag", tag)
    }

    fun deleteLocalTag(tag: String) {
        runGit(localDir, "tag", "-d", tag)
    }

    fun deleteRecursively() {
        tempDir.deleteRecursively()
    }

    private fun runGit(dir: File, vararg args: String) {
        val cmd = listOf("git") + args.toList()
        val process = ProcessBuilder(cmd)
            .directory(dir)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw RuntimeException(
                "Git command failed (exit $exitCode): ${cmd.joinToString(" ")}\n$output"
            )
        }
    }
}

class TempGitRepoListener : TestListener {
    lateinit var repo: TempGitRepo
        private set

    override suspend fun beforeEach(testCase: TestCase) {
        repo = TempGitRepo()
        repo.initialize()
    }

    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        if (::repo.isInitialized) {
            repo.deleteRecursively()
        }
    }
}
