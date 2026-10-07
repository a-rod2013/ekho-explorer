package com.fsql.gradle

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

/**
 * Applies the `compileFsql` task. In an Android project, the task's output is added to every
 * variant's assets, so an invalid stored procedure fails the normal build.
 */
class FsqlPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("fsql", FsqlExtension::class.java)
        extension.sourceDir.convention(project.layout.projectDirectory.dir("src/main/fsql"))

        val compileFsql = project.tasks.register("compileFsql", CompileFsqlTask::class.java) { task ->
            task.group = "fsql"
            task.description = "Compiles .fsql stored procedures and fails the build on any error."
            task.fsqlFiles.from(
                project.fileTree(extension.sourceDir) { tree -> tree.include("**/*.fsql") }
            )
            task.assetsOutputDir.set(project.layout.buildDirectory.dir("generated/fsql/assets"))
        }

        val wire = { wireIntoAndroid(project, compileFsql) }
        project.pluginManager.withPlugin("com.android.application") { wire() }
        project.pluginManager.withPlugin("com.android.library") { wire() }
    }

    private fun wireIntoAndroid(project: Project, compileFsql: TaskProvider<CompileFsqlTask>) {
        val components = project.extensions.getByType(AndroidComponentsExtension::class.java)
        components.onVariants { variant ->
            variant.sources.assets?.addGeneratedSourceDirectory(compileFsql, CompileFsqlTask::assetsOutputDir)
        }
    }
}
