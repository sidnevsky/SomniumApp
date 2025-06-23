package com.example.somnium;

public class OpenRouterSentimentRequest {
    public static String create(String description) {
        return "{\n" +
                "  \"model\": \"mistralai/mistral-7b-instruct\",\n" +
                "  \"messages\": [\n" +
                "    {\n" +
                "      \"role\": \"system\",\n" +
                "      \"content\": \"Ты — помощник, который анализирует эмоциональную окраску текста. В ответе дай JSON с тремя полями: positive, negative, neutral. Эти значения показывают процентное распределение эмоций в тексте. Сумма должна быть равна 100. Пример: { \\\"positive\\\": 30, \\\"negative\\\": 50, \\\"neutral\\\": 20 }\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"role\": \"user\",\n" +
                "      \"content\": \"Определи эмоциональную окраску следующего описания сна: \\\"" + description + "\\\"\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }
}