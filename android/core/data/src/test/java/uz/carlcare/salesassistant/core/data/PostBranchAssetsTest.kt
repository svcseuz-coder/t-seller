package uz.carlcare.salesassistant.core.data

import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostBranchAssetsTest {

    private fun asset(): JSONObject {
        val file = listOf("src/main/assets/post_branches.json", "core/data/src/main/assets/post_branches.json")
            .map(::File)
            .firstOrNull { it.exists() }
            ?: error("missing post_branches.json (cwd=${File(".").absolutePath})")
        return JSONObject(file.readText())
    }

    @Test
    fun `post branch bundle keeps every exported branch`() {
        val root = asset()
        assertEquals("2026-11-16", root.getString("checkedAt"))
        val branches = root.getJSONArray("branches")
        assertEquals(218, branches.length())

        val ids = mutableSetOf<Int>()
        val names = mutableSetOf<String>()
        val regions = mutableSetOf<String>()
        for (i in 0 until branches.length()) {
            val branch = branches.getJSONObject(i)
            assertTrue("duplicate id", ids.add(branch.getInt("id")))
            assertTrue("duplicate name", names.add(branch.getString("name")))
            regions.add(branch.getString("region"))
            assertTrue("blank city", branch.getString("city").isNotBlank())
            assertTrue("blank address", branch.getString("address").isNotBlank())
            assertTrue("blank hours", branch.getString("hours").isNotBlank())

            val phones = branch.getJSONArray("phones")
            assertTrue("no phone in ${branch.getString("name")}", phones.length() > 0)
            for (p in 0 until phones.length()) {
                val digits = phones.getString(p)
                assertTrue("bad phone $digits", digits.all(Char::isDigit))
                assertTrue("bad phone $digits", digits.length in 9..13)
            }

            val lat = branch.getDouble("lat")
            val lng = branch.getDouble("lng")
            assertTrue("bad lat $lat", lat in 37.0..46.0)
            assertTrue("bad lng $lng", lng in 55.0..74.0)
        }
        assertEquals(14, regions.size)
    }

    @Test
    fun `every branch is found by its own city and name`() {
        val branches = asset().getJSONArray("branches")
        for (i in 0 until branches.length()) {
            val b = branches.getJSONObject(i)
            val haystack =
                "${b.getString("city")} ${b.getString("region")} ${b.getString("name")} ${b.getString("address")}"
            assertTrue(b.getString("name"), SearchText.matches(haystack, b.getString("city")))
            assertTrue(b.getString("name"), SearchText.matches(haystack, b.getString("name")))
        }
    }
}
