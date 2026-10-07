package com.fsql.compiler

import com.fsql.plan.ParamDecl
import com.fsql.plan.ParamType

data class HeaderResult(val params: List<ParamDecl>, val body: String)

/**
 * Reads the `-- @name: TYPE` header lines at the top of a `.fsql` file.
 */
object HeaderParser {

    private val PARAM_LINE =
        Regex("""^--\s*@(\w+):\s*(\w+)(\[\s*\])?\s*(\(\s*nullable\s*\))?\s*$""", RegexOption.IGNORE_CASE)
    private val AT_PARAM = Regex("""@(\w+)""")

    /** @throws FsqlFileException when a header line is malformed */
    fun parse(text: String, fileName: String = "<unknown>"): HeaderResult {
        if (text.isBlank()) throw FsqlFileException(fileName, 0, "The file is empty.")

        val lines = text.lines()
        val params = mutableListOf<ParamDecl>()
        val seen = mutableSetOf<String>()
        var bodyStart = 0

        for ((index, line) in lines.withIndex()) {
            val match = PARAM_LINE.matchEntire(line)
            if (match == null) {
                bodyStart = index
                break
            }
            val name = match.groupValues[1]
            val typeWord = match.groupValues[2]
            val isList = match.groupValues[3].isNotEmpty()
            val nullable = match.groupValues[4].isNotEmpty()
            val type = runCatching { ParamType.valueOf(typeWord.uppercase()) }.getOrNull()
                ?: throw FsqlFileException(
                    fileName, index + 1,
                    "Unknown parameter type '$typeWord' for @$name. Supported types: " +
                            ParamType.entries.joinToString(", ") + ".",
                )
            if (!seen.add(name)) {
                throw FsqlFileException(fileName, index + 1, "@$name is declared twice.")
            }
            params += ParamDecl(name, type, nullable, isList)
            bodyStart = index + 1
        }

        val body = lines.drop(bodyStart).joinToString("\n")
        if (body.isBlank()) throw FsqlFileException(fileName, 0, "The file has no SQL statement.")
        return HeaderResult(params, body)
    }

    /** Turns `@name` into `:name`, the parameter syntax JSqlParser understands. */
    fun substituteParams(sql: String): String = AT_PARAM.replace(sql) { ":" + it.groupValues[1] }
}
