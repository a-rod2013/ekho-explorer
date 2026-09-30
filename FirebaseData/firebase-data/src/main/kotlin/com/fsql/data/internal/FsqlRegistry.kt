package com.fsql.data.internal

import net.sf.jsqlparser.JSQLParserException
import net.sf.jsqlparser.parser.CCJSqlParserUtil

/**
 * Parses `.fsql` files once and keeps the result in memory. This is the only place that calls
 * [HeaderParser] and JSqlParser; everything else works with the resulting [ParsedProcedure]
 */
internal object FsqlRegistry {
    /**
     * Parses one file. [name] is the procedure name (the file name without `.fsql`).
     * @throws FsqlFileException when the header or the SQL is invalid
     */
    fun parseFile(name: String, text: String): ParsedProcedure {
        val header = HeaderParser.parse(text, fileName = name)
        val sql = HeaderParser.substituteParams(header.body).trim().removeSuffix(";")

        for(decl in header.params) {
            if(!Regex(""":${Regex.escape(decl.name)}\b""").containsMatchIn(sql)) {
                throw FsqlFileException(
                    name, 0,
                    "@${decl.name} is declared but never used in the SQL. Remove it or use it in the query."
                )
            }
        }

        val statement = try {
            CCJSqlParserUtil.parse(sql)
        } catch(e: JSQLParserException) {
            throw FsqlFileException(name, 0, "Could not parse the SQL: ${e.message}")
        }

        return ParsedProcedure(name, header.params, statement)
    }

    /**
     * Parses every file and returns files that fail. Does not exit on failure.
     */
    fun parseAll(files: Map<String, String>): ParseAllResult {
        val ok = LinkedHashMap<String, ParsedProcedure>()
        val problems = mutableListOf<FsqlFileException>()

        for((name, text) in files.toSortedMap()) {
            try {
                ok[name] = parseFile(name, text)
            } catch(e: FsqlFileException) {
                problems += e
            }
        }

        return ParseAllResult(ok, problems)
    }
}

internal data class ParseAllResult(
    val procedures: Map<String, ParsedProcedure>,
    val problems: List<FsqlFileException>,
) {
    val ok: Boolean get() = problems.isEmpty()
}