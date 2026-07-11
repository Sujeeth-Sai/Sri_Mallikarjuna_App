package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

object RepositoryProvider {
    val repository = FactoryRepository()
    val feedbackMessage = MutableStateFlow<String?>(null)

    fun showFeedback(msg: String) {
        feedbackMessage.value = msg
    }
}

// ==========================================
// 1. INVENTORY VIEW MODEL
// ==========================================
class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepositoryProvider.repository

    val products: StateFlow<List<Product>> = repository.allProducts
    val contacts: StateFlow<List<Contact>> = repository.allContacts
    val stockLogs: StateFlow<List<StockTransaction>> = repository.allStockTransactions

    fun addProduct(
        name: String,
        classification: String,
        unit: String,
        initialStock: Double,
        costPrice: Double,
        sellingPrice: Double,
        reorderThreshold: Double
    ) {
        viewModelScope.launch {
            try {
                val product = Product(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    classification = classification,
                    unit = unit,
                    current_stock = initialStock,
                    cost_per_kg = costPrice,
                    selling_price_per_kg = sellingPrice,
                    reorder_threshold = reorderThreshold,
                    is_archived = false
                )
                repository.insertProduct(product)
                RepositoryProvider.showFeedback("Product '$name' added.")
            } catch (e: Exception) {
                RepositoryProvider.showFeedback("Error: ${e.localizedMessage}")
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            try {
                repository.deleteProduct(product.id)
                RepositoryProvider.showFeedback("Product deleted.")
            } catch (e: Exception) {
                RepositoryProvider.showFeedback("Error: ${e.localizedMessage}")
            }
        }
    }

    fun addContact(name: String, classification: String, phone: String, location: String) {
        viewModelScope.launch {
            try {
                val contact = Contact(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    type = classification,
                    phone = phone,
                    email = "",
                    address = location,
                    balance = 0.0
                )
                repository.insertContact(contact)
                RepositoryProvider.showFeedback("Contact '$name' added.")
            } catch (e: Exception) {
                RepositoryProvider.showFeedback("Error: ${e.localizedMessage}")
            }
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            try {
                repository.deleteContact(contact.id)
                RepositoryProvider.showFeedback("Contact deleted.")
            } catch (e: Exception) {
                RepositoryProvider.showFeedback("Error: ${e.localizedMessage}")
            }
        }
    }

    fun logStockMovement(
        productId: String,
        productName: String,
        productClassification: String,
        type: String,
        quantity: Double,
        pricePerKg: Double,
        contactId: String,
        contactName: String
    ) {
        viewModelScope.launch {
            try {
                val transaction = StockTransaction(
                    id = UUID.randomUUID().toString(),
                    product_id = productId,
                    contact_id = contactId,
                    type = type,
                    quantity = quantity,
                    price_per_kg = pricePerKg,
                    total_amount = quantity * pricePerKg,
                    remarks = "productName:$productName|contactName:$contactName|classification:$productClassification",
                    transaction_date = getCurrentDateTimeString()
                )
                repository.insertStockTransactionAndUpdateStock(transaction)
                RepositoryProvider.showFeedback("Stock transaction logged.")
            } catch (e: Exception) {
                RepositoryProvider.showFeedback("Error: ${e.localizedMessage}")
            }
        }
    }

    fun deleteStockLog(transaction: StockTransaction) {
        viewModelScope.launch {
            try {
                repository.deleteStockTransactionAndRevertStock(transaction)
                RepositoryProvider.showFeedback("Stock log deleted.")
            } catch (e: Exception) {
                RepositoryProvider.showFeedback("Error: ${e.localizedMessage}")
            }
        }
    }
}

// ==========================================
// 2. EMPLOYEE VIEW MODEL
// ==========================================
class EmployeeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepositoryProvider.repository

    val employees: StateFlow<List<Employee>> = repository.allEmployees

    fun addEmployee(name: String, designation: String, monthlySalary: Double) {
        viewModelScope.launch {
            try {
                val employee = Employee(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    designation = designation,
                    monthly_salary = monthlySalary,
                    phone = "",
                    joining_date = "2026-01-01",
                    is_active = true
                )
                repository.insertEmployee(employee)
                RepositoryProvider.showFeedback("Employee '$name' added.")
            } catch (e: Exception) {
                RepositoryProvider.showFeedback("Error: ${e.localizedMessage}")
            }
        }
    }

    fun deleteEmployee(employee: Employee) {
        viewModelScope.launch {
            try {
                repository.deleteEmployee(employee.id)
                RepositoryProvider.showFeedback("Employee deleted.")
            } catch (e: Exception) {
                RepositoryProvider.showFeedback("Error: ${e.localizedMessage}")
            }
        }
    }
}

