package com.example.somnium.api;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface StabilityApi {
    @POST("v1/generation/stable-diffusion-xl-1024-v1-0/text-to-image")
    Call<StabilityResponse> generateImage(
            @Header("Authorization") String apiKey,
            @Header("Accept") String accept,
            @Body RequestBody body
    );
}