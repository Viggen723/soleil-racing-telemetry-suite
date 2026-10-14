package com.example.soleilracingproject.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.example.soleilracingproject.data.local.entity.CarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCar(car: CarEntity)

    @Update
    suspend fun updateCar(car: CarEntity)

    @Query("DELETE FROM cars WHERE id = :id")
    suspend fun deleteCarById(id: String)

    @Query("SELECT * FROM cars WHERE id = :id")
    fun getCarById(id: String): Flow<CarEntity?>

    @Query("SELECT * FROM cars ORDER BY name ASC, id ASC")
    fun getAllCars(): Flow<List<CarEntity>>
}