// ==========================================
// 3. ATTENDANCE VIEW MODEL
// ==========================================
class AttendanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepositoryProvider.repository

    val allAttendance: StateFlow<List<Attendance>> = repository.allAttendance
    val selectedDateStr = MutableStateFlow(getCurrentDateString())

    val attendanceForSelectedDate: StateFlow<List<Attendance>> = combine(
        selectedDateStr,
        repository.allAttendance
    ) { date, list ->
        list.filter { it.attendance_date == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleAttendance(employeeId: String, isPresent: Boolean) {
        viewModelScope.launch {
            try {
                val date = selectedDateStr.value
                val existingList = attendanceForSelectedDate.value
                val existing = existingList.find { it.employee_id == employeeId }

                val status = if (isPresent) "Present" else "Absent"
                if (existing != null) {
                    val updated = existing.copy(status = status)
                    repository.saveAttendance(updated)
                } else {
                    val employee = repository.allEmployees.value.find { it.id == employeeId }
                    val dailyWage = if (employee != null) employee.monthly_salary / 30.0 else 0.0
                    val newAttendance = Attendance(
                        id = UUID.nameUUIDFromBytes("${employeeId}_$date".toByteArray()).toString(),
                        employee_id = employeeId,
                        attendance_date = date,
                        status = status,
                        daily_wage = dailyWage
                    )
                    repository.saveAttendance(newAttendance)
                }
            } catch (e: Exception) {
                RepositoryProvider.showFeedback("Error toggling attendance: ${e.localizedMessage}")
            }
        }
    }

    private fun getCurrentDateString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
}

// ==========================================
// 4. EXPENSE VIEW MODEL
// ==========================================
class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepositoryProvider.repository

    val expenses: StateFlow<List<Expense>> = repository.allExpenses

    fun addExpense(title: String, amount: Double, status: String, category: String) {
        viewModelScope.launch {
            try {
                val expense = Expense(
                    id = UUID.randomUUID().toString(),
                    description = title,
                    category = category,
                    amount = amount,
                    paid = status == "Settled",
                    status = status,
                    notes = title,
                    expense_date = getCurrentDateTimeString()
                )
                repository.insertExpense(expense)
                RepositoryProvider.showFeedback("Expense '$title' added.")
            } catch (e: Exception) {
                RepositoryProvider.showFeedback("Error: ${e.localizedMessage}")
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            try {
                repository.deleteExpense(expense.id)
                RepositoryProvider.showFeedback("Expense deleted.")
            } catch (e: Exception) {
                RepositoryProvider.showFeedback("Error: ${e.localizedMessage}")
            }
        }
    }
}

// ==========================================
// 5. AI VIEW MODEL
// ==========================================
class AIViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepositoryProvider.repository

    val cfoTimeline = MutableStateFlow("All-Time")
    val cfoStartDate = MutableStateFlow(getDaysAgoTimestamp(30))
    val cfoEndDate = MutableStateFlow(System.currentTimeMillis())

    val cfoReportText = MutableStateFlow<String?>(null)
    val cfoIsGeneratingReport = MutableStateFlow(false)

    val cfoChatMessages = MutableStateFlow<List<CfoChatMessage>>(emptyList())
    val cfoIsGeneratingResponse = MutableStateFlow(false)

    fun getTimelineFinancials(
        logs: List<StockTransaction>,
        exp: List<Expense>,
        employees: List<Employee>
    ): TimelineFinancials {
        val range = getTimelineRange()
        val start = range.first
        val end = range.second

        val filteredLogs = logs.filter { 
            val ts = parseDateTimeToMillis(it.transaction_date)
            ts in start..end 
        }
        val revenue = filteredLogs
            .filter { it.type == "Outbound" }
            .sumOf { it.quantity * it.price_per_kg }

        val rawMaterialCost = filteredLogs
            .filter { it.type == "Inbound" }
            .sumOf { it.quantity * it.price_per_kg }

        val otherExpenses = exp.filter {
            val ts = parseDateTimeToMillis(it.expense_date)
            ts in start..end
        }.sumOf { it.amount }

        val daysInPeriod = ((end - start) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
        val daysForProration = if (cfoTimeline.value == "All-Time") {
            val allTimestamps = logs.map { parseDateTimeToMillis(it.transaction_date) } +
                    exp.map { parseDateTimeToMillis(it.expense_date) }
            if (allTimestamps.isNotEmpty()) {
                val minT = allTimestamps.minOrNull() ?: System.currentTimeMillis()
                val diffDays = ((System.currentTimeMillis() - minT) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
                diffDays.toDouble()
            } else {
                30.0
            }
        } else {
            daysInPeriod.toDouble()
        }

        val totalStaffMonthly = employees.sumOf { it.monthly_salary }
        val proratedStaffOverhead = (totalStaffMonthly / 30.0) * daysForProration

        val totalExpenses = rawMaterialCost + otherExpenses + proratedStaffOverhead
        val netProfit = revenue - totalExpenses

        val totalKgsBought = filteredLogs
            .filter { it.type == "Inbound" }
            .sumOf { it.quantity }

        val totalKgsSold = filteredLogs
            .filter { it.type == "Outbound" }
            .sumOf { it.quantity }

        return TimelineFinancials(
            revenue = revenue,
            rawMaterialCost = rawMaterialCost,
            otherExpenses = otherExpenses,
            proratedStaffOverhead = proratedStaffOverhead,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            days = daysForProration.toInt(),
            totalKgsBought = totalKgsBought,
            totalKgsSold = totalKgsSold
        )
    }

    private fun getTimelineRange(): Pair<Long, Long> {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis

        return when (cfoTimeline.value) {
            "Today" -> {
                val end = System.currentTimeMillis()
                Pair(todayStart, end)
            }
            "Last 7 Days (This Week)" -> {
                val start = getDaysAgoTimestamp(7)
                Pair(start, System.currentTimeMillis())
            }
            "Last 30 Days (This Month)" -> {
                val start = getDaysAgoTimestamp(30)
                Pair(start, System.currentTimeMillis())
            }
            "Custom Range" -> {
                Pair(cfoStartDate.value, cfoEndDate.value)
            }
            else -> { // "All-Time"
                Pair(0L, System.currentTimeMillis())
            }
        }
    }

    fun generateCfoReportNow(
        logs: List<StockTransaction>,
        exp: List<Expense>,
        employees: List<Employee>,
        language: Language
    ) {
        val financials = getTimelineFinancials(logs, exp, employees)
        val rangeStr = when (language) {
            Language.EN -> "${cfoTimeline.value} (Period: ${financials.days} days)"
            Language.TE -> "${Loc.t(cfoTimeline.value, Language.TE)} (సమయం: ${financials.days} రోజులు)"
        }

        val productsStr = repository.allProducts.value.joinToString("\n") { 
            "- ${it.name} (${it.classification}): Current Stock: ${it.current_stock}${it.unit}, Cost: ₹${it.cost_per_kg}/KG" 
        }

        val recentLogsStr = logs.take(15).joinToString("\n") { 
            "- ${it.type} ${it.quantity}KG @ ₹${it.price_per_kg}/KG" 
        }

        val expensesStr = exp.take(10).joinToString("\n") { 
            "- ${it.description} (Category: ${it.category}, Status: ${it.status}): ₹${it.amount}" 
        }

        val prompt = """
            Here is the financial sheet of our Cashew Processing Factory for the timeline: $rangeStr
            
            Financial Summary:
            - Period Revenue: ₹${financials.revenue}
            - Raw Material Cost (Kaju Shell bought): ₹${financials.rawMaterialCost}
            - Staff Salaries (Prorated for ${financials.days} days): ₹${financials.proratedStaffOverhead}
            - Other Expenses (Fuel, utilities, packaging etc.): ₹${financials.otherExpenses}
            - Total Expenses: ₹${financials.totalExpenses}
            - Net Profit/Loss: ₹${financials.netProfit}
            
            Inventory Stocks Breakdown:
            $productsStr
            
            Recent operations (Recent stock logs):
            $recentLogsStr
            
            Expenses Breakdown:
            $expensesStr
            
            Based on this, deliver an expert, rigorous CFO report. 
            Highlight the Net Margin percentage.
            Offer 3 critical actions to optimize processing, minimize inventory cost, and improve profitability.
            Answer in the selected language: ${if (language == Language.EN) "ENGLISH" else "TELUGU (తెలుగు)"}.
            Write beautifully in clean Markdown with professional headers.
        """.trimIndent()

        viewModelScope.launch {
            cfoIsGeneratingReport.value = true
            val systemMsg = Loc.t("cfo_system_prompt", language)
            val result = GeminiClient.generateCFOReport(prompt, systemMsg)
            cfoReportText.value = result
            cfoIsGeneratingReport.value = false
        }
    }

    fun askCfoQuestion(
        question: String,
        logs: List<StockTransaction>,
        exp: List<Expense>,
        employees: List<Employee>,
        language: Language
    ) {
        if (question.isBlank()) return

        val financials = getTimelineFinancials(logs, exp, employees)
        val userMsg = CfoChatMessage(role = "user", message = question)
        cfoChatMessages.value = cfoChatMessages.value + userMsg

        val productsStr = repository.allProducts.value.joinToString("\n") { 
            "- ${it.name} (${it.classification}): ${it.current_stock}${it.unit} in stock, Cost: ₹${it.cost_per_kg}" 
        }

        val context = """
            Factory Financial State:
            - Period Revenue: ₹${financials.revenue}
            - Raw Material cost: ₹${financials.rawMaterialCost}
            - Staff salaries (Prorated): ₹${financials.proratedStaffOverhead}
            - Other expenses: ₹${financials.otherExpenses}
            - Net Profit: ₹${financials.netProfit}
            
            Products list:
            $productsStr
            
            User's Specific Question: $question
            
            Provide a direct, practical, and highly strategic answer. Keep it actionable and structured under 300 words.
            Respond in ${if (language == Language.EN) "English" else "Telugu (తెలుగు)"} to match the user's focus.
        """.trimIndent()

        viewModelScope.launch {
            cfoIsGeneratingResponse.value = true
            val systemMsg = Loc.t("cfo_system_prompt", language)
            val result = GeminiClient.generateCFOReport(context, systemMsg)
            val assistantMsg = CfoChatMessage(role = "assistant", message = result)
            cfoChatMessages.value = cfoChatMessages.value + assistantMsg
            cfoIsGeneratingResponse.value = false
        }
    }

    fun clearChat() {
        cfoChatMessages.value = emptyList()
    }

    private fun getDaysAgoTimestamp(days: Int): Long {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -days)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        return cal.timeInMillis
    }
}

// ==========================================
// 6. SETTINGS VIEW MODEL
// ==========================================
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepositoryProvider.repository

    val language = MutableStateFlow(Language.EN)

    val isLoggedIn = MutableStateFlow(SupabaseService.isLoggedIn)
    val userEmail = MutableStateFlow(SupabaseService.userEmail)
    val authError = MutableStateFlow<String?>(null)
    val isAuthLoading = MutableStateFlow(false)

    val isSyncing = MutableStateFlow(false)
    val syncError = MutableStateFlow<String?>(null)
    val lastSyncTime = MutableStateFlow<Long?>(null)

    init {
        SupabaseService.init(application)
        isLoggedIn.value = SupabaseService.isLoggedIn
        userEmail.value = SupabaseService.userEmail
        syncWithSupabase()
    }

    fun toggleLanguage() {
        language.value = if (language.value == Language.EN) Language.TE else Language.EN
    }

    fun syncWithSupabase() {
        viewModelScope.launch {
            isSyncing.value = true
            syncError.value = null
            try {
                repository.refreshAll()
                lastSyncTime.value = System.currentTimeMillis()
            } catch (e: Exception) {
                syncError.value = e.localizedMessage ?: "Sync failed"
            } finally {
                isSyncing.value = false
            }
        }
    }

    fun signInWithSupabase(emailStr: String, passwordStr: String) {
        viewModelScope.launch {
            isAuthLoading.value = true
            authError.value = null
            try {
                SupabaseService.signIn(emailStr, passwordStr)
                isLoggedIn.value = true
                userEmail.value = SupabaseService.userEmail
                syncWithSupabase()
                RepositoryProvider.showFeedback("Successfully Signed In")
            } catch (e: Exception) {
                authError.value = e.localizedMessage ?: "Sign in failed"
                RepositoryProvider.showFeedback("Sign in failed: ${e.localizedMessage}")
            } finally {
                isAuthLoading.value = false
            }
        }
    }

    fun signUpWithSupabase(emailStr: String, passwordStr: String) {
        viewModelScope.launch {
            isAuthLoading.value = true
            authError.value = null
            try {
                SupabaseService.signUp(emailStr, passwordStr)
                RepositoryProvider.showFeedback("Verification email sent! Please confirm then sign in.")
            } catch (e: Exception) {
                authError.value = e.localizedMessage ?: "Sign up failed"
                RepositoryProvider.showFeedback("Sign up failed: ${e.localizedMessage}")
            } finally {
                isAuthLoading.value = false
            }
        }
    }

    fun signOutFromSupabase() {
        viewModelScope.launch {
            try {
                SupabaseService.signOut()
                isLoggedIn.value = false
                userEmail.value = null
                RepositoryProvider.showFeedback("Successfully Signed Out")
            } catch (e: Exception) {
                RepositoryProvider.showFeedback("Sign out error: ${e.localizedMessage}")
            }
        }
    }
}

