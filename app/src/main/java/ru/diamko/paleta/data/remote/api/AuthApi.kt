package ru.diamko.paleta.data.remote.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import ru.diamko.paleta.data.remote.dto.ApiEnvelope
import ru.diamko.paleta.data.remote.dto.AuthDataDto
import ru.diamko.paleta.data.remote.dto.ChangePasswordRequestDto
import ru.diamko.paleta.data.remote.dto.ForgotPasswordRequestDto
import ru.diamko.paleta.data.remote.dto.LoginRequestDto
import ru.diamko.paleta.data.remote.dto.RefreshRequestDto
import ru.diamko.paleta.data.remote.dto.RegisterRequestDto
import ru.diamko.paleta.data.remote.dto.ResetPasswordRequestDto
import ru.diamko.paleta.data.remote.dto.UpdateProfileRequestDto
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

    @POST("api/mobile/v1/auth/password/forgot")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequestDto,
    ): ApiEnvelope<Unit>

    @POST("api/mobile/v1/auth/password/reset")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequestDto,
    ): ApiEnvelope<Unit>

    @GET("api/mobile/v1/profile")
    suspend fun profile(): ApiEnvelope<UserDto>

    @PATCH("api/mobile/v1/profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequestDto,
    ): ApiEnvelope<UserDto>

    @POST("api/mobile/v1/profile/password/send-code")
    suspend fun sendProfilePasswordCode(): ApiEnvelope<Unit>

    @POST("api/mobile/v1/profile/password/change")
    suspend fun changeProfilePassword(
        @Body request: ChangePasswordRequestDto,
    ): ApiEnvelope<Unit>
}
