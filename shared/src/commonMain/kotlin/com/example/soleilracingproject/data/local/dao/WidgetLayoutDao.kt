package com.example.soleilracingproject.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.example.soleilracingproject.data.local.entity.WidgetLayoutEntity

@Dao
interface WidgetLayoutDao {
    // REPLACE means: if the row already exists, overwrite it
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLayout(layout: WidgetLayoutEntity)

    @Query("SELECT * FROM widget_layout WHERE id = 'hud'")
    suspend fun getLayout(): WidgetLayoutEntity?
}