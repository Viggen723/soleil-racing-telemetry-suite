package com.example.soleilracingproject.data.local.relation

import androidx.room3.Embedded
import androidx.room3.Relation
import com.example.soleilracingproject.data.local.entity.LapEntity
import com.example.soleilracingproject.data.local.entity.SessionEntity

data class SessionWithLaps(
    @Embedded val session: SessionEntity,

    @Relation(
        parentColumns = ["id"],
        entityColumns = ["sessionId"]
    )
    val laps: List<LapEntity>
)
