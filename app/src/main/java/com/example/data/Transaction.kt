package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // "EXPENSE" or "INCOME"
    val categoryName: String,
    val timestamp: Long,
    val note: String = ""
) {
    val isExpense: Boolean
        get() = type == "EXPENSE"

    val isIncome: Boolean
        get() = type == "INCOME"

    // Helper to get formatted date string
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // Helper to get formatted month/day (e.g. 05-22)
    fun getFormattedDay(): String {
        val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

enum class TransactionType {
    EXPENSE, INCOME
}

data class CategoryPreset(
    val id: String,
    val displayName: String,
    val iconName: String,
    val colorHex: String,
    val isDefaultIncome: Boolean = false
)

object Categories {
    val presets = listOf(
        CategoryPreset("food", "餐飲美食", "Fastfood", "#FF9800"),      // Orange
        CategoryPreset("shopping", "日常購物", "ShoppingCart", "#E91E63"),  // Pink
        CategoryPreset("transport", "交通出行", "DirectionsCar", "#2196F3"), // Blue
        CategoryPreset("entertainment", "休閒娛樂", "Movie", "#9C27B0"),    // Purple
        CategoryPreset("utilities", "水電物業", "Lightbulb", "#795548"),   // Brown
        CategoryPreset("medical", "醫療健康", "MedicalServices", "#009688"),// Teal
        CategoryPreset("salary", "薪資收入", "AttachMoney", "#4CAF50", true), // Green (Income)
        CategoryPreset("investment", "投資理財", "TrendingUp", "#3F51B5", true), // Indigo (Income)
        CategoryPreset("misc", "其他雜項", "Category", "#607D8B")          // Blue Grey
    )

    fun getCategory(name: String): CategoryPreset {
        return presets.firstOrNull { it.displayName == name || it.id == name }
            ?: CategoryPreset("misc", name, "Category", "#607D8B")
    }
}
