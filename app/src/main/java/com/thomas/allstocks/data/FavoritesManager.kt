package com.thomas.allstocks.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

/**
 * 收藏夹管理器，负责收藏股票的持久化存储
 */
class FavoritesManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "stock_favorites", 
        Context.MODE_PRIVATE
    )
    
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    companion object {
        private const val KEY_FAVORITES = "favorite_stocks"
    }
    
    /**
     * 保存收藏股票列表
     */
    fun saveFavorites(favorites: List<FavoriteStock>) {
        val jsonString = gson.toJson(favorites)
        sharedPreferences.edit()
            .putString(KEY_FAVORITES, jsonString)
            .apply()
    }
    
    /**
     * 加载收藏股票列表
     */
    fun loadFavorites(): List<FavoriteStock> {
        val jsonString = sharedPreferences.getString(KEY_FAVORITES, null)
        return if (jsonString != null) {
            try {
                val type = object : TypeToken<List<FavoriteStock>>() {}.type
                gson.fromJson(jsonString, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    /**
     * 添加收藏股票
     */
    fun addFavorite(symbol: String, name: String = symbol): Boolean {
        val favorites = loadFavorites().toMutableList()
        
        // 检查是否已存在
        if (favorites.any { it.symbol.equals(symbol, ignoreCase = true) }) {
            return false // 已存在，不添加
        }
        
        val newFavorite = FavoriteStock(
            symbol = symbol.uppercase(),
            name = name,
            addedAt = dateFormat.format(Date())
        )
        
        favorites.add(newFavorite)
        saveFavorites(favorites)
        return true
    }
    
    /**
     * 删除收藏股票
     */
    fun removeFavorite(symbol: String): Boolean {
        val favorites = loadFavorites().toMutableList()
        val removed = favorites.removeAll { it.symbol.equals(symbol, ignoreCase = true) }
        
        if (removed) {
            saveFavorites(favorites)
        }
        return removed
    }
    
    /**
     * 更新收藏股票名称
     */
    fun updateFavoriteName(symbol: String, name: String): Boolean {
        val favorites = loadFavorites().toMutableList()
        val favorite = favorites.find { it.symbol.equals(symbol, ignoreCase = true) }
        
        return if (favorite != null) {
            val index = favorites.indexOf(favorite)
            favorites[index] = favorite.copy(name = name)
            saveFavorites(favorites)
            true
        } else {
            false
        }
    }
    
    /**
     * 检查股票是否已收藏
     */
    fun isFavorite(symbol: String): Boolean {
        return loadFavorites().any { it.symbol.equals(symbol, ignoreCase = true) }
    }
    
    /**
     * 获取收藏股票数量
     */
    fun getFavoriteCount(): Int {
        return loadFavorites().size
    }
    
    /**
     * 清空所有收藏
     */
    fun clearAllFavorites() {
        sharedPreferences.edit()
            .remove(KEY_FAVORITES)
            .apply()
    }
}