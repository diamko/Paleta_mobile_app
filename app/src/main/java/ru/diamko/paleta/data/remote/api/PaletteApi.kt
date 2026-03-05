package ru.diamko.paleta.data.remote.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import ru.diamko.paleta.data.remote.dto.ApiEnvelope
import ru.diamko.paleta.data.remote.dto.CreatePaletteRequestDto
import ru.diamko.paleta.data.remote.dto.ExportPaletteRequestDto
import ru.diamko.paleta.data.remote.dto.LegacyUploadImageResponseDto
import ru.diamko.paleta.data.remote.dto.PaletteDto
import ru.diamko.paleta.data.remote.dto.PaletteListDataDto
import ru.diamko.paleta.data.remote.dto.RecentUploadsDataDto
import ru.diamko.paleta.data.remote.dto.RenamePaletteRequestDto
import ru.diamko.paleta.data.remote.dto.UploadImageDataDto

interface PaletteApi {
    @GET("api/mobile/v1/palettes")
    suspend fun getPalettes(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("sort") sort: String = "created_desc",
    ): ApiEnvelope<PaletteListDataDto>

    @GET("api/mobile/v1/uploads/recent")
    suspend fun getRecentUploads(
        @Query("days") days: Int = 7,
    ): ApiEnvelope<RecentUploadsDataDto>

    @POST("api/mobile/v1/palettes")
    suspend fun createPalette(
        @Body request: CreatePaletteRequestDto,
    ): ApiEnvelope<PaletteDto>

    @PATCH("api/mobile/v1/palettes/{paletteId}")
    suspend fun renamePalette(
        @Path("paletteId") paletteId: Long,
        @Body request: RenamePaletteRequestDto,
    ): ApiEnvelope<PaletteDto>

    @DELETE("api/mobile/v1/palettes/{paletteId}")
    suspend fun deletePalette(
        @Path("paletteId") paletteId: Long,
    ): ApiEnvelope<Unit>

    @Multipart
    @POST("api/mobile/v1/upload")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part,
        @Part("color_count") colorCount: RequestBody,
    ): ApiEnvelope<UploadImageDataDto>

    @Multipart
    @POST("api/upload")
    suspend fun uploadImageLegacy(
        @Part image: MultipartBody.Part,
        @Part("color_count") colorCount: RequestBody,
    ): LegacyUploadImageResponseDto

    @Streaming
    @POST("api/mobile/v1/export")
    suspend fun exportPalette(
        @Query("format") format: String,
        @Body request: ExportPaletteRequestDto,
    ): Response<ResponseBody>
}
