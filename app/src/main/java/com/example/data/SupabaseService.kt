package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class AuthRequest(val email: String, val password: String)
data class AuthUser(val id: String, val email: String)
data class AuthResponse(
    val access_token: String?,
    val refresh_token: String?,
    val expires_in: Long?,
    val user: AuthUser?
)

object SupabaseService {
    private const val TAG = "SupabaseService"

    private var sharedPrefs: android.content.SharedPreferences? = null

    var accessToken: String? = null
        private set

    var userEmail: String? = null
        private set

    val isLoggedIn: Boolean
        get() = accessToken != null

    fun init(context: android.content.Context) {
        sharedPrefs = context.applicationContext.getSharedPreferences("supabase_auth_prefs", android.content.Context.MODE_PRIVATE)
        accessToken = sharedPrefs?.getString("access_token", null)
        userEmail = sharedPrefs?.getString("user_email", null)
        Log.d(TAG, "Initialized Supabase Auth. LoggedIn: $isLoggedIn | Email: $userEmail")
    }

    private const val DEFAULT_SUPABASE_URL = "https://dghnymvkqkhfjbwjmcok.supabase.co"
    private const val DEFAULT_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRnaG55bXZrcWtoZmpid2ptY29rIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODM1MTI2MzQsImV4cCI6MjA5OTA4ODYzNH0.8JMpz1SRqUnY9WSv6jP5an3kB-YS_KXa6Ig1PuscMEQ"

    val supabaseUrl: String
        get() {
            return try {
                val configUrl = BuildConfig.SUPABASE_URL
                if (configUrl.isNotEmpty() && configUrl != "SUPABASE_URL_PLACEHOLDER") configUrl else DEFAULT_SUPABASE_URL
            } catch (e: Exception) {
                DEFAULT_SUPABASE_URL
            }
        }

    val supabaseAnonKey: String
        get() {
            return try {
                val configKey = BuildConfig.SUPABASE_ANON_KEY
                if (configKey.isNotEmpty() && configKey != "SUPABASE_ANON_KEY_PLACEHOLDER") configKey else DEFAULT_ANON_KEY
            } catch (e: Exception) {
                DEFAULT_ANON_KEY
            }
        }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // --- CRUD Operations using direct models ---

    suspend fun pushProducts(products: List<Product>) {
        if (products.isEmpty()) return
        val listType = Types.newParameterizedType(List::class.java, Product::class.java)
        val json = moshi.adapter<List<Product>>(listType).toJson(products)
        upsertTable("products", json)
    }

    suspend fun pushContacts(contacts: List<Contact>) {
        if (contacts.isEmpty()) return
        val listType = Types.newParameterizedType(List::class.java, Contact::class.java)
        val json = moshi.adapter<List<Contact>>(listType).toJson(contacts)
        upsertTable("contacts", json)
    }

    suspend fun pushStockTransactions(transactions: List<StockTransaction>) {
        if (transactions.isEmpty()) return
        val listType = Types.newParameterizedType(List::class.java, StockTransaction::class.java)
        val json = moshi.adapter<List<StockTransaction>>(listType).toJson(transactions)
        upsertTable("stock_transactions", json)
    }

    suspend fun pushExpenses(expenses: List<Expense>) {
        if (expenses.isEmpty()) return
        val listType = Types.newParameterizedType(List::class.java, Expense::class.java)
        val json = moshi.adapter<List<Expense>>(listType).toJson(expenses)
        upsertTable("expenses", json)
    }

    suspend fun pushEmployees(employees: List<Employee>) {
        if (employees.isEmpty()) return
        val listType = Types.newParameterizedType(List::class.java, Employee::class.java)
        val json = moshi.adapter<List<Employee>>(listType).toJson(employees)
        upsertTable("employees", json)
    }

    suspend fun pushAttendance(attendanceList: List<Attendance>) {
        if (attendanceList.isEmpty()) return
        val listType = Types.newParameterizedType(List::class.java, Attendance::class.java)
        val json = moshi.adapter<List<Attendance>>(listType).toJson(attendanceList)
        upsertTable("attendance", json)
    }

    suspend fun pullProducts(): List<Product> {
        val json = fetchTable("products")
        return try {
            val listType = Types.newParameterizedType(List::class.java, Product::class.java)
            moshi.adapter<List<Product>>(listType).fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing products", e)
            throw Exception("Failed to parse products table: ${e.localizedMessage}")
        }
    }

    suspend fun pullContacts(): List<Contact> {
        val json = fetchTable("contacts")
        return try {
            val listType = Types.newParameterizedType(List::class.java, Contact::class.java)
            moshi.adapter<List<Contact>>(listType).fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing contacts", e)
            throw Exception("Failed to parse contacts table: ${e.localizedMessage}")
        }
    }

