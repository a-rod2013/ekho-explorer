package com.fsql.data.internal

import com.fsql.plan.Plan
import com.fsql.plan.PlanBody
import com.fsql.plan.PlanJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PlanLoaderTest {

    @Test
    fun `plans are indexed by procedure name`() {
        val a = Plan("A", emptyList(), PlanBody.Read("t", null, emptyList(), null, null))
        val b = Plan("B", emptyList(), PlanBody.Read("t", null, emptyList(), null, null))
        val map = PlanLoader.parse(PlanJson.encode(listOf(a, b)))
        assertEquals(setOf("A", "B"), map.keys)
        assertEquals(a, map["A"])
    }

    @Test
    fun `a plan file from a different format version is rejected with a rebuild hint`() {
        val error = assertFailsWith<IllegalStateException> {
            PlanLoader.parse("""{"formatVersion":42,"plans":[]}""")
        }
        assertTrue(error.message!!.contains("Rebuild"), error.message!!)
    }

    @Test
    fun `the asset path matches what the Gradle plugin writes`() {
        assertEquals("fsql/plans.json", PlanLoader.ASSET_PATH)
    }
}
