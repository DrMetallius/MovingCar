package com.malcolmsoft.movingcar

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Path
import android.graphics.PathMeasure
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sign
import kotlin.math.sin

const val TURN_RADIUS = 200

fun Double.toRange(min: Double, max: Double): Double {
	val interval = max - min

	var result = this

	while (result < min) result += interval
	while (result > max) result -= interval

	return result
}

class MovingCarActivityFragment : Fragment() {
	private val model by lazy {
		ViewModelProviders.of(this)[MovingCarModel::class.java]
	}
	private lateinit var carView: View

	private var currentAnimator: Animator? = null

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
		inflater.inflate(R.layout.fragment_moving_car, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		carView = view.findViewById(R.id.moving_car)
		var lastPosition: MovingCarModel.CarPosition? = null

		view.doOnLayout {
			model.setDimensions(view.width, view.height)

			model.carPositionData.observe(viewLifecycleOwner, Observer { carPosition ->
				val (x, y) = carPosition

				lastPosition?.let { (lastX, lastY) ->
					val lastAngle = carView.rotation * PI / 180

					val animationParameters = calculateAnimationParameters(lastX.toDouble(), lastY.toDouble(), lastAngle, x.toDouble(), y.toDouble())
					currentAnimator = createAnimations(animationParameters).also(AnimatorSet::start)
				} ?: run {
					carView.x = x
					carView.y = y
				}

				lastPosition = carPosition
			})
		}

		view.setOnTouchListener { _, event ->
			if (currentAnimator?.isStarted == true) return@setOnTouchListener false

			model.setCarPosition(event.x, event.y)
			true
		}
	}

	private fun calculateAnimationParameters(lastX: Double, lastY: Double, lastAngle: Double, x: Double, y: Double): AnimationParameters {
		data class TurnParameters(
			val turnDirectionFactor: Double,
			val turnCenterX: Double,
			val turnCenterY: Double,
			val turnRadiusVectorX: Double,
			val turnRadiusVectorY: Double,
			val distanceVectorX: Double,
			val distanceVectorY: Double,
			val distance: Double
		)

		fun getTurnParameters(startX: Double, startY: Double): TurnParameters {
			val turnDirectionFactor = (atan2(y - startY, x - startX) + PI / 2 - lastAngle).toRange(-PI, PI).sign
			val turnCenterAngle = (lastAngle + PI / 2 * turnDirectionFactor).toRange(-PI, PI)
			val turnCenterX = startX + TURN_RADIUS * sin(turnCenterAngle)
			val turnCenterY = startY - TURN_RADIUS * cos(turnCenterAngle)

			val turnRadiusVectorX = startX - turnCenterX
			val turnRadiusVectorY = startY - turnCenterY

			val distanceVectorX = x - turnCenterX
			val distanceVectorY = y - turnCenterY
			val distance = hypot(distanceVectorX, distanceVectorY)

			return TurnParameters(
				turnDirectionFactor,
				turnCenterX,
				turnCenterY,
				turnRadiusVectorX,
				turnRadiusVectorY,
				distanceVectorX,
				distanceVectorY,
				distance
			)
		}

		val (startX, startY, params) = getTurnParameters(lastX, lastY).let { params ->
			if (params.distance < TURN_RADIUS) {
				// 2 * TURN_RADIUS is the minimum always safe value, could be if calculated case by case
				val startX = lastX + 2 * TURN_RADIUS * sin(lastAngle)
				val startY = lastY - 2 * TURN_RADIUS * cos(lastAngle)
				Triple(startX, startY, getTurnParameters(startX, startY))
			} else {
				Triple(null, null, params)
			}
		}
		val (turnDirectionFactor, turnCenterX, turnCenterY, turnRadiusVectorX, turnRadiusVectorY, distanceVectorX, distanceVectorY, distance) = params

		val turnRadiusAngle = atan2(turnRadiusVectorY, turnRadiusVectorX)
		val fullTurnAngle = (atan2(distanceVectorY, distanceVectorX) - turnRadiusAngle)
		val extraTurnAngle = turnDirectionFactor * acos(TURN_RADIUS / distance)
		val sufficientTurnAngle = (fullTurnAngle - extraTurnAngle).toRange(PI * (turnDirectionFactor - 1), PI * (turnDirectionFactor + 1))

		return AnimationParameters(lastX, lastY, lastAngle, startX, startY, turnCenterX, turnCenterY, turnRadiusAngle, sufficientTurnAngle, x, y)
	}

	private fun createAnimations(animationParameters: AnimationParameters): AnimatorSet {
		val (lastX, lastY, lastAngle, startX, startY, turnCenterX, turnCenterY, turnRadiusAngle, sufficientTurnAngle, x, y) = animationParameters

		val animatorSet = AnimatorSet().apply {
			interpolator = AnimationUtils.loadInterpolator(activity, android.R.interpolator.linear)
			duration = resources.getInteger(android.R.integer.config_longAnimTime).toLong()
		}

		val startPositioningAnimator = if (startX != null && startY != null) {
			val startPositioningPath = Path().apply {
				moveTo(lastX.toFloat(), lastY.toFloat())
				lineTo(startX.toFloat(), startY.toFloat())
			}

			ObjectAnimator.ofFloat(carView, View.X, View.Y, startPositioningPath)
		} else null

		val path = Path().apply {
			arcTo(
				(turnCenterX - TURN_RADIUS).toFloat(),
				(turnCenterY - TURN_RADIUS).toFloat(),
				(turnCenterX + TURN_RADIUS).toFloat(),
				(turnCenterY + TURN_RADIUS).toFloat(),
				(turnRadiusAngle * 180 / PI).toFloat(),
				(sufficientTurnAngle * 180 / PI).toFloat(),
				false
			)
		}

		val turnMovementAnimator = ObjectAnimator.ofFloat(carView, View.X, View.Y, path)
		animatorSet.play(turnMovementAnimator)

		val rotation = ((lastAngle + sufficientTurnAngle) * 180 / PI).toFloat()
		val turnRotationAnimator = ObjectAnimator.ofFloat(carView, View.ROTATION, carView.rotation, rotation)

		animatorSet.play(turnRotationAnimator).with(turnMovementAnimator)
		startPositioningAnimator?.let { animatorSet.play(startPositioningAnimator).before(turnMovementAnimator) }

		val finalMovementPath = Path().apply {
			val measure = PathMeasure(path, false)
			val finalMovementPos = FloatArray(2)
			measure.getPosTan(measure.length, finalMovementPos, null)

			moveTo(finalMovementPos[0], finalMovementPos[1])
			lineTo(x.toFloat(), y.toFloat())
		}

		val finalMovementAnimator = ObjectAnimator.ofFloat(carView, View.X, View.Y, finalMovementPath)
		animatorSet.play(finalMovementAnimator).after(turnMovementAnimator)

		return animatorSet
	}

	private data class AnimationParameters(
		val lastX: Double,
		val lastY: Double,
		val lastAngle: Double,
		val startX: Double?,
		val startY: Double?,
		val turnCenterX: Double,
		val turnCenterY: Double,
		val turnRadiusAngle: Double,
		val sufficientTurnAngle: Double,
		val x: Double,
		val y: Double
	)
}
