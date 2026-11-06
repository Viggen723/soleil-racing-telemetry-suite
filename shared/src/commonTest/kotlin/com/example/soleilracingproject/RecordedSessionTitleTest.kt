package com.example.soleilracingproject

import com.example.soleilracingproject.features.hud.viewmodel.formatRecordedSessionTitle
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class RecordedSessionTitleTest {
    @Test fun formatsRecordingStartInLocalTimeZone() {
        val epochMillis = Instant.parse("2026-11-06T06:30:00Z").toEpochMilliseconds()
        assertEquals(
            "Session 2026-11-06 15:30",
            formatRecordedSessionTitle(epochMillis, TimeZone.of("Asia/Tokyo")),
        )
    }

    @Test fun zeroPadsDateAndTimeAcrossDayBoundary() {
        val epochMillis = Instant.parse("2026-01-01T00:05:59Z").toEpochMilliseconds()
        assertEquals(
            "Session 2026-01-01 09:05",
            formatRecordedSessionTitle(epochMillis, TimeZone.of("Asia/Tokyo")),
        )
        assertEquals(
            "Session 2025-12-31 19:05",
            formatRecordedSessionTitle(epochMillis, TimeZone.of("America/New_York")),
        )
    }
}
