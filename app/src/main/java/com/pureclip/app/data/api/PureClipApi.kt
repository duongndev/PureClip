package com.pureclip.app.data.api

import com.pureclip.app.data.model.ParseVideoRequest
import com.pureclip.app.data.model.ParseVideoResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface PureClipApi {
    @POST("api/v1/parse")
    suspend fun parseVideo(@Body request: ParseVideoRequest): Response<ParseVideoResponse>
}
