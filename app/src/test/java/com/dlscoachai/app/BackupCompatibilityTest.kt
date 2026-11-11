package com.dlscoachai.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject

class BackupCompatibilityTest {
    @Test
    fun schemaThreeRoundTripsManagerLedger() {
        val manager = JSONObject()
            .put("coins", "1200")
            .put("gems", "45")
            .put("coaches", "3")
            .put("gifts", "2")
            .put("resourcesTime", 123456L)
            .put("decisions", org.json.JSONArray().put(JSONObject()
                .put("text", "Test compact defence")
                .put("status", "APPROVED")
                .put("time", 123456L)))
            .toString()

        val encoded = encodeBackup(initialSquad(), emptyList(), "5-3-2", "Aggressive attack", emptyList(), manager)
        val decoded = decodeBackup(encoded)
        val restored = JSONObject(decoded.managerData)

        assertEquals(3, JSONObject(encoded).getInt("schemaVersion"))
        assertEquals("1200", restored.getString("coins"))
        assertEquals("45", restored.getString("gems"))
        assertEquals(3, restored.getInt("coaches"))
        assertEquals(1, restored.getJSONArray("decisions").length())
    }

    @Test
    fun schemaTwoBackupRemainsReadableWithoutManagerData() {
        val old = JSONObject(encodeBackup(initialSquad(), emptyList(), "5-3-2", "Old style"))
            .put("schemaVersion", 2)
            .also { it.remove("managerData") }
            .toString()

        val decoded = decodeBackup(old)

        assertEquals("5-3-2", decoded.formation)
        assertEquals("{}", decoded.managerData)
        assertTrue(decoded.players.isNotEmpty())
    }
}