// ==========================================
// COORDINATOR VIEW MODEL (REPLACES MONOLITH)
// ==========================================
class FactoryViewModel(application: Application) : AndroidViewModel(application) {
    val inventory = InventoryViewModel(application)
    val employee = EmployeeViewModel(application)
    val attendance = AttendanceViewModel(application)
    val expense = ExpenseViewModel(application)
    val ai = AIViewModel(application)
    val settings = SettingsViewModel(application)

    // Exposed States
    val products = inventory.products
    val contacts = inventory.contacts
    val stockLogs = inventory.stockLogs
    val staff = employee.employees // Aligned with Employee (Staff renamed internally)
    val expenses = expense.expenses
    val allAttendance = attendance.allAttendance
    val selectedDateStr = attendance.selectedDateStr
    val attendanceForSelectedDate = attendance.attendanceForSelectedDate

    val cfoTimeline = ai.cfoTimeline
    val cfoStartDate = ai.cfoStartDate
    val cfoEndDate = ai.cfoEndDate
    val cfoReportText = ai.cfoReportText
    val cfoIsGeneratingReport = ai.cfoIsGeneratingReport
    val cfoChatMessages = ai.cfoChatMessages
    val cfoIsGeneratingResponse = ai.cfoIsGeneratingResponse

    val language = settings.language
    val isLoggedIn = settings.isLoggedIn
    val userEmail = settings.userEmail
    val authError = settings.authError
    val isAuthLoading = settings.isAuthLoading
    val isSyncing = settings.isSyncing
    val syncError = settings.syncError
    val lastSyncTime = settings.lastSyncTime

