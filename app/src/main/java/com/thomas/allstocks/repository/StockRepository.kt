package com.thomas.allstocks.repository

import com.thomas.allstocks.data.ProcessedStockData
import com.thomas.allstocks.data.StockInfo
import com.thomas.allstocks.network.YahooFinanceService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StockRepository {
    private val api = YahooFinanceService.api

    suspend fun getStockData(
        symbol: String,
        startTime: Long,
        endTime: Long
    ): Result<List<ProcessedStockData>> = withContext(Dispatchers.IO) {
        try {
            val startTimestamp = startTime / 1000
            val endTimestamp = endTime / 1000
            
            val response = api.getStockData(
                symbol = symbol,
                period1 = startTimestamp,
                period2 = endTimestamp
            )

            if (response.isSuccessful) {
                val data = response.body()
                if (data?.chart?.result?.isNotEmpty() == true) {
                    val result = data.chart.result[0]
                    val timestamps = result.timestamp
                    val indicators = result.indicators.quote[0]

                    val processedData = mutableListOf<ProcessedStockData>()
                    
                    for (i in timestamps.indices) {
                        val timestamp = timestamps[i] * 1000 // 转换为毫秒
                        val open = indicators.open[i]
                        val high = indicators.high[i]
                        val low = indicators.low[i]
                        val close = indicators.close[i]

                        if (open != null && high != null && low != null && close != null &&
                            timestamp >= startTime && timestamp <= endTime) {
                            processedData.add(
                                ProcessedStockData(
                                    time = timestamp,
                                    open = open,
                                    high = high,
                                    low = low,
                                    close = close
                                )
                            )
                        }
                    }

                    Result.success(processedData)
                } else {
                    Result.failure(Exception("未找到 $symbol 的历史数据"))
                }
            } else {
                Result.failure(Exception("API请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStockInfo(symbol: String): Result<StockInfo> = withContext(Dispatchers.IO) {
        try {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - (7 * 24 * 60 * 60 * 1000) // 过去7天
            val startTimestamp = startTime / 1000
            val endTimestamp = endTime / 1000
            
            val response = api.getStockData(
                symbol = symbol,
                period1 = startTimestamp,
                period2 = endTimestamp
            )

            if (response.isSuccessful) {
                val data = response.body()
                if (data?.chart?.result?.isNotEmpty() == true) {
                    val result = data.chart.result[0]
                    val meta = result.meta
                    val timestamps = result.timestamp
                    val indicators = result.indicators.quote[0]

                    // 获取最新价格
                    val lastIndex = timestamps.size - 1
                    val currentPrice = if (lastIndex >= 0) indicators.close[lastIndex] else null

                    val stockInfo = StockInfo(
                        symbol = symbol,
                        companyName = meta.shortName ?: meta.longName ?: symbol,
                        currency = meta.currency ?: "USD",
                        currentPrice = currentPrice ?: meta.regularMarketPrice
                    )

                    Result.success(stockInfo)
                } else {
                    Result.failure(Exception("未找到股票信息"))
                }
            } else {
                Result.failure(Exception("API请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentStockPrice(symbol: String): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val stockInfo = getStockInfo(symbol)
            if (stockInfo.isSuccess) {
                val price = stockInfo.getOrNull()?.currentPrice
                if (price != null) {
                    Result.success(price)
                } else {
                    Result.failure(Exception("无法获取股价"))
                }
            } else {
                Result.failure(stockInfo.exceptionOrNull() ?: Exception("获取股价失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}