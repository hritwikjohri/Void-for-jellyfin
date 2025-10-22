package com.hritwik.avoid.websocket

import com.hritwik.avoid.data.remote.websocket.PlaybackEvent
import com.hritwik.avoid.data.remote.websocket.PlaybackEventParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class PlaybackEventParserTest {
    private val parser = PlaybackEventParser()

    @Test
    fun `parse progress event`() {
        val json = """{"MessageType":"PlayState","DeviceId":"d1","Data":{"UserId":"u1","ItemId":"i1","DatePlayed":"2024","PlayState":{"PositionTicks":10,"RunTimeTicks":100}}}"""
        val event = parser.parse(json, "u1", "d1")
        val progress = assertIs<PlaybackEvent.Progress>(event)
        assertEquals("i1", progress.itemId)
        assertEquals(10, progress.positionTicks)
        assertEquals(100, progress.runTimeTicks)
        assertEquals("2024", progress.datePlayed)
    }

    @Test
    fun `parse stop event`() {
        val json = """{"MessageType":"PlaybackStopped","DeviceId":"d1","Data":{"UserId":"u1","ItemId":"i1","PlayState":{"PositionTicks":100,"RunTimeTicks":100}}}"""
        val event = parser.parse(json, "u1", "d1")
        val stop = assertIs<PlaybackEvent.Stop>(event)
        assertEquals("i1", stop.itemId)
        assertEquals(100, stop.positionTicks)
    }

    @Test
    fun `filters out other users`() {
        val json = """{"MessageType":"PlayState","DeviceId":"d1","Data":{"UserId":"u2","ItemId":"i1","PlayState":{"PositionTicks":10,"RunTimeTicks":100}}}"""
        val event = parser.parse(json, "u1", "d1")
        assertNull(event)
    }
}