    val feedbackMessage = RepositoryProvider.feedbackMessage

    // Functions
    fun showFeedback(msg: String) = RepositoryProvider.showFeedback(msg)

    fun toggleLanguage() = settings.toggleLanguage()
    fun syncWithSupabase() = settings.syncWithSupabase()

    fun signInWithSupabase(e: String, p: String) = settings.signInWithSupabase(e, p)
    fun signUpWithSupabase(e: String, p: String) = settings.signUpWithSupabase(e, p)
    fun signOutFromSupabase() = settings.signOutFromSupabase()

    fun addProduct(name: String, classification: String, initialStock: Double, costPrice: Double) =
        inventory.addProduct(name, classification, "KG", initialStock, costPrice, costPrice * 1.3, 100.0)

    fun deleteProduct(p: Product) = inventory.deleteProduct(p)

    fun addContact(n: String, c: String, p: String, l: String) = inventory.addContact(n, c, p, l)
    fun deleteContact(c: Contact) = inventory.deleteContact(c)

    fun logStockMovement(
        productId: String,
        productName: String,
        classification: String,
        type: String,
        quantity: Double,
        pricePerKg: Double,
        contactId: String,
        contactName: String
    ) = inventory.logStockMovement(productId, productName, classification, type, quantity, pricePerKg, contactId, contactName)

