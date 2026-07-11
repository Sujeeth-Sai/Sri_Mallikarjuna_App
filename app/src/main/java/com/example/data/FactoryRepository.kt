package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FactoryRepository {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val allProducts: StateFlow<List<Product>> = _products.asStateFlow()

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val allContacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _stockTransactions = MutableStateFlow<List<StockTransaction>>(emptyList())
    val allStockTransactions: StateFlow<List<StockTransaction>> = _stockTransactions.asStateFlow()

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val allExpenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val allEmployees: StateFlow<List<Employee>> = _employees.asStateFlow()

    private val _attendance = MutableStateFlow<List<Attendance>>(emptyList())
    val allAttendance: StateFlow<List<Attendance>> = _attendance.asStateFlow()

    // --- Pull/Refresh methods from Supabase ---

    suspend fun refreshProducts() {
        _products.value = SupabaseService.pullProducts()
    }

    suspend fun refreshContacts() {
        _contacts.value = SupabaseService.pullContacts()
    }

    suspend fun refreshStockTransactions() {
        _stockTransactions.value = SupabaseService.pullStockTransactions()
    }

    suspend fun refreshExpenses() {
        _expenses.value = SupabaseService.pullExpenses()
    }

    suspend fun refreshEmployees() {
        _employees.value = SupabaseService.pullEmployees()
    }

    suspend fun refreshAttendance() {
        _attendance.value = SupabaseService.pullAttendance()
    }

    suspend fun refreshAll() {
        refreshProducts()
        refreshContacts()
        refreshStockTransactions()
        refreshExpenses()
        refreshEmployees()
        refreshAttendance()
    }

    // --- Create / Update operations ---

    suspend fun insertProduct(product: Product): String {
        SupabaseService.pushProducts(listOf(product))
        refreshProducts()
        return product.id
    }

    suspend fun deleteProduct(id: String) {
        SupabaseService.deleteProduct(id)
        refreshProducts()
    }

    suspend fun insertContact(contact: Contact): String {
        SupabaseService.pushContacts(listOf(contact))
        refreshContacts()
        return contact.id
    }

    suspend fun deleteContact(id: String) {
        SupabaseService.deleteContact(id)
        refreshContacts()
    }

    suspend fun insertExpense(expense: Expense): String {
        SupabaseService.pushExpenses(listOf(expense))
        refreshExpenses()
        return expense.id
    }

    suspend fun deleteExpense(id: String) {
        SupabaseService.deleteExpense(id)
        refreshExpenses()
    }

    suspend fun insertEmployee(employee: Employee): String {
        SupabaseService.pushEmployees(listOf(employee))
        refreshEmployees()
        return employee.id
    }

    suspend fun deleteEmployee(id: String) {
        SupabaseService.deleteAttendanceForEmployee(id)
        SupabaseService.deleteEmployee(id)
        refreshEmployees()
        refreshAttendance() // If employee is deleted, refresh attendance too
    }

    suspend fun saveAttendance(attendance: Attendance) {
        SupabaseService.pushAttendance(listOf(attendance))
        refreshAttendance()
    }

    suspend fun saveAttendanceList(attendanceList: List<Attendance>) {
        SupabaseService.pushAttendance(attendanceList)
        refreshAttendance()
    }

    suspend fun insertStockTransactionAndUpdateStock(transaction: StockTransaction): Pair<String, Product?> {
        SupabaseService.pushStockTransactions(listOf(transaction))
        
        val product = _products.value.find { it.id == transaction.product_id }
        var updatedProduct: Product? = null
        if (product != null) {
            val delta = transaction.quantity
            val newStock = if (transaction.type == "Inbound") {
                product.current_stock + delta
            } else {
                product.current_stock - delta
            }
            updatedProduct = product.copy(current_stock = newStock)
            SupabaseService.pushProducts(listOf(updatedProduct))
        }
        
        refreshProducts()
        refreshStockTransactions()
        return Pair(transaction.id, updatedProduct)
    }

    suspend fun deleteStockTransactionAndRevertStock(transaction: StockTransaction): Product? {
        SupabaseService.deleteStockTransaction(transaction.id)
        
        val product = _products.value.find { it.id == transaction.product_id }
        var updatedProduct: Product? = null
        if (product != null) {
            val delta = transaction.quantity
            val revertedStock = if (transaction.type == "Inbound") {
                product.current_stock - delta
            } else {
                product.current_stock + delta
            }
            updatedProduct = product.copy(current_stock = revertedStock)
            SupabaseService.pushProducts(listOf(updatedProduct))
        }
        
        refreshProducts()
        refreshStockTransactions()
        return updatedProduct
    }

    // --- Bulk inserts for compatibility if needed ---
    suspend fun insertProductsBulk(products: List<Product>) {
        SupabaseService.pushProducts(products)
        refreshProducts()
    }

    suspend fun insertContactsBulk(contacts: List<Contact>) {
        SupabaseService.pushContacts(contacts)
        refreshContacts()
    }

    suspend fun insertStockTransactionsBulk(transactions: List<StockTransaction>) {
        SupabaseService.pushStockTransactions(transactions)
        refreshStockTransactions()
    }

    suspend fun insertExpensesBulk(expenses: List<Expense>) {
        SupabaseService.pushExpenses(expenses)
        refreshExpenses()
    }

    suspend fun insertEmployeesBulk(employees: List<Employee>) {
        SupabaseService.pushEmployees(employees)
        refreshEmployees()
    }

    suspend fun insertAttendanceBulk(attendanceList: List<Attendance>) {
        SupabaseService.pushAttendance(attendanceList)
        refreshAttendance()
    }
}
