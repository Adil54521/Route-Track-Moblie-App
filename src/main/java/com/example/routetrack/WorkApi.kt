package com.example.routetrack

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface WorkApi {
    @FormUrlEncoded
    @POST("request_ftp.php")
    fun checkBranchKey(@Field("branch_key") branchKey: String): Call<BranchDataResponse>
}


