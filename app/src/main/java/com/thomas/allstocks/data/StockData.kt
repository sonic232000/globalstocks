package com.thomas.allstocks.data

import com.google.gson.annotations.SerializedName

// Yahoo Finance API 响应数据模型
data class YahooFinanceResponse(
    @SerializedName("chart")
    val chart: Chart
)

data class Chart(
    @SerializedName("result")
    val result: List<ChartResult>?,
    @SerializedName("error")
    val error: String?
)

data class ChartResult(
    @SerializedName("meta")
    val meta: Meta,
    @SerializedName("timestamp")
    val timestamp: List<Long>,
    @SerializedName("indicators")
    val indicators: Indicators
)

data class Meta(
    @SerializedName("currency")
    val currency: String?,
    @SerializedName("symbol")
    val symbol: String,
    @SerializedName("longName")
    val longName: String?,
    @SerializedName("shortName")
    val shortName: String?,
    @SerializedName("regularMarketPrice")
    val regularMarketPrice: Double?
)

data class Indicators(
    @SerializedName("quote")
    val quote: List<Quote>
)

data class Quote(
    @SerializedName("open")
    val open: List<Double?>,
    @SerializedName("high")
    val high: List<Double?>,
    @SerializedName("low")
    val low: List<Double?>,
    @SerializedName("close")
    val close: List<Double?>
)

// 处理后的股票数据
data class ProcessedStockData(
    val time: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double
)

// 股票信息
data class StockInfo(
    val symbol: String,
    val companyName: String,
    val currency: String,
    val currentPrice: Double?
)