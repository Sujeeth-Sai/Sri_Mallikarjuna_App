package com.example.data

import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@JsonClass(generateAdapter = true)
data class Product(
    val id: String,
    val name: String,
    val classification: String,
    val unit: String,
    val current_stock: Double,
    val cost_per_kg: Double,
    val selling_price_per_kg: Double,
    val reorder_threshold: Double,
    val is_archived: Boolean,
    val created_at: String? = null,
    val updated_at: String? = null
)

@JsonClass(generateAdapter = true)
data class Contact(
    val id: String,
    val name: String,
    val type: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val balance: Double,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class Employee(
    val id: String,
    val name: String,
    val designation: String? = null,
    val monthly_salary: Double,
    val phone: String? = null,
    val joining_date: String? = null,
    val is_active: Boolean,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class Attendance(
    val id: String,
    val employee_id: String,
    val attendance_date: String,
    val status: String,
    val daily_wage: Double,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class StockTransaction(
    val id: String,
    val product_id: String,
    val contact_id: String,
    val type: String,
    val quantity: Double,
    val price_per_kg: Double,
    val total_amount: Double,
    val remarks: String? = null,
    val transaction_date: String,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class Expense(
    val id: String,
    val description: String,
    val category: String,
    val amount: Double,
    val paid: Boolean,
    val status: String,
    val notes: String? = null,
    val expense_date: String,
    val created_at: String? = null
)

// --- Typealiases for strict backward compatibility and seamless refactoring ---
typealias Staff = Employee
typealias StockLog = StockTransaction

// --- Compatibility Extension Properties for UI Layer (MainActivity) ---

val Product.currentStockKg: Double
    get() = current_stock

val Product.costPricePerKg: Double
    get() = cost_per_kg

val Product.initialStockKg: Double
    get() = current_stock

val Product.reorderThresholdKg: Double
    get() = reorder_threshold

val Contact.location: String
    get() = address ?: ""

val Contact.phoneNumber: String
    get() = phone ?: ""

val Contact.classification: String
    get() = type

val StockTransaction.productName: String
    get() {
        val rem = remarks ?: ""
        val parts = rem.split("|")
        val part = parts.find { it.startsWith("productName:") }
        return part?.substringAfter("productName:") ?: "Product"
    }

val StockTransaction.contactName: String
    get() {
        val rem = remarks ?: ""
        val parts = rem.split("|")
        val part = parts.find { it.startsWith("contactName:") }
        return part?.substringAfter("contactName:") ?: "Contact"
    }

val StockTransaction.quantityKg: Double
    get() = quantity

val StockTransaction.pricePerKg: Double
    get() = price_per_kg

val Expense.title: String
    get() = description

val Attendance.staffId: String
    get() = employee_id

val Attendance.isPresent: Boolean
    get() = status == "Present"

val Employee.role: String
    get() = designation ?: ""

val Employee.monthlySalary: Double
    get() = monthly_salary

fun parseDateTimeToMillis(dateStr: String?): Long {
    if (dateStr.isNullOrBlank()) return 0L
    val trimmed = dateStr.trim()
    trimmed.toLongOrNull()?.let { return it }

    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss.SSS",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd"
    )

    for (format in formats) {
        try {
            val sdf = SimpleDateFormat(format, Locale.US)
            if (format.endsWith("Z") || format.contains("XXX")) {
                sdf.timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = sdf.parse(trimmed)
            if (date != null) {
                return date.time
            }
        } catch (e: Exception) {
            // Try other formats
        }
    }
    return 0L
}

fun getCurrentDateTimeString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(java.util.Date())
}
