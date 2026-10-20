package com.tridente.notiz.data.remote

import com.tridente.notiz.data.remote.api.AuthApi
import com.tridente.notiz.data.remote.dto.RefreshTokenRequest
import com.tridente.notiz.data.security.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class AuthInterceptor(
    private val tokenManager: TokenManager,
    private val authApiProvider: () -> AuthApi
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val accessToken = tokenManager.getAccessToken()

        val requestWithToken = if (accessToken != null && !originalRequest.url.encodedPath.contains("/auth/")) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(requestWithToken)

        // Handle Token Refresh on 401 Unauthorized
        if (response.code == 401 && !originalRequest.url.encodedPath.contains("/auth/")) {
            response.close()

            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken != null) {
                synchronized(this) {
                    val currentToken = tokenManager.getAccessToken()
                    val newAccessToken = if (currentToken != accessToken && currentToken != null) {
                        currentToken
                    } else {
                        runBlocking {
                            try {
                                val refreshResponse = authApiProvider().refreshToken(
                                    RefreshTokenRequest(refreshToken)
                                )
                                if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                                    val body = refreshResponse.body()!!
                                    tokenManager.saveTokens(body.accessToken, body.refreshToken)
                                    body.accessToken
                                } else {
                                    tokenManager.clear()
                                    null
                                }
                            } catch (e: Exception) {
                                tokenManager.clear()
                                null
                            }
                        }
                    }

                    if (newAccessToken != null) {
                        val newRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer $newAccessToken")
                            .build()
                        return chain.proceed(newRequest)
                    }
                }
            }
        }

        return response
    }
}
