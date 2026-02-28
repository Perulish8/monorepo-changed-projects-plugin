package io.github.doughawley.monoreporelease.git

import org.gradle.api.logging.Logger
import java.io.File

/**
 * Responsible for executing git commands and handling their output.
 */
class GitCommandExecutor(private val logger: Logger) {

    data class CommandResult(
        val success: Boolean,
        val output: List<String>,
        val exitCode: Int,
        val errorOutput: String = ""
    )

    fun execute(directory: File, vararg command: String): CommandResult {
        val fullCommand = arrayOf("git") + command

        val process = try {
            ProcessBuilder(*fullCommand)
                .directory(directory)
                .redirectErrorStream(true)
                .start()
        } catch (e: Exception) {
            logger.error("Exception starting git command: ${fullCommand.joinToString(" ")}", e)
            return CommandResult(
                success = false,
                output = emptyList(),
                exitCode = -1,
                errorOutput = e.message ?: "Unknown error"
            )
        }

        return try {
            val exitCode = process.waitFor()
            val output = process.inputStream.bufferedReader().use { reader ->
                reader.readLines().filter { it.isNotBlank() }
            }

            if (exitCode == 0) {
                CommandResult(
                    success = true,
                    output = output,
                    exitCode = exitCode
                )
            } else {
                val errorOutput = output.joinToString("\n")
                logger.warn("Git command failed with exit code $exitCode: ${fullCommand.joinToString(" ")}")
                logger.warn("Error output: $errorOutput")

                CommandResult(
                    success = false,
                    output = emptyList(),
                    exitCode = exitCode,
                    errorOutput = errorOutput
                )
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.error("Interrupted waiting for git command: ${fullCommand.joinToString(" ")}", e)
            CommandResult(
                success = false,
                output = emptyList(),
                exitCode = -1,
                errorOutput = e.message ?: "Interrupted"
            )
        } catch (e: Exception) {
            logger.error("Exception executing git command: ${fullCommand.joinToString(" ")}", e)
            CommandResult(
                success = false,
                output = emptyList(),
                exitCode = -1,
                errorOutput = e.message ?: "Unknown error"
            )
        } finally {
            process.destroy()
        }
    }

    fun executeForOutput(directory: File, vararg command: String): List<String> {
        val result = execute(directory, *command)
        return if (result.success) result.output else emptyList()
    }
}