    suspend fun pullStockTransactions(): List<StockTransaction> {
        val json = fetchTable("stock_transactions")
        return try {
            val listType = Types.newParameterizedType(List::class.java, StockTransaction::class.java)
            moshi.adapter<List<StockTransaction>>(listType).fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing stock transactions", e)
            throw Exception("Failed to parse stock_transactions table: ${e.localizedMessage}")
        }
    }

    suspend fun pullExpenses(): List<Expense> {
        val json = fetchTable("expenses")
        return try {
            val listType = Types.newParameterizedType(List::class.java, Expense::class.java)
            moshi.adapter<List<Expense>>(listType).fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing expenses", e)
            throw Exception("Failed to parse expenses table: ${e.localizedMessage}")
        }
    }

    suspend fun pullEmployees(): List<Employee> {
        val json = fetchTable("employees")
        return try {
            val listType = Types.newParameterizedType(List::class.java, Employee::class.java)
            moshi.adapter<List<Employee>>(listType).fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing employees", e)
            throw Exception("Failed to parse employees table: ${e.localizedMessage}")
        }
    }

    suspend fun pullAttendance(): List<Attendance> {
        val json = fetchTable("attendance")
        return try {
            val listType = Types.newParameterizedType(List::class.java, Attendance::class.java)
            moshi.adapter<List<Attendance>>(listType).fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing attendance", e)
            throw Exception("Failed to parse attendance table: ${e.localizedMessage}")
        }
    }

    suspend fun deleteProduct(id: String) {
        deleteRow("products", id)
    }

    suspend fun deleteContact(id: String) {
        deleteRow("contacts", id)
    }

    suspend fun deleteStockTransaction(id: String) {
        deleteRow("stock_transactions", id)
    }

    suspend fun deleteExpense(id: String) {
        deleteRow("expenses", id)
    }

    suspend fun deleteEmployee(id: String) {
        deleteRow("employees", id)
    }

