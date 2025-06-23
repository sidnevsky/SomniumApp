package com.example.somnium.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class StabilityResponse {
    @SerializedName("artifacts")
    public List<Artifact> artifacts;

    public static class Artifact {
        @SerializedName("base64")
        public String base64Image;
    }
}