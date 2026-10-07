package com.fsql.data.internal

import android.content.res.AssetManager
import com.fsql.plan.Plan
import com.fsql.plan.PlanJson
import java.io.FileNotFoundException

/** Reads the compiled stored procedures the fsql Gradle plugin put into the app's assets. */
internal object PlanLoader {

    const val ASSET_PATH = "fsql/plans.json"

    /** @throws IllegalStateException when the asset is missing or was written by another plan format version */
    fun load(assets: AssetManager): Map<String, Plan> {
        val text = try {
            assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        } catch (e: FileNotFoundException) {
            throw IllegalStateException(
                "The compiled stored procedures ($ASSET_PATH) are missing from the app. " +
                        "Apply the com.fsql.compile Gradle plugin and put your .fsql files in src/main/fsql.",
                e,
            )
        }
        return parse(text)
    }

    fun parse(text: String): Map<String, Plan> = PlanJson.decode(text).plans.associateBy { it.name }
}
