package io.github.doughawley.monoreporelease

import io.github.doughawley.monorepocore.git.GitCommandExecutor
import io.github.doughawley.monoreporelease.git.GitReleaseExecutor
import io.github.doughawley.monoreporelease.git.GitTagScanner
import io.github.doughawley.monoreporelease.task.ReleaseTask
import io.github.doughawley.monorepobuild.MonorepoBuildExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class MonorepoReleasePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // Auto-apply the build plugin so allAffectedProjects is always populated.
        // Coupling is strictly one-directional: release → build, never build → release.
        project.pluginManager.apply("io.github.doug-hawley.monorepo-build-plugin")

        // Register releaseChangedProjects as a ref-mode task in the build plugin's extension.
        // This must happen before projectsEvaluated fires (where resolveMode() reads it).
        val buildExt = project.extensions.getByType(MonorepoBuildExtension::class.java)
        buildExt.additionalRefModeTasks = setOf("releaseChangedProjects")

        val rootExtension = project.extensions.create("monorepoRelease", MonorepoReleaseExtension::class.java)

        // Register per-project opt-in extension and release tasks eagerly so that build scripts
        // can configure tasks like postRelease during their own configuration phase.
        project.subprojects.forEach { sub ->
            val config = sub.extensions.create("monorepoReleaseConfig", MonorepoReleaseConfigExtension::class.java)
            registerReleaseTasks(sub, rootExtension, config)
        }

        // Root-level aggregator task.
        val releaseChangedProjectsTask = project.tasks.register("releaseChangedProjects") {
            group = "monorepo-release"
            description = "Releases all opted-in projects that have changed since the configured commit ref"
            dependsOn(project.tasks.named("buildChangedProjectsFromRef"))
            doLast {
                val ext = project.rootProject.extensions
                    .getByType(MonorepoBuildExtension::class.java)
                val changed = ext.allAffectedProjects
                if (changed.isEmpty()) {
                    logger.lifecycle("No projects have changed — nothing to release.")
                } else {
                    logger.lifecycle("Released changed projects: ${changed.joinToString(", ")}")
                }
            }
        }

        // Wire release tasks for opted-in changed projects after all projects are evaluated.
        // Build plugin's projectsEvaluated fires first (registered first), populating allAffectedProjects.
        // This callback fires second and reads allAffectedProjects to wire release dependencies.
        project.gradle.projectsEvaluated {
            val ext = project.rootProject.extensions
                .getByType(MonorepoBuildExtension::class.java)
            val buildChangedTask = project.tasks.named("buildChangedProjectsFromRef")

            ext.allAffectedProjects.forEach { projectPath ->
                val sub = project.rootProject.findProject(projectPath) ?: return@forEach
                val config = sub.extensions
                    .findByType(MonorepoReleaseConfigExtension::class.java) ?: return@forEach
                if (!config.enabled) {
                    return@forEach
                }

                val releaseTask = sub.tasks.findByName("release") ?: return@forEach

                // mustRunAfter ensures release runs after build when both are in the graph,
                // but does not force buildChangedProjectsFromRef to run for standalone :project:release.
                releaseTask.mustRunAfter(buildChangedTask)

                releaseChangedProjectsTask.configure {
                    dependsOn(releaseTask)
                }
            }
        }
    }

    private fun registerReleaseTasks(
        sub: Project,
        rootExtension: MonorepoReleaseExtension,
        config: MonorepoReleaseConfigExtension
    ) {
        val executor = GitCommandExecutor(sub.logger)
        val scanner = GitTagScanner(sub.rootProject.rootDir, executor)
        val releaseExecutor = GitReleaseExecutor(sub.rootProject.rootDir, executor, sub.logger)

        val postRelease = sub.tasks.register("postRelease") {
            group = "monorepo-release"
            description = "Lifecycle hook: wire publish tasks here via finalizedBy"
        }

        val releaseTask = sub.tasks.register("release", ReleaseTask::class.java) {
            group = "monorepo-release"
            description = "Creates a versioned git tag for this project"
            this.rootExtension = rootExtension
            this.projectConfig = config
            this.gitTagScanner = scanner
            this.gitReleaseExecutor = releaseExecutor
            finalizedBy(postRelease)
        }

        postRelease.configure {
            onlyIf {
                val state = releaseTask.get().state
                val failure: Throwable? = state.failure
                state.executed && failure == null
            }
        }
    }
}
