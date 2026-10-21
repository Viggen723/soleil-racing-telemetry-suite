package com.example.soleilracingproject.features.garage.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.soleilracingproject.features.garage.data.GarageRepository
import com.example.soleilracingproject.model.garage.Car
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class GarageViewmodel(private val repository: GarageRepository) : ViewModel() {
    val cars = repository.cars.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val selectedCarId = repository.selectedCarId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun selectCar(carId: String?) {
        repository.selectCar(carId)
    }

    fun addCar(car: Car, onSaved: () -> Unit) = write(onSaved) {
        val newId = "car_${Random.nextLong().toString(16)}"
        repository.saveCar(car.copy(id = newId))
    }

    fun editCar(car: Car, onSaved: () -> Unit) = write(onSaved) { repository.updateCar(car) }
    fun deleteCar(id: String, onDeleted: () -> Unit) = write(onDeleted) { repository.deleteCar(id) }

    private fun write(onSuccess: () -> Unit, operation: suspend () -> Unit) {
        if (_isSaving.value) return
        _isSaving.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                operation()
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "Couldn't save the change. Please try again."
            } finally {
                _isSaving.value = false
            }
        }
    }
}