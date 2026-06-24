package pl.kawadlataty.dad

import kotlinx.serialization.Serializable

@Serializable data class AppState(val date: String, val lastMealAt: String? = null, val minutesSinceMeal: Int? = null, val orders: List<Order> = emptyList())
@Serializable data class Order(val id: String, val coffeeType: String, val coffeeName: String, val status: String, val createdAt: String, val minutesSinceMealAtOrder: Int? = null, val warningOverridden: Boolean = false)
@Serializable data class TokenRequest(val role: String, val token: String)
@Serializable data class CoffeeRequest(val coffeeType: String, val overrideWarning: Boolean = false)
@Serializable data class StatusRequest(val status: String)
@Serializable data class WarningResponse(val warning: Boolean, val minutesSinceMeal: Int, val minutesLeft: Int)
