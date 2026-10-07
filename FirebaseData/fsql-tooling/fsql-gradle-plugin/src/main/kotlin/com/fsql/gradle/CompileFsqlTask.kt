package com.fsql.gradle

import com.fsql.compiler.FsqlCompiler
import com.fsql.compiler.SourceFile
import com.fsql.plan.PlanJson
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Compiles every `.fsql` stored procedure. Any error fails the build (all errors are listed at once).
 * On success it writes the plan asset the runtime loads.
 */
@CacheableTask
abstract class CompileFsqlTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val fsqlFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val assetsOutputDir: DirectoryProperty

    @TaskAction
    fun compile() {
        val sources = fsqlFiles.files
            .filter { it.isFile }
            .map { SourceFile(it.absolutePath, it.readText()) }
        val result = FsqlCompiler.compile(sources)

        if (!result.ok) {
            val lines = result.diagnostics.joinToString("\n") { it.format() }
            val count = result.diagnostics.size
            throw GradleException(
                "Stored procedure compilation failed with $count ${if (count == 1) "error" else "errors"}:\n$lines"
            )
        }

        val assetsDir = assetsOutputDir.get().asFile
        assetsDir.deleteRecursively()
        File(assetsDir, "fsql").apply { mkdirs() }
            .resolve("plans.json")
            .writeText(PlanJson.encode(result.plans))

        logger.lifecycle("fsql: compiled ${result.plans.size} stored procedure(s)")
    }
}
