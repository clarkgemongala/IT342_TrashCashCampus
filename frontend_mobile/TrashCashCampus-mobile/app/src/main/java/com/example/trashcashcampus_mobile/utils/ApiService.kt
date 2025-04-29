package com.example.trashcashcampus_mobile.utils

import android.util.Log
import com.example.trashcashcampus_mobile.models.*
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit
import com.google.gson.GsonBuilder

interface ApiService {
    @POST("/api/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("/api/auth/register")
    suspend fun register(@Body registrationRequest: RegistrationRequest): Response<Map<String, Any>>

    @POST("/api/auth/request-password-reset")
    suspend fun requestPasswordReset(@Body email: Map<String, String>): Response<Map<String, Any>>

    @POST("/api/auth/request-email-verification")
    suspend fun requestEmailVerification(@Body email: Map<String, String>): Response<Map<String, Any>>

    @POST("/api/auth/verify-email")
    suspend fun verifyEmail(@Body data: Map<String, String>): Response<Map<String, Any>>

    @POST("/api/auth/verify")
    suspend fun verifyToken(@Header("Authorization") token: String): Response<Boolean>

    @GET("/api/users/{userId}/profile")
    suspend fun getProfile(
        @Path("userId") userId: String,
        @Header("Authorization") authToken: String
    ): Response<ProfileResponse>

    @GET("/api/users/{userId}/profile/email")
    suspend fun getUserEmail(
        @Path("userId") userId: String,
        @Header("Authorization") authToken: String
    ): Response<EmailResponse>

    @PUT("/api/users/{userId}/profile")
    suspend fun updateProfile(
        @Path("userId") userId: String,
        @Header("Authorization") authToken: String,
        @Body profileUpdateRequest: ProfileUpdateRequest
    ): Response<ProfileResponse>

    @PUT("/api/auth/profile/{userId}/password")
    suspend fun updatePassword(
        @Path("userId") userId: String,
        @Header("Authorization") authToken: String,
        @Body passwordUpdateRequest: PasswordUpdateRequest
    ): Response<Map<String, String>>

    @PUT("/api/users/{userId}/profile/email")
    suspend fun updateUserEmail(
        @Path("userId") userId: String,
        @Header("Authorization") authToken: String,
        @Body emailRequest: EmailRequest
    ): Response<EmailResponse>

    // User points and data endpoint
    @GET("/api/users/{userId}/points")
    suspend fun getUserPoints(
        @Path("userId") userId: String
    ): Response<Map<String, Any>>

    // Ping endpoint to check if server is available
    @GET("/api/health")
    suspend fun ping(): Response<Void>

    // Pickup Location endpoints
    @GET("/api/pickup-locations")
    suspend fun getPickupLocations(): Response<PickupLocationResponse>
    
    @GET("/api/pickup-locations/{id}")
    suspend fun getPickupLocationById(@Path("id") id: String): Response<PickupLocationResponse>

    companion object {
        private const val TAG = "ApiService"
        
        // Common URLs for different environments
        // For Android Emulator use 10.0.2.2 (special alias to your host loopback interface)
        // For real device testing on same WiFi network, use your computer's actual IP address
        private const val EMULATOR_URL = "http://10.0.2.2:8080/"
        private const val LOCAL_DEVICE_URL = "http://192.168.0.194:8080/" // Your actual computer's IP
        private const val PRODUCTION_URL = "https://it342-trashcashcampus.onrender.com/" // Production URL
        
        // Change this to switch between environments
        // Using production URL for render hosting
        private const val BASE_URL = PRODUCTION_URL
        
        // Shorter timeout for faster feedback when backend is unavailable
        private const val CONNECTION_TIMEOUT_SECONDS = 15L

        fun create(): ApiService {
            val loggingInterceptor = HttpLoggingInterceptor { message ->
                Log.d(TAG, message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                // Add retry mechanism
                .retryOnConnectionFailure(true)
                // Ensure proper TLS is used
                .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT))
                .build()

            val gson = GsonBuilder()
                .setLenient() // Be lenient with malformed JSON
                .create()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(ApiService::class.java)
        }
        
        // Get current base URL for display or diagnostics
        fun getBaseUrl(): String {
            return BASE_URL
        }
    }
} 