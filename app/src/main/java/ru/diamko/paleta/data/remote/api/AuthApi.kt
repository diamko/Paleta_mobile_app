package ru.diamko.paleta.data.remote.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import ru.diamko.paleta.data.remote.dto.ApiEnvelope
import ru.diamko.paleta.data.remote.dto.AuthDataDto
import ru.diamko.paleta.data.remote.dto.LoginRequestDto
import ru.diamko.paleta.data.remote.dto.RefreshRequestDto
import ru.diamko.paleta.data.remote.dto.RegisterRequestDto
import ru.diamko.paleta.data.remote.dto.UserDto

interface AuthApi {
    @POST("api/mobile/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequestDto,
    ): ApiEnvelope<AuthDataDto>

    @POST("api/mobile/v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequestDto,
    ): ApiEnvelope<AuthDataDto>

    @POST("api/mobile/v1/auth/refresh")
    suspend fun refresh(
        @Body request: RefreshRequestDto,
    ): ApiEnvelope<AuthDataDto>

    @POST("api/mobile/v1/auth/logout")
    suspend fun logout(): ApiEnvelope<Unit>

    @GET("api/mobile/v1/auth/me")
    suspend fun me(): ApiEnvelope<UserDto>
}
