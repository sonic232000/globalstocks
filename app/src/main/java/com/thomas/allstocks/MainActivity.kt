package com.thomas.allstocks

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.thomas.allstocks.data.FavoritesManager
import com.thomas.allstocks.data.ProcessedStockData
import com.thomas.allstocks.data.StockInfo
import com.thomas.allstocks.repository.StockRepository
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var stockRepository: StockRepository
    private lateinit var favoritesManager: FavoritesManager
    private val gson = Gson()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        stockRepository = StockRepository()
        favoritesManager = FavoritesManager(this)

        webView = findViewById(R.id.webView)

        // 配置WebView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }

        // 添加JavaScript接口
        webView.addJavascriptInterface(WebAppInterface(), "AndroidInterface")

        // 设置WebViewClient
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // 页面加载完成后的处理
            }
        }

        // 加载HTML文件
        webView.loadUrl("file:///android_asset/stock_analysis.html")
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    /**
     * JavaScript接口类，提供原生方法给HTML调用
     */
    inner class WebAppInterface {

        @JavascriptInterface
        fun getStockData(symbol: String, startTime: Long, endTime: Long, callback: String) {
            lifecycleScope.launch {
                try {
                    val result = stockRepository.getStockData(symbol, startTime, endTime)

                    if (result.isSuccess) {
                        val stockData = result.getOrNull() ?: emptyList()
                        val jsonData = gson.toJson(stockData)

                        // 在主线程中执行JavaScript回调
                        runOnUiThread {
                            webView.evaluateJavascript("($callback)('$jsonData')") {}
                        }
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "获取股票数据失败"
                        val errorResponse = mapOf("error" to error)
                        val jsonError = gson.toJson(errorResponse)

                        runOnUiThread {
                            webView.evaluateJavascript("($callback)('$jsonError')") {}
                        }
                    }
                } catch (e: Exception) {
                    val errorResponse = mapOf("error" to e.message)
                    val jsonError = gson.toJson(errorResponse)

                    runOnUiThread {
                        webView.evaluateJavascript("($callback)('$jsonError')") {}
                    }
                }
            }
        }

        @JavascriptInterface
        fun getStockInfo(symbol: String, callback: String) {
            lifecycleScope.launch {
                try {
                    val result = stockRepository.getStockInfo(symbol)

                    if (result.isSuccess) {
                        val stockInfo = result.getOrNull()
                        val jsonData = gson.toJson(stockInfo)

                        runOnUiThread {
                            webView.evaluateJavascript("($callback)('$jsonData')") {}
                        }
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "获取股票信息失败"
                        val errorResponse = mapOf("error" to error)
                        val jsonError = gson.toJson(errorResponse)

                        runOnUiThread {
                            webView.evaluateJavascript("($callback)('$jsonError')") {}
                        }
                    }
                } catch (e: Exception) {
                    val errorResponse = mapOf("error" to e.message)
                    val jsonError = gson.toJson(errorResponse)

                    runOnUiThread {
                        webView.evaluateJavascript("($callback)('$jsonError')") {}
                    }
                }
            }
        }

        @JavascriptInterface
        fun getCurrentStockPrice(symbol: String, callback: String) {
            lifecycleScope.launch {
                try {
                    val result = stockRepository.getCurrentStockPrice(symbol)

                    if (result.isSuccess) {
                        val price = result.getOrNull() ?: 0.0

                        runOnUiThread {
                            webView.evaluateJavascript("($callback)('$price')") {}
                        }
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "获取股价失败"

                        runOnUiThread {
                            webView.evaluateJavascript("($callback)('null')") {}
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        webView.evaluateJavascript("($callback)('null')") {}
                    }
                }
            }
        }

        // ========== 收藏夹相关方法 ==========

        /**
         * 保存收藏夹到Android存储
         */
        @JavascriptInterface
        fun saveFavorites(favoritesJson: String, callback: String) {
            try {
                val type = object : com.google.gson.reflect.TypeToken<List<com.thomas.allstocks.data.FavoriteStock>>() {}.type
                val favorites: List<com.thomas.allstocks.data.FavoriteStock> = gson.fromJson(favoritesJson, type)

                favoritesManager.saveFavorites(favorites)

                runOnUiThread {
                    webView.evaluateJavascript("($callback)('success')") {}
                }
            } catch (e: Exception) {
                runOnUiThread {
                    webView.evaluateJavascript("($callback)('error: ${e.message}')") {}
                }
            }
        }

        /**
         * 从Android存储加载收藏夹
         */
        @JavascriptInterface
        fun loadFavorites(callback: String) {
            try {
                val favorites = favoritesManager.loadFavorites()
                val jsonData = gson.toJson(favorites)

                runOnUiThread {
                    webView.evaluateJavascript("($callback)('$jsonData')") {}
                }
            } catch (e: Exception) {
                runOnUiThread {
                    webView.evaluateJavascript("($callback)('[]')") {}
                }
            }
        }

        /**
         * 添加单个收藏股票
         */
        @JavascriptInterface
        fun addFavorite(symbol: String, name: String, callback: String) {
            try {
                val success = favoritesManager.addFavorite(symbol, name)
                val result = if (success) "success" else "already_exists"

                runOnUiThread {
                    webView.evaluateJavascript("($callback)('$result')") {}
                }
            } catch (e: Exception) {
                runOnUiThread {
                    webView.evaluateJavascript("($callback)('error: ${e.message}')") {}
                }
            }
        }

        /**
         * 删除单个收藏股票
         */
        @JavascriptInterface
        fun removeFavorite(symbol: String, callback: String) {
            try {
                val success = favoritesManager.removeFavorite(symbol)
                val result = if (success) "success" else "not_found"

                runOnUiThread {
                    webView.evaluateJavascript("($callback)('$result')") {}
                }
            } catch (e: Exception) {
                runOnUiThread {
                    webView.evaluateJavascript("($callback)('error: ${e.message}')") {}
                }
            }
        }

        /**
         * 更新收藏股票名称
         */
        @JavascriptInterface
        fun updateFavoriteName(symbol: String, name: String, callback: String) {
            try {
                val success = favoritesManager.updateFavoriteName(symbol, name)
                val result = if (success) "success" else "not_found"

                runOnUiThread {
                    webView.evaluateJavascript("($callback)('$result')") {}
                }
            } catch (e: Exception) {
                runOnUiThread {
                    webView.evaluateJavascript("($callback)('error: ${e.message}')") {}
                }
            }
        }

        /**
         * 检查股票是否已收藏
         */
        @JavascriptInterface
        fun isFavorite(symbol: String, callback: String) {
            try {
                val isFav = favoritesManager.isFavorite(symbol)

                runOnUiThread {
                    webView.evaluateJavascript("($callback)('$isFav')") {}
                }
            } catch (e: Exception) {
                runOnUiThread {
                    webView.evaluateJavascript("($callback)('false')") {}
                }
            }
        }

        @JavascriptInterface
        fun log(message: String) {
            android.util.Log.d("WebView", message)
        }
    }
}