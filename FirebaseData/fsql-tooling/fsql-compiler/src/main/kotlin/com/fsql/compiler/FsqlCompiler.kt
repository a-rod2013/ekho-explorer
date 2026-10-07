package com.fsql.compiler

import com.fsql.plan.Plan
import net.sf.jsqlparser.JSQLParserException
import net.sf.jsqlparser.parser.CCJSqlParserUtil

/** One `.fsql` file: its path (used in messages) and its text. */
data class SourceFile(val path: String, val text: String)

/** A build error in one file. [line] and [col] are 1-based (col is always 1: see HeaderParser/PlanBuilder). */
data class Diagnostic(val file: String, val line: Int, val col: Int, val message: String) {
    /**
     * Formats the error like a Kotlin compiler error, `e: file:///path:line:col message`.
     */
    fun format(): String {
        val path = file.replace('\\', '/')
        val url = if (path.startsWith("/")) "file://$path" else "file:///$path"
        return "e: $url:$line:$col $message"
    }
}

data class CompileResult(val plans: List<Plan>, val diagnostics: List<Diagnostic>) {
    val ok: Boolean get() = diagnostics.isEmpty()
}

/** Compiles a set of `.fsql` files. */
object FsqlCompiler {

    fun compile(files: List<SourceFile>): CompileResult {
        val diagnostics = mutableListOf<Diagnostic>()
        val plans = mutableListOf<Plan>()
        val firstFileByName = mutableMapOf<String, String>()

        for (file in files.sortedBy { it.path }) {
            val fileName = file.path.replace('\\', '/').substringAfterLast('/')
            val name = fileName.removeSuffix(".fsql")
            try {
                val header = HeaderParser.parse(file.text, name)
                val sql = HeaderParser.substituteParams(header.body).trim().removeSuffix(";")
                for (decl in header.params) {
                    if (!Regex(""":${Regex.escape(decl.name)}\b""").containsMatchIn(sql)) {
                        throw FsqlFileException(
                            name, 0,
                            "@${decl.name} is declared but never used in the SQL. Remove it, or use it in the query.",
                        )
                    }
                }
                val statement = try {
                    CCJSqlParserUtil.parse(sql)
                } catch (e: JSQLParserException) {
                    throw FsqlFileException(name, 0, "Could not parse the SQL: ${e.message}")
                }
                val body = PlanBuilder.bind(name, statement, header.params)
                val plan = Plan(name, header.params, body)

                val existing = firstFileByName[plan.name]
                if (existing != null) {
                    diagnostics += Diagnostic(
                        file.path, 1, 1,
                        "Another file already defines the procedure '${plan.name}': $existing. " +
                                "Procedure names must be unique across all folders.",
                    )
                } else {
                    firstFileByName[plan.name] = file.path
                    plans += plan
                }
            } catch (e: FsqlFileException) {
                diagnostics += Diagnostic(file.path, maxOf(e.line, 1), 1, e.message ?: "Invalid stored procedure.")
            }
        }
        return CompileResult(plans.sortedBy { it.name }, diagnostics)
    }
}
