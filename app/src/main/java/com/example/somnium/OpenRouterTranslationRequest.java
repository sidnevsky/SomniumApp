package com.example.somnium;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class OpenRouterTranslationRequest {
    public static String create(String russianText) {
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", "Переведи точно на английский для генерации изображения, сохраняя все детали: " + russianText +
                ". Не добавляй пояснений, только перевод.");

        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject request = new JsonObject();
        request.addProperty("model", "meta-llama/llama-3.3-8b-instruct:free");
        request.addProperty("temperature", 0.5);
        request.addProperty("max_tokens", 500);
        request.addProperty("top_p", 0.8);
        request.add("messages", messages);

        return request.toString();
    }
}