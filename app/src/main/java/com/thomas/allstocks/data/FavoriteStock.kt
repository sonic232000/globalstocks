package com.thomas.allstocks.data

import com.google.gson.annotations.SerializedName

/**
 * 收藏股票数据模型
 */
data class FavoriteStock(
    @SerializedName("symbol")
    val symbol: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("addedAt")
    val addedAt: String
)