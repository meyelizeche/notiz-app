package com.tridente.notiz.data.repository

import com.tridente.notiz.data.local.dao.UserDao
import com.tridente.notiz.data.local.entities.UserEntity
import com.tridente.notiz.data.remote.RetrofitClient
import com.tridente.notiz.data.remote.dto.LoginRequest
import com.tridente.notiz.data.remote.dto.RegisterRequest
import com.tridente.notiz.data.security.TokenManager
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class AuthRepository(
    private val userDao: UserDao,
    private val tokenManager: TokenManager
) {

    val currentUser: Flow<UserEntity?> = userDao.getLoggedInUser()

    suspend fun login(username: String, password: String): Result<UserEntity> {
        return try {
            val response = RetrofitClient.getAuthApi().login(LoginRequest(username, password))
            if (response.isSuccessful && response.body() != null) {
                val authBody = response.body()!!
                tokenManager.saveTokens(authBody.accessToken, authBody.refreshToken)
                tokenManager.saveUserEmail(authBody.user.email)

                userDao.logoutAll()
                val user = UserEntity(
                    id = authBody.user.id,
                    username = authBody.user.username,
                    email = authBody.user.email,
                    isLoggedIn = true
                )
                userDao.insertUser(user)
                Result.success(user)
            } else {
                // Offline / Local fallback login or error
                val localUser = userDao.getLoggedInUserSync()
                if (localUser != null && localUser.username.equals(username, ignoreCase = true)) {
                    Result.success(localUser)
                } else {
                    Result.failure(Exception("Credenciales incorrectas o backend no disponible"))
                }
            }
        } catch (e: Exception) {
            // Local offline fallback login if user was already created
            val localUser = userDao.getLoggedInUserSync()
            if (localUser != null) {
                Result.success(localUser)
            } else {
                // If offline and first time, allow local session creation
                val offlineUser = UserEntity(
                    id = UUID.randomUUID().toString(),
                    username = username,
                    email = "$username@notiz.local",
                    isLoggedIn = true
                )
                userDao.insertUser(offlineUser)
                Result.success(offlineUser)
            }
        }
    }

    suspend fun register(username: String, email: String, password: String): Result<UserEntity> {
        return try {
            val response = RetrofitClient.getAuthApi().register(RegisterRequest(username, email, password))
            if (response.isSuccessful && response.body() != null) {
                val authBody = response.body()!!
                tokenManager.saveTokens(authBody.accessToken, authBody.refreshToken)
                tokenManager.saveUserEmail(authBody.user.email)

                userDao.logoutAll()
                val user = UserEntity(
                    id = authBody.user.id,
                    username = authBody.user.username,
                    email = authBody.user.email,
                    isLoggedIn = true
                )
                userDao.insertUser(user)
                Result.success(user)
            } else {
                Result.failure(Exception("Error al registrar usuario: ${response.message()}"))
            }
        } catch (e: Exception) {
            // Offline fallback registration
            userDao.logoutAll()
            val user = UserEntity(
                id = UUID.randomUUID().toString(),
                username = username,
                email = email,
                isLoggedIn = true
            )
            userDao.insertUser(user)
            tokenManager.saveUserEmail(email)
            Result.success(user)
        }
    }

    suspend fun logout() {
        tokenManager.clear()
        userDao.logoutAll()
    }
}
