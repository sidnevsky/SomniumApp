package com.example.somnium;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface OpenRouterApi {

    @Headers({
            "Authorization: Bearer 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",
            "Content-Type: application/json"
    })
    @POST("v1/chat/completions")
    Call<ResponseBody> getInterpretation(@Body RequestBody request);

    @Headers({
            "Authorization: Bearer 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",
            "Content-Type: application/json"
    })
    @POST("v1/chat/completions")
    Call<ResponseBody> translateToEnglish(@Body RequestBody request);

    @Headers({
            "Authorization: Bearer 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",
            "Content-Type: application/json"
    })
    @POST("v1/chat/completions")
    Call<ResponseBody> getSentiment(@Body RequestBody request);
}