package pl.kawadlataty.dad

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

object Api {
    private val db = FirebaseFirestore.getInstance()
    private val zone = ZoneId.of("Europe/Warsaw")

    private fun today(): String = LocalDate.now(zone).toString()

    private fun coffeeName(type: String): String = when (type) {
        "AMERICANO" -> "AMERICANO"
        "LATTE" -> "LATTE"
        "ESPRESSO" -> "ESPRESSO"
        "CAPPUCCINO" -> "CAPPUCCINO"
        "KAWKAO" -> "KAWKAO"
        "KAWKAO_MINI" -> "KAWKAO MINI"
        "MILK_KAWKAO" -> "Mleczne KAWKAO"
        "MILK_KAWKAO_MINI" -> "Mleczne KAWKAO MINI"
        else -> type
    }

    private fun minutesSinceMeal(lastMealAt: String?): Int? {
        if (lastMealAt == null) return null
        val diff = Instant.now().toEpochMilli() - Instant.parse(lastMealAt).toEpochMilli()
        return (diff / 60000).toInt()
    }

    suspend fun state(): AppState {
        val stateDoc = db.collection("app").document("state").get().await()
        val lastMealAt = stateDoc.getString("lastMealAt")
        val date = today()

        val ordersSnap = db.collection("orders")
            .whereEqualTo("date", date)
            .get()
            .await()

        val orders = ordersSnap.documents.mapNotNull { doc ->
            Order(
                id = doc.id,
                coffeeType = doc.getString("coffeeType") ?: return@mapNotNull null,
                coffeeName = doc.getString("coffeeName") ?: "",
                status = doc.getString("status") ?: "NOWE",
                createdAt = doc.getString("createdAt") ?: "",
                minutesSinceMealAtOrder = doc.getLong("minutesSinceMealAtOrder")?.toInt(),
                warningOverridden = doc.getBoolean("warningOverridden") ?: false
            )
        }.sortedByDescending { it.createdAt }

        return AppState(
            date = date,
            lastMealAt = lastMealAt,
            minutesSinceMeal = minutesSinceMeal(lastMealAt),
            orders = orders
        )
    }

    suspend fun registerToken(role: String, token: String) {
        db.collection("devices").document(role).set(
            mapOf("token" to token, "updatedAt" to Instant.now().toString())
        ).await()
    }

    suspend fun meal() {
        db.collection("app").document("state").set(
            mapOf("lastMealAt" to Instant.now().toString())
        ).await()
    }

    suspend fun order(coffeeType: String, override: Boolean = false): Pair<Order?, WarningResponse?> {
        val currentState = state()
        val mins = currentState.minutesSinceMeal
        if (mins != null && mins < 60 && !override) {
            return null to WarningResponse(true, mins, 60 - mins)
        }

        val id = UUID.randomUUID().toString()
        val order = Order(
            id = id,
            coffeeType = coffeeType,
            coffeeName = coffeeName(coffeeType),
            status = "NOWE",
            createdAt = Instant.now().toString(),
            minutesSinceMealAtOrder = mins,
            warningOverridden = override
        )

        db.collection("orders").document(id).set(
            mapOf(
                "date" to today(),
                "coffeeType" to order.coffeeType,
                "coffeeName" to order.coffeeName,
                "status" to order.status,
                "createdAt" to order.createdAt,
                "minutesSinceMealAtOrder" to order.minutesSinceMealAtOrder,
                "warningOverridden" to order.warningOverridden
            )
        ).await()

        return order to null
    }

    suspend fun status(orderId: String, status: String): Order {
        db.collection("orders").document(orderId).update(
            mapOf(
                "status" to status,
                "updatedAt" to Instant.now().toString()
            )
        ).await()

        return state().orders.first { it.id == orderId }
    }

    suspend fun cancelMeal() {
        db.collection("app")
            .document("state")
            .update("lastMealAt", null)
            .await()
    }
}