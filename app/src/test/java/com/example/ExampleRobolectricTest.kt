package com.example

import com.example.data.*
import com.example.ui.Language
import com.example.ui.Loc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun testProductExtensions() {
        val product = Product(
            id = UUID.randomUUID().toString(),
            name = "Premium Cashew W180",
            classification = "Finished Good",
            unit = "KG",
            current_stock = 450.5,
            cost_per_kg = 650.0,
            selling_price_per_kg = 850.0,
            reorder_threshold = 100.0,
            is_archived = false
        )

        assertEquals(450.5, product.currentStockKg, 0.001)
        assertEquals(650.0, product.costPricePerKg, 0.001)
        assertEquals(450.5, product.initialStockKg, 0.001)
        assertEquals(100.0, product.reorderThresholdKg, 0.001)
    }

    @Test
    fun testContactExtensions() {
        val contact = Contact(
            id = UUID.randomUUID().toString(),
            name = "Srinivasa Traders",
            type = "Supplier",
            phone = "+919876543210",
            email = "srini@cashew.com",
            address = "Palasa, Andhra Pradesh",
            balance = -15000.0
        )

        assertEquals("Palasa, Andhra Pradesh", contact.location)
        assertEquals("+919876543210", contact.phoneNumber)
        assertEquals("Supplier", contact.classification)
    }

    @Test
    fun testStockTransactionExtensions() {
        val transaction = StockTransaction(
            id = UUID.randomUUID().toString(),
            product_id = "p-123",
            contact_id = "c-456",
            type = "Inbound",
            quantity = 1200.0,
            price_per_kg = 110.0,
            total_amount = 132000.0,
            remarks = "productName:Raw Cashew Nuts|contactName:Srinivasa Traders|classification:Raw Material",
            transaction_date = System.currentTimeMillis().toString()
        )

        assertEquals("Raw Cashew Nuts", transaction.productName)
        assertEquals("Srinivasa Traders", transaction.contactName)
        assertEquals(1200.0, transaction.quantityKg, 0.001)
        assertEquals(110.0, transaction.pricePerKg, 0.001)
    }

    @Test
    fun testExpenseExtensions() {
        val expense = Expense(
            id = UUID.randomUUID().toString(),
            description = "Electricity Bill June",
            category = "Utilities",
            amount = 4500.0,
            paid = true,
            status = "Settled",
            notes = "Paid via UPI",
            expense_date = System.currentTimeMillis().toString()
        )

        assertEquals("Electricity Bill June", expense.title)
    }

    @Test
    fun testEmployeeAndAttendanceExtensions() {
        val employee = Employee(
            id = "e-999",
            name = "Ramesh Kumar",
            designation = "Peeling Operator",
            monthly_salary = 15000.0,
            phone = "+919988776655",
            joining_date = "2026-01-15",
            is_active = true
        )

        assertEquals("Peeling Operator", employee.role)
        assertEquals(15000.0, employee.monthlySalary, 0.001)

        val attendancePresent = Attendance(
            id = UUID.randomUUID().toString(),
            employee_id = "e-999",
            attendance_date = "2026-07-09",
            status = "Present",
            daily_wage = 500.0
        )

        val attendanceAbsent = Attendance(
            id = UUID.randomUUID().toString(),
            employee_id = "e-999",
            attendance_date = "2026-07-09",
            status = "Absent",
            daily_wage = 500.0
        )

        assertEquals("e-999", attendancePresent.staffId)
        assertTrue(attendancePresent.isPresent)
        assertFalse(attendanceAbsent.isPresent)
    }

    @Test
    fun testLocalizationTrans() {
        assertEquals("Sri Mallikarjuna Industries", Loc.t("app_title", Language.EN))
        assertEquals("శ్రీ మల్లికార్జున ఇండస్ట్రీస్", Loc.t("app_title", Language.TE))

        assertEquals("AI CFO Advisor", Loc.t("ai_cfo", Language.EN))
        assertEquals("AI CFO సలహాదారు", Loc.t("ai_cfo", Language.TE))

        assertEquals("NonExistentKey", Loc.t("NonExistentKey", Language.EN))
    }
}
