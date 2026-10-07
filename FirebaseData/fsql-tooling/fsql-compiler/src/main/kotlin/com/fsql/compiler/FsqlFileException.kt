package com.fsql.compiler

/** A problem in one `.fsql` file. [line] is 1-based, or 0 when the problem has no specific line. */
class FsqlFileException(val fileName: String, val line: Int, message: String) : Exception(message)
