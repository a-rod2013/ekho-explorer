package com.fsql.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CompileFsqlFunctionalTest {

    private fun project(vararg files: Pair<String, String>): File {
        val dir = Files.createTempDirectory("fsql-test").toFile()
        File(dir, "settings.gradle").writeText("rootProject.name = 'fsql-test'\n")
        File(dir, "build.gradle").writeText("plugins { id 'com.fsql.compile' }\n")
        for ((path, text) in files) {
            File(dir, "src/main/fsql/$path").apply { parentFile.mkdirs() }.writeText(text)
        }
        return dir
    }

    private fun runner(dir: File, vararg args: String) = GradleRunner.create()
        .withProjectDir(dir)
        .withPluginClasspath()
        .withArguments(*args, "--stacktrace")

    private val good = "SELECT * FROM notes"

    @Test
    fun `compiles good procedures into a plan asset`() {
        val dir = project("Good.fsql" to good)
        val result = runner(dir, "compileFsql").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":compileFsql")!!.outcome)

        val plans = File(dir, "build/generated/fsql/assets/fsql/plans.json")
        assertTrue(plans.exists())
        assertTrue(plans.readText().contains("\"Good\""))
    }

    @Test
    fun `a second run with no changes is up to date and a change reruns it`() {
        val dir = project("Good.fsql" to good)
        runner(dir, "compileFsql").build()
        val again = runner(dir, "compileFsql").build()
        assertEquals(TaskOutcome.UP_TO_DATE, again.task(":compileFsql")!!.outcome)

        File(dir, "src/main/fsql/Other.fsql").writeText(good)
        val changed = runner(dir, "compileFsql").build()
        assertEquals(TaskOutcome.SUCCESS, changed.task(":compileFsql")!!.outcome)
        assertTrue(File(dir, "build/generated/fsql/assets/fsql/plans.json").readText().contains("\"Other\""))
    }

    @Test
    fun `invalid procedures fail the build and every error is listed`() {
        val dir = project(
            "Good.fsql" to good,
            "BadJoin.fsql" to "SELECT * FROM a JOIN b ON a.x = b.x",
            "sub/BadParam.fsql" to "-- @x: WEIRD\nSELECT 1",
        )
        val result = runner(dir, "compileFsql").buildAndFail()
        val output = result.output
        assertEquals(TaskOutcome.FAILED, result.task(":compileFsql")!!.outcome)
        assertTrue(output.contains("BadJoin.fsql"), output)
        assertTrue(output.contains("JOIN is not supported"), output)
        assertTrue(output.contains("BadParam.fsql"), output)
        assertTrue(output.contains("WEIRD"), output)
        assertTrue(output.contains("e: file:///"), output)
        assertFalse(output.contains("Good.fsql:"), output)
    }

    @Test
    fun `no fsql folder at all still succeeds with an empty plan file`() {
        val dir = project()
        val result = runner(dir, "compileFsql").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":compileFsql")!!.outcome)
        assertTrue(File(dir, "build/generated/fsql/assets/fsql/plans.json").exists())
    }
}
