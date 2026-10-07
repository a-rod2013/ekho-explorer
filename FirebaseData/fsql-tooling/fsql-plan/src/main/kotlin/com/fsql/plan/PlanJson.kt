package com.fsql.plan

import kotlinx.serialization.json.Json

/** Encodes and decodes the `fsql/plans.json` asset: the compiler writes it, the runtime reads it. */
object PlanJson {
    private val json = Json { classDiscriminator = "t" }

    fun encode(plans: List<Plan>): String =
        json.encodeToString(PlanFile.serializer(), PlanFile(PLAN_FORMAT_VERSION, plans))

    /** @throws IllegalStateException when the file was written by a different plan format version */
    fun decode(text: String): PlanFile {
        val file = json.decodeFromString(PlanFile.serializer(), text)
        check(file.formatVersion == PLAN_FORMAT_VERSION) {
            "The compiled stored procedure file has format version ${file.formatVersion}, but this " +
                    "app understands version $PLAN_FORMAT_VERSION. Rebuild the app."
        }
        return file
    }
}
