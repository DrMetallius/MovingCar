package com.malcolmsoft.movingcar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class MovingCarModel(application: Application) : AndroidViewModel(application) {
	private var width = 0
	private var height = 0

	private val carPositionDataMutable = MutableLiveData<CarPosition>()
	val carPositionData: LiveData<CarPosition> = carPositionDataMutable

	fun setDimensions(width: Int, height: Int) {
		if (width == this.width || height == this.height) return

		val currPosition = carPositionDataMutable.value
		if (currPosition == null) {
			carPositionDataMutable.value = CarPosition(width / 2F, height / 2F)
		} else  {
			val newX = currPosition.x / this.width * width
			val newY = currPosition.y / this.height * height
			carPositionDataMutable.value = currPosition.copy(x = newX, y = newY)
		}

		this.width = width
		this.height = height
	}

	fun setCarPosition(x: Float, y: Float) {
		carPositionDataMutable.value = carPositionDataMutable.value?.copy(x = x, y = y) ?: CarPosition(x, y)
	}

	data class CarPosition(val x: Float, val y: Float)
}