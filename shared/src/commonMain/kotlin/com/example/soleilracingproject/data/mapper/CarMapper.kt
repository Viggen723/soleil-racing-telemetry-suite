package com.example.soleilracingproject.data.mapper

import com.example.soleilracingproject.data.local.entity.CarEntity
import com.example.soleilracingproject.model.garage.Car

fun CarEntity.toDomain(): Car {
    return Car(
        id = id,
        name = name,
        make = make,
        model = model,
        year = year,
        specifications = specifications,
        modifications = modifications,
        photoPath = photoPath
    )
}

fun Car.toEntity(): CarEntity {
    return CarEntity(
        id = id,
        name = name,
        make = make,
        model = model,
        year = year,
        specifications = specifications,
        modifications = modifications,
        photoPath = photoPath
    )
}
