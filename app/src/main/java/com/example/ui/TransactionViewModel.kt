package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = TransactionRepository(database.transactionDao())

    // Filter states
    private val _filterType = MutableStateFlow("全部") // "全部", "支出", "收入"
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    private val _filterCategory = MutableStateFlow("全部") // "全部" or specific category displayNames
    val filterCategory: StateFlow<String> = _filterCategory.asStateFlow()

    // Map raw transactions database flow
    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered transaction list for display
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions,
        _filterType,
        _filterCategory
    ) { transactions, type, category ->
        transactions.filter { tx ->
            val matchesType = when (type) {
                "支出" -> tx.isExpense
                "收入" -> tx.isIncome
                else -> true
            }
            val matchesCategory = when (category) {
                "全部" -> true
                else -> tx.categoryName == category
            }
            matchesType && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Financial balance flows
    val totalIncome: StateFlow<Double> = allTransactions.map { list ->
        list.filter { it.isIncome }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = allTransactions.map { list ->
        list.filter { it.isExpense }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val netBalance: StateFlow<Double> = combine(totalIncome, totalExpense) { income, expense ->
        income - expense
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Category summary statistics (computed from Expense transactions)
    val categoryStatistics: StateFlow<List<CategoryStat>> = allTransactions.map { list ->
        val expenseTx = list.filter { it.isExpense }
        val sumMap = expenseTx.groupBy { it.categoryName }
            .mapValues { entry -> entry.value.sumOf { tx -> tx.amount } }

        val totalExp = sumMap.values.sum()

        // Map categories back to presets with percentages
        Categories.presets.filter { !it.isDefaultIncome }.map { preset ->
            val total = sumMap[preset.displayName] ?: 0.0
            val percentage = if (totalExp > 0) (total / totalExp * 100).toFloat() else 0f
            CategoryStat(preset, total, percentage)
        }
        .filter { it.totalAmount > 0 } // Only display categories that have expenses
        .sortedByDescending { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Auto seed sample data if the app launches completely cold and empty
        viewModelScope.launch {
            repository.allTransactions.first().let { currentList ->
                if (currentList.isEmpty()) {
                    seedSampleTransactions()
                }
            }
        }
    }

    fun setFilterType(type: String) {
        _filterType.value = type
    }

    fun setFilterCategory(category: String) {
        _filterCategory.value = category
    }

    fun addTransaction(amount: Double, type: String, categoryName: String, note: String = "", timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val transaction = Transaction(
                amount = amount,
                type = type,
                categoryName = categoryName,
                timestamp = timestamp,
                note = note
            )
            repository.insert(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.delete(transaction)
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            allTransactions.value.forEach {
                repository.delete(it)
            }
        }
    }

    fun seedSampleTransactions() {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val now = cal.timeInMillis

            // Today
            repository.insert(Transaction(amount = 120.0, type = "EXPENSE", categoryName = "餐飲美食", timestamp = now, note = "午餐牛肉麵"))
            repository.insert(Transaction(amount = 75.0, type = "EXPENSE", categoryName = "餐飲美食", timestamp = now, note = "超商研磨拿鐵"))
            
            // Yesterday
            cal.add(Calendar.DAY_OF_YEAR, -1)
            repository.insert(Transaction(amount = 45000.0, type = "INCOME", categoryName = "薪資收入", timestamp = cal.timeInMillis, note = "5月份薪水發放"))
            repository.insert(Transaction(amount = 1580.0, type = "EXPENSE", categoryName = "日常購物", timestamp = cal.timeInMillis, note = "百貨公司買睡衣"))
            
            // Three days ago
            cal.add(Calendar.DAY_OF_YEAR, -2)
            repository.insert(Transaction(amount = 450.0, type = "EXPENSE", categoryName = "休閒娛樂", timestamp = cal.timeInMillis, note = "跟朋友看電影"))
            repository.insert(Transaction(amount = 1200.0, type = "INCOME", categoryName = "投資理財", timestamp = cal.timeInMillis, note = "股票現金股利"))
            repository.insert(Transaction(amount = 150.0, type = "EXPENSE", categoryName = "交通出行", timestamp = cal.timeInMillis, note = "悠遊卡自動加值"))
            
            // Four days ago
            cal.add(Calendar.DAY_OF_YEAR, -1)
            repository.insert(Transaction(amount = 890.0, type = "EXPENSE", categoryName = "水電物業", timestamp = cal.timeInMillis, note = "本月自來水費"))
        }
    }
}

data class CategoryStat(
    val category: CategoryPreset,
    val totalAmount: Double,
    val percentage: Float
)
