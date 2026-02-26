package io.github.doughawley.monorepobuild

import io.github.doughawley.monorepobuild.domain.MonorepoProjects
import org.gradle.api.Project

/**
 * Formats the changed-projects report as a string.
 *
 * Responsible solely for formatting; callers supply the header and the domain
 * object, and decide how to output the result.
 */
class ChangedProjectsPrinter(private val rootProject: Project) {

    /**
     * Builds the changed-projects report string.
     *
     * @param header The first line of the report (e.g. "Changed projects:" or "Changed projects (since abc123):")
     * @param monorepoProjects All monorepo projects with their metadata and change information
     * @return The formatted report string ready to be passed to a logger
     */
    fun buildReport(
        header: String,
        monorepoProjects: MonorepoProjects
    ): String {
        val changedProjectPaths = monorepoProjects.getChangedProjectPaths().toSet()
        if (changedProjectPaths.isEmpty()) {
            return "No projects have changed."
        }

        val directlyChangedPaths = monorepoProjects.getProjectsWithDirectChanges()
            .map { it.fullyQualifiedName }
            .toSet()

        val directlyChanged = directlyChangedPaths.sorted()
        val transitivelyAffected = changedProjectPaths
            .filter { it !in directlyChangedPaths }
            .sorted()

        val sb = StringBuilder()
        sb.appendLine(header)

        directlyChanged.forEach { projectPath ->
            val project = monorepoProjects.projects.find { it.fullyQualifiedName == projectPath }
            val files = buildDisplayFiles(projectPath, project?.changedFiles.orEmpty())
            sb.appendLine()
            sb.appendLine("  $projectPath")
            files.take(FILE_DISPLAY_LIMIT).forEach { sb.appendLine("    - $it") }
            if (files.size > FILE_DISPLAY_LIMIT) {
                sb.appendLine("    ... and ${files.size - FILE_DISPLAY_LIMIT} more")
            }
        }

        if (transitivelyAffected.isNotEmpty()) {
            sb.appendLine()
            val maxPathLen = transitivelyAffected.maxOf { it.length }
            transitivelyAffected.forEach { projectPath ->
                val via = monorepoProjects.projects.find { it.fullyQualifiedName == projectPath }
                    ?.dependencies
                    ?.filter { it.hasChanges() }
                    ?.map { it.fullyQualifiedName }
                    ?.sorted()
                    ?.joinToString(", ")
                    .orEmpty()
                val annotation = if (via.isNotEmpty()) "  (affected via $via)" else ""
                sb.appendLine("  ${projectPath.padEnd(maxPathLen)}$annotation")
            }
        }

        return sb.toString().trimEnd()
    }

    private fun buildDisplayFiles(projectPath: String, files: List<String>): List<String> {
        val projectDir = rootProject.findProject(projectPath)
            ?.projectDir
            ?.relativeTo(rootProject.rootDir)
            ?.path
            ?.replace('\\', '/')
            .orEmpty()
        return files.map { file ->
            if (projectDir.isNotEmpty() && file.startsWith("$projectDir/")) {
                file.removePrefix("$projectDir/")
            } else {
                file
            }
        }.sorted()
    }

    companion object {
        const val FILE_DISPLAY_LIMIT = 50
    }
}
