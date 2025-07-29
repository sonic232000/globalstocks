package com.thomas.allstocks.network

import com.thomas.allstocks.data.YahooFinanceResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

interface YahooFinanceApi {
    @GET("v8/finance/chart/{symbol}")
    suspend fun getStockData(
        @Path("symbol") symbol: String,
        @Query("period1") period1: Long,
        @Query("period2") period2: Long,
        @Query("interval") interval: String = "1d",
        @Query("includePrePost") includePrePost: Boolean = false,
        @Query("events") events: String = "div,split"
    ): Response<YahooFinanceResponse>
}

object YahooFinanceService {
    private const val BASE_URL = "https://query1.finance.yahoo.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Cache-Control", "no-cache")
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: YahooFinanceApi = retrofit.create(YahooFinanceApi::class.java)
}