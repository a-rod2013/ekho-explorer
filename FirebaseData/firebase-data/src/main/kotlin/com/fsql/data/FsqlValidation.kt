package com.fsql.data

import com.fsql.data.internal.PlanLoader


object FsqlValidation {
    /** @return the names of every compiled procedure. @throws Exception if the file is missing, unreadable, or stale. */
    fun checkPlanFile(text: String): List<String> = PlanLoader.parse(text).keys.sorted()
}
