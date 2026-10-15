package com.example.soleilracingproject.features.garage.data

import com.example.soleilracingproject.data.local.dao.CarDao
import com.example.soleilracingproject.data.mapper.toDomain
import com.example.soleilracingproject.data.mapper.toEntity
import com.example.soleilracingproject.model.garage.Car
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GarageRepository(private val carDao: CarDao) {
    // Room stays the source of truth; the screen works with domain cars.
    val cars: Flow<List<Car>> = carDao.getAllCars().map { cars -> cars.map { it.toDomain() } }

    suspend fun saveCar(car: Car) = carDao.insertCar(car.toEntity())
    suspend fun updateCar(car: Car) = carDao.updateCar(car.toEntity())
    suspend fun deleteCar(id: String) = carDao.deleteCarById(id)
}
