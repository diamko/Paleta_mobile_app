package ru.diamko.paleta.data.remote.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import ru.diamko.paleta.data.remote.dto.ApiEnvelope
import ru.diamko.paleta.data.remote.dto.CreatePaletteRequestDto
import ru.diamko.paleta.data.remote.dto.PaletteDto
import ru.diamko.paleta.data.remote.dto.PaletteListDataDto
import ru.diamko.paleta.data.remote.dto.RenamePaletteRequestDto

interface PaletteApi {
    @GET("api/mobile/v1/palettes")
    suspend fun getPalettes(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("sort") sort: String = "created_desc",
    ): ApiEnvelope<PaletteListDataDto>

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
}
