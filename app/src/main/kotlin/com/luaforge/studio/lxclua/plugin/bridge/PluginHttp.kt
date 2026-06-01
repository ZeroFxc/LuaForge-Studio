package com.luaforge.studio.lxclua.plugin.bridge

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.Executors

/**
 * 网络请求 API
 * 
 * 使用方式: plugin.http.get("url", callback)
 */
class PluginHttp {
    
    private val httpClient = OkHttpClient()
    private val executor = Executors.newCachedThreadPool()
    
    /**
     * HTTP GET 请求
     */
    fun get(url: String, callback: (String?, Exception?) -> Unit) {
        executor.execute {
            try {
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()
                
                val response = httpClient.newCall(request).execute()
                val body = response.body?.string()
                
                if (response.isSuccessful) {
                    callback(body, null)
                } else {
                    callback(null, Exception("HTTP error: ${response.code}"))
                }
            } catch (e: Exception) {
                callback(null, e)
            }
        }
    }
    
    /**
     * HTTP POST 请求
     */
    fun post(url: String, body: String, callback: (String?, Exception?) -> Unit) {
        executor.execute {
            try {
                val request = Request.Builder()
                    .url(url)
                    .post(body.toRequestBody())
                    .build()
                
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful) {
                    callback(responseBody, null)
                } else {
                    callback(null, Exception("HTTP error: ${response.code}"))
                }
            } catch (e: Exception) {
                callback(null, e)
            }
        }
    }
    
    /**
     * HTTP POST JSON 请求
     */
    fun postJson(url: String, json: String, callback: (String?, Exception?) -> Unit) {
        executor.execute {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Content-Type", "application/json")
                    .post(json.toRequestBody())
                    .build()
                
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful) {
                    callback(responseBody, null)
                } else {
                    callback(null, Exception("HTTP error: ${response.code}"))
                }
            } catch (e: Exception) {
                callback(null, e)
            }
        }
    }
    
    /**
     * HTTP PUT 请求
     */
    fun put(url: String, body: String, callback: (String?, Exception?) -> Unit) {
        executor.execute {
            try {
                val request = Request.Builder()
                    .url(url)
                    .put(body.toRequestBody())
                    .build()
                
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful) {
                    callback(responseBody, null)
                } else {
                    callback(null, Exception("HTTP error: ${response.code}"))
                }
            } catch (e: Exception) {
                callback(null, e)
            }
        }
    }
    
    /**
     * HTTP DELETE 请求
     */
    fun delete(url: String, callback: (String?, Exception?) -> Unit) {
        executor.execute {
            try {
                val request = Request.Builder()
                    .url(url)
                    .delete()
                    .build()
                
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful) {
                    callback(responseBody, null)
                } else {
                    callback(null, Exception("HTTP error: ${response.code}"))
                }
            } catch (e: Exception) {
                callback(null, e)
            }
        }
    }
    
    /**
     * 同步 HTTP GET 请求（注意：不要在主线程调用）
     */
    fun getSync(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}