    suspend fun deleteAttendanceForEmployee(employeeId: String) = withContext(Dispatchers.IO) {
        val url = "$supabaseUrl/rest/v1/attendance?employee_id=eq.$employeeId"
        val authHeader = if (!accessToken.isNullOrEmpty()) "Bearer $accessToken" else "Bearer $supabaseAnonKey"

        val request = Request.Builder()
            .url(url)
            .delete()
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", authHeader)
            .build()

        Log.d(TAG, "==> [SUPABASE REQUEST] DELETE: $url")
        try {
            client.newCall(request).execute().use { response ->
                val responseBodyStr = response.body?.string() ?: ""
                Log.d(TAG, "<== [SUPABASE RESPONSE] DELETE Status: ${response.code} | Body: $responseBodyStr")
                if (!response.isSuccessful) {
                    Log.e(TAG, "!!! [SUPABASE ERROR] DELETE failed for table 'attendance' employee_id '$employeeId': Code ${response.code}, Msg: $responseBodyStr")
                    throw Exception("HTTP ${response.code} during delete from 'attendance': $responseBodyStr")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "!!! [SUPABASE ERROR] Exception during DELETE from 'attendance' employee_id '$employeeId'", e)
            throw e
        }
    }

    private suspend fun deleteRow(tableName: String, idValue: String) = withContext(Dispatchers.IO) {
        val url = "$supabaseUrl/rest/v1/$tableName?id=eq.$idValue"
        val authHeader = if (!accessToken.isNullOrEmpty()) "Bearer $accessToken" else "Bearer $supabaseAnonKey"

        val request = Request.Builder()
            .url(url)
            .delete()
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", authHeader)
            .build()

        Log.d(TAG, "==> [SUPABASE REQUEST] DELETE: $url")
        try {
            client.newCall(request).execute().use { response ->
                val responseBodyStr = response.body?.string() ?: ""
                Log.d(TAG, "<== [SUPABASE RESPONSE] DELETE Status: ${response.code} | Body: $responseBodyStr")
                if (!response.isSuccessful) {
                    Log.e(TAG, "!!! [SUPABASE ERROR] DELETE failed for table '$tableName' id '$idValue': Code ${response.code}, Msg: $responseBodyStr")
                    throw Exception("HTTP ${response.code} during delete from '$tableName': $responseBodyStr")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "!!! [SUPABASE ERROR] Exception during DELETE from '$tableName' id '$idValue'", e)
            throw e
        }
    }

    private suspend fun upsertTable(tableName: String, jsonBody: String) = withContext(Dispatchers.IO) {
        val url = "$supabaseUrl/rest/v1/$tableName"
        val body = jsonBody.toRequestBody(jsonMediaType)
        val authHeader = if (!accessToken.isNullOrEmpty()) "Bearer $accessToken" else "Bearer $supabaseAnonKey"

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", authHeader)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates")
            .build()

        Log.d(TAG, "==> [SUPABASE REQUEST] POST: $url\nPayload: $jsonBody")
        try {
            client.newCall(request).execute().use { response ->
                val responseBodyStr = response.body?.string() ?: ""
                Log.d(TAG, "<== [SUPABASE RESPONSE] POST Status: ${response.code} | Body: $responseBodyStr")
                if (!response.isSuccessful) {
                    Log.e(TAG, "!!! [SUPABASE ERROR] POST failed for table '$tableName': Code ${response.code}, Msg: $responseBodyStr")
                    throw Exception("HTTP ${response.code} during push to '$tableName': $responseBodyStr")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "!!! [SUPABASE ERROR] Exception during POST to '$tableName'", e)
            throw e
        }
    }

    private suspend fun fetchTable(tableName: String): String = withContext(Dispatchers.IO) {
        val url = "$supabaseUrl/rest/v1/$tableName?select=*"
        val authHeader = if (!accessToken.isNullOrEmpty()) "Bearer $accessToken" else "Bearer $supabaseAnonKey"

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", authHeader)
            .build()

        Log.d(TAG, "==> [SUPABASE REQUEST] GET: $url")
        try {
            client.newCall(request).execute().use { response ->
                val responseBodyStr = response.body?.string() ?: "[]"
                Log.d(TAG, "<== [SUPABASE RESPONSE] GET Status: ${response.code} | Body: $responseBodyStr")
                if (!response.isSuccessful) {
                    Log.e(TAG, "!!! [SUPABASE ERROR] GET failed for table '$tableName': Code ${response.code}, Msg: $responseBodyStr")
                    throw Exception("HTTP ${response.code} during pull from '$tableName': $responseBodyStr")
                } else {
                    responseBodyStr
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "!!! [SUPABASE ERROR] Exception during GET from '$tableName'", e)
            throw e
        }
    }

    suspend fun signUp(email: String, password: String): AuthResponse {
        val url = "$supabaseUrl/auth/v1/signup"
        val requestBody = moshi.adapter(AuthRequest::class.java).toJson(AuthRequest(email, password))
        val body = requestBody.toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("apikey", supabaseAnonKey)
            .build()

        Log.d(TAG, "==> [SUPABASE AUTH REQUEST] SIGNUP: $url")
        return executeAuthRequest(request)
    }

    suspend fun signIn(email: String, password: String): AuthResponse {
        val url = "$supabaseUrl/auth/v1/token?grant_type=password"
        val requestBody = moshi.adapter(AuthRequest::class.java).toJson(AuthRequest(email, password))
        val body = requestBody.toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("apikey", supabaseAnonKey)
            .build()

        Log.d(TAG, "==> [SUPABASE AUTH REQUEST] SIGNIN: $url")
        val response = executeAuthRequest(request)
        response.access_token?.let { token ->
            accessToken = token
            userEmail = response.user?.email ?: email
            sharedPrefs?.edit()?.apply {
                putString("access_token", token)
                putString("user_email", userEmail)
                apply()
            }
        }
        return response
    }

    fun signOut() {
        val token = accessToken
        if (!token.isNullOrEmpty()) {
            val url = "$supabaseUrl/auth/v1/logout"
            val request = Request.Builder()
                .url(url)
                .post("{}".toRequestBody(jsonMediaType))
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $token")
                .build()
            
            kotlin.concurrent.thread {
                try {
                    client.newCall(request).execute().close()
                } catch (e: Exception) {
                    Log.w(TAG, "Logout request failed: ${e.localizedMessage}")
                }
            }
        }

        accessToken = null
        userEmail = null
        sharedPrefs?.edit()?.apply {
            remove("access_token")
            remove("user_email")
            apply()
        }
    }

    private suspend fun executeAuthRequest(request: Request): AuthResponse = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            val responseBodyStr = response.body?.string() ?: ""
            Log.d(TAG, "<== [SUPABASE AUTH RESPONSE] Status: ${response.code} | Body: $responseBodyStr")
            if (!response.isSuccessful) {
                val errorMsg = try {
                    val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
                    val errorMap = moshi.adapter<Map<String, Any>>(mapType).fromJson(responseBodyStr)
                    errorMap?.get("error_description") as? String 
                        ?: errorMap?.get("msg") as? String 
                        ?: errorMap?.get("message") as? String 
                        ?: "Authentication failed"
                } catch (e: Exception) {
                    "HTTP ${response.code}: $responseBodyStr"
                }
                throw Exception(errorMsg)
            }
            moshi.adapter(AuthResponse::class.java).fromJson(responseBodyStr)
                ?: throw Exception("Invalid response from auth server")
        }
    }
}