    fun deleteStockLog(s: StockTransaction) = inventory.deleteStockLog(s)

    fun addStaff(name: String, role: String, monthlySalary: Double) =
        employee.addEmployee(name, role, monthlySalary)
    fun deleteStaff(member: Employee) = employee.deleteEmployee(member)

    fun toggleAttendance(employeeId: String, isPresent: Boolean) = attendance.toggleAttendance(employeeId, isPresent)

    fun addExpense(title: String, amount: Double, status: String, category: String) = expense.addExpense(title, amount, status, category)
    fun deleteExpense(e: Expense) = expense.deleteExpense(e)

    fun getTimelineFinancials(l: List<StockTransaction>, e: List<Expense>, em: List<Employee>) =
        ai.getTimelineFinancials(l, e, em)

    fun generateCfoReportNow(l: List<StockTransaction>, e: List<Expense>, em: List<Employee>) =
        ai.generateCfoReportNow(l, e, em, language.value)

    fun askCfoQuestion(q: String, l: List<StockTransaction>, e: List<Expense>, em: List<Employee>) =
        ai.askCfoQuestion(q, l, e, em, language.value)

    fun clearChat() = ai.clearChat()
}

// Helpers
data class TimelineFinancials(
    val revenue: Double,
    val rawMaterialCost: Double,
    val otherExpenses: Double,
    val proratedStaffOverhead: Double,
    val totalExpenses: Double,
    val netProfit: Double,
    val days: Int,
    val totalKgsBought: Double = 0.0,
    val totalKgsSold: Double = 0.0
)

data class CfoChatMessage(
    val role: String,
    val message: String
)
