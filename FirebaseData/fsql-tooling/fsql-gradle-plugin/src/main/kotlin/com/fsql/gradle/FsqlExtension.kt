package com.fsql.gradle

import org.gradle.api.file.DirectoryProperty

/** Configuration block: `fsql { sourceDir.set(...) }` (optional; defaults to `src/main/fsql`). */
abstract class FsqlExtension {
    abstract val sourceDir: DirectoryProperty
}
