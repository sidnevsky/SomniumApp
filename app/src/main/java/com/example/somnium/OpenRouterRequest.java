package com.example.somnium;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class OpenRouterRequest {
    public static String create(String description) {
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", "Сон: " + description + ". Как можно его интерпретировать? Кратко, тектс должен быть цельным и законченным. До 50 слов.");

        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject request = new JsonObject();
        request.addProperty("model", "mistralai/mistral-7b-instruct");
        request.addProperty("temperature", 0.8);
        request.addProperty("max_tokens", 300);
        request.addProperty("top_p", 0.95);
        request.add("messages", messages);

        return request.toString();
    }
}

