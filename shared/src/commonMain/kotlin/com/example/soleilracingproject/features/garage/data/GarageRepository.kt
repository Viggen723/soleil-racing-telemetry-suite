package com.example.soleilracingproject.features.garage.data

import com.example.soleilracingproject.data.local.dao.CarDao
import com.example.soleilracingproject.data.mapper.toDomain
import com.example.soleilracingproject.data.mapper.toEntity
import com.example.soleilracingproject.model.garage.Car
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class GarageRepository(private val carDao: CarDao) {
    private var cachedCars: List<Car> = emptyList()

    val cars: Flow<List<Car>> = carDao.getAllCars().map { list ->
        val domainList = list.map { it.toDomain() }
        cachedCars = domainList
        domainList
    }

    private val _selectedCarId = MutableStateFlow<String?>(null)
    val selectedCarId: StateFlow<String?> = _selectedCarId.asStateFlow()

    fun selectCar(id: String?) {
        _selectedCarId.value = id
    }

    fun getActiveCar(): Car? {
        val activeId = _selectedCarId.value
        return cachedCars.firstOrNull { it.id == activeId } ?: cachedCars.firstOrNull()
    }

    suspend fun saveCar(car: Car) {
        carDao.insertCar(car.toEntity())
        if (_selectedCarId.value == null) {
            _selectedCarId.value = car.id
        }
    }

    suspend fun updateCar(car: Car) = carDao.updateCar(car.toEntity())

    suspend fun deleteCar(id: String) {
        if (_selectedCarId.value == id) {
            _selectedCarId.value = null
        }
        carDao.deleteCarById(id)
    }
}