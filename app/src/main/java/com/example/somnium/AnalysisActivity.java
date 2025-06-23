package com.example.somnium;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.bumptech.glide.Glide;
import com.example.somnium.api.StabilityApi;
import com.example.somnium.api.StabilityResponse;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AnalysisActivity extends AppCompatActivity {

    private TextView dreamTitleTextView;
    private TextView interpretationTextView;
    private TextView sentimentResultTextView;
    private TextView imageResultTextView;
    private ImageView generatedImage;
    private PieChart sentimentChart;
    private TextView backButton;
    private FrameLayout progressOverlay;
    private TextView progressText;

    private static final String TAG = "SomniumLog";
    private String dreamText;

    private static final String STABILITY_API_KEY = "Bearer sk-uar46Dpj0HMqt0t6U7dHcgeDmBik83amIKoc52BkRTSCoznO";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);
        initializeViews();
        processDreamText();
        setupButtonListeners();
    }

    private void initializeViews() {
        dreamTitleTextView = findViewById(R.id.dreamTitleTextView);
        interpretationTextView = findViewById(R.id.interpretationTextView);
        sentimentResultTextView = findViewById(R.id.sentimentResultTextView);
        imageResultTextView = findViewById(R.id.imageResultTextView);
        generatedImage = findViewById(R.id.generatedDreamImage);
        sentimentChart = findViewById(R.id.sentimentChart);
        backButton = findViewById(R.id.backButton);
        progressOverlay = findViewById(R.id.progressOverlay);
        progressText = findViewById(R.id.progressText);
    }

    private void processDreamText() {
        dreamText = getIntent().getStringExtra("dream_text");
        if (dreamText != null) {
            String[] lines = dreamText.split("\n");
            String title = lines.length > 0 ? lines[0].trim() : "Без названия";
            String logText = lines.length > 1 ?
                    String.join("\n", Arrays.copyOfRange(lines, 1, lines.length)) : "";

            Log.d(TAG, "Текст сна (без заголовка):\n" + logText);
            dreamTitleTextView.setText("Анализируем тему: \"" + title + "\"");
        } else {
            dreamTitleTextView.setText("Анализируем тему");
            Log.w(TAG, "Текст сна отсутствует");
        }
    }

    private void setupButtonListeners() {
        findViewById(R.id.btnDreamInterpretation).setOnClickListener(v -> handleInterpretation());
        findViewById(R.id.btnAnalyzeSentiment).setOnClickListener(v -> handleSentimentAnalysis());
        findViewById(R.id.btnGenerateImage).setOnClickListener(v -> handleImageGeneration());

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(AnalysisActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void showProgress(String message) {
        runOnUiThread(() -> {
            String formattedMessage = formatLongMessage(message);
            progressText.setText(formattedMessage);
            progressOverlay.setVisibility(View.VISIBLE);
            progressOverlay.setClickable(true);
            progressOverlay.setFocusable(true);
        });
    }

    private String formatLongMessage(String message) {
        int maxLineLength = 30;

        if (message.length() <= maxLineLength) {
            return message;
        }

        String[] words = message.split(" ");
        StringBuilder result = new StringBuilder();
        int currentLineLength = 0;

        for (String word : words) {
            if (currentLineLength + word.length() > maxLineLength) {
                result.append("\n");
                currentLineLength = 0;
            }
            if (currentLineLength > 0) {
                result.append(" ");
                currentLineLength++;
            }
            result.append(word);
            currentLineLength += word.length();
        }

        return result.toString();
    }

    private void hideProgress() {
        runOnUiThread(() -> {
            progressOverlay.setVisibility(View.GONE);
            progressOverlay.setClickable(false);
            progressOverlay.setFocusable(false);
        });
    }

    private void handleInterpretation() {
        if (isEmpty(dreamText)) return;
        String description = extractDescription(dreamText);
        if (description.isEmpty()) {
            showToast("Описание сна отсутствует");
            return;
        }
        interpretationTextView.setVisibility(View.VISIBLE);
        interpretationTextView.setText("Загрузка толкования...");
        showProgress("Анализируем ваш сон...\nЭто может занять несколько секунд");
        generateDreamInterpretation(description);
    }

    private void generateDreamInterpretation(String description) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://openrouter.ai/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        OpenRouterApi api = retrofit.create(OpenRouterApi.class);
        String requestJson = OpenRouterRequest.create(description);
        RequestBody body = RequestBody.create(
                requestJson,
                MediaType.parse("application/json")
        );

        api.getInterpretation(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                hideProgress();
                interpretationTextView.setVisibility(View.VISIBLE);
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject result = new JSONObject(responseBody);
                        String output = result.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");

                        runOnUiThread(() -> interpretationTextView.setText("Толкование:\n" + output));

                        Log.d(TAG, "Успешно получено толкование сна");

                    } catch (Exception e) {
                        handleInterpretationError("Ошибка обработки ответа: " + e.getMessage());
                    }
                } else {
                    handleInterpretationError(response);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                hideProgress();
                handleInterpretationError("Сетевая ошибка: " + t.getMessage());
            }
        });
    }

    private void handleSentimentAnalysis() {
        if (isEmpty(dreamText)) return;
        String description = extractDescription(dreamText);
        if (description.isEmpty()) {
            showToast("Описание сна отсутствует");
            return;
        }

        sentimentResultTextView.setVisibility(View.VISIBLE);
        sentimentChart.setVisibility(View.GONE);
        sentimentResultTextView.setText("Анализ тональности...");
        showProgress("Определяем эмоциональную\nтональность вашего сна...");

        Log.d(TAG, "Запрос анализа тональности для:\n" + description);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://openrouter.ai/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        OpenRouterApi api = retrofit.create(OpenRouterApi.class);
        String requestJson = OpenRouterSentimentRequest.create(description);

        RequestBody body = RequestBody.create(
                requestJson,
                MediaType.parse("application/json")
        );

        api.getSentiment(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                hideProgress();
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject result = new JSONObject(responseBody);
                        String content = result.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");

                        updateSentimentUI(content.trim());

                    } catch (Exception e) {
                        handleSentimentError("Ошибка обработки ответа: " + e.getMessage());
                    }
                } else {
                    handleSentimentError("Ошибка API: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                hideProgress();
                handleSentimentError("Сетевая ошибка: " + t.getMessage());
            }
        });
    }

    private void updateSentimentUI(String content) {
        try {
            if (content.startsWith("{")) {
                JSONObject data = new JSONObject(content);
                float positive = (float) data.getDouble("positive");
                float negative = (float) data.getDouble("negative");
                float neutral = (float) data.getDouble("neutral");

                buildPieChart(positive, negative, neutral);

            } else {
                float p = 0, n = 0, neu = 0;
                switch (content.toLowerCase()) {
                    case "положительный":
                        p = 100;
                        break;
                    case "отрицательный":
                        n = 100;
                        break;
                    default:
                        neu = 100;
                }

                buildPieChart(p, n, neu);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка разбора JSON: " + e.getMessage());
            runOnUiThread(() -> {
                sentimentResultTextView.setText("Ошибка анализа тональности");
                sentimentChart.clear();
                sentimentChart.setVisibility(View.GONE);
            });
        }
    }

    private void buildPieChart(float positive, float negative, float neutral) throws JSONException {
        ArrayList<PieEntry> entries = new ArrayList<>();
        if (positive > 0) entries.add(new PieEntry(positive, "Положительный"));
        if (negative > 0) entries.add(new PieEntry(negative, "Отрицательный"));
        if (neutral > 0) entries.add(new PieEntry(neutral, "Нейтральный"));

        int positiveColor = Color.parseColor("#4CAF50");
        int negativeColor = Color.parseColor("#F44336");
        int neutralColor = Color.parseColor("#9E9E9E");
        int textColor = Color.parseColor("#FFFFFF");

        int[] colors = new int[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            String label = entries.get(i).getLabel();
            if ("Положительный".equals(label)) {
                colors[i] = positiveColor;
            } else if ("Отрицательный".equals(label)) {
                colors[i] = negativeColor;
            } else {
                colors[i] = neutralColor;
            }
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextColor(textColor);
        dataSet.setValueTextSize(16f);
        dataSet.setValueTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        dataSet.setValueFormatter(new PercentFormatter(sentimentChart));

        PieData pieData = new PieData(dataSet);
        sentimentChart.setData(pieData);

        sentimentChart.setMinimumHeight(400);
        sentimentChart.setMinimumWidth(400);

        sentimentChart.setHoleRadius(0f);
        sentimentChart.setDrawEntryLabels(false);
        sentimentChart.setDrawCenterText(false);

        Legend legend = sentimentChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setTextColor(textColor);
        legend.setTextSize(16f);
        legend.setTypeface(ResourcesCompat.getFont(this, R.font.montserrat_bold));
        legend.setFormSize(12f);
        legend.setFormToTextSpace(8f);
        legend.setXEntrySpace(10f);
        legend.setYEntrySpace(15f);
        legend.setWordWrapEnabled(true);

        sentimentChart.getDescription().setEnabled(false);
        sentimentChart.setExtraOffsets(20f, 20f, 20f, 20f);
        sentimentChart.setEntryLabelColor(textColor);
        sentimentChart.setDrawHoleEnabled(false);
        sentimentChart.setTransparentCircleColor(Color.TRANSPARENT);

        sentimentChart.animateY(1000, Easing.EaseInOutQuad);
        sentimentChart.setVisibility(View.VISIBLE);

        String overall;
        if (positive > negative && positive > neutral) {
            overall = "Положительный";
        } else if (negative > positive && negative > neutral) {
            overall = "Отрицательный";
        } else {
            overall = "Нейтральный";
        }

        sentimentResultTextView.setText("Тональность сна: " + capitalizeFirstLetter(overall));
        sentimentResultTextView.setTextSize(16);
    }

    private String capitalizeFirstLetter(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private void handleSentimentError(String error) {
        Log.e(TAG, error);
        runOnUiThread(() -> {
            sentimentResultTextView.setText("Ошибка анализа тональности");
            sentimentChart.clear();
            sentimentChart.setVisibility(View.GONE);
            showToast(error.length() > 50 ? error.substring(0, 50) + "..." : error);
        });
    }

    private void handleImageGeneration() {
        if (isEmpty(dreamText)) return;
        imageResultTextView.setVisibility(View.VISIBLE);
        generatedImage.setVisibility(View.GONE);
        imageResultTextView.setText("Подготовка текста...");
        showProgress("Генерируем изображение\nпо вашему описанию...\nПожалуйста, подождите");
        String cleanText = prepareBaseText(dreamText);
        translateForImageGeneration(cleanText);
    }

    private String prepareBaseText(String text) {
        String[] lines = text.split("\n");
        StringBuilder cleanText = new StringBuilder();
        int startLine = (lines.length > 1 && lines[0].trim().equalsIgnoreCase("сон")) ? 1 : 0;
        for (int i = startLine; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty()) {
                if (cleanText.length() > 0) cleanText.append(" ");
                cleanText.append(line);
            }
        }
        return cleanText.toString()
                .replace("\"", "")
                .replace("\\", "")
                .replace("/", "")
                .trim();
    }

    private void translateForImageGeneration(String text) {
        if (text.length() > 900) {
            text = text.substring(0, 900);
        }
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://openrouter.ai/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        OpenRouterApi api = retrofit.create(OpenRouterApi.class);
        String requestJson = OpenRouterTranslationRequest.create(text);
        RequestBody body = RequestBody.create(
                requestJson,
                MediaType.parse("application/json")
        );
        api.translateToEnglish(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject result = new JSONObject(responseBody);
                        String translatedText = result.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");
                        String imagePrompt = translatedText + ", digital art, surrealism, vibrant colors, highly detailed";
                        generateImageFromDream(imagePrompt);
                    } catch (Exception e) {
                        hideProgress();
                        handleImageError("Ошибка перевода: " + e.getMessage());
                    }
                } else {
                    hideProgress();
                    handleImageError("Ошибка API: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                hideProgress();
                handleImageError("Сетевая ошибка: " + t.getMessage());
            }
        });
    }

    private void generateImageFromDream(String prompt) {
        Log.d(TAG, "Генерация изображения: " + prompt);
        runOnUiThread(() -> imageResultTextView.setText("Генерация изображения..."));

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.stability.ai/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        StabilityApi api = retrofit.create(StabilityApi.class);

        try {
            JSONObject textPrompt = new JSONObject().put("text", prompt).put("weight", 1.0);
            JSONObject negativePrompt = new JSONObject()
                    .put("text", "blurry, low quality, artifacts, watermark")
                    .put("weight", -0.5);

            JSONArray textPrompts = new JSONArray().put(textPrompt).put(negativePrompt);
            JSONObject requestBody = new JSONObject()
                    .put("text_prompts", textPrompts)
                    .put("cfg_scale", 7)
                    .put("height", 1024)
                    .put("width", 1024)
                    .put("steps", 30)
                    .put("samples", 1);

            RequestBody body = RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json")
            );

            api.generateImage(STABILITY_API_KEY, "application/json", body).enqueue(new Callback<StabilityResponse>() {
                @Override
                public void onResponse(Call<StabilityResponse> call, Response<StabilityResponse> response) {
                    hideProgress();
                    if (response.isSuccessful() && response.body() != null && !response.body().artifacts.isEmpty()) {
                        String base64Image = response.body().artifacts.get(0).base64Image;
                        displayStabilityImage(base64Image);
                    } else {
                        handleImageApiError(response);
                    }
                }

                @Override
                public void onFailure(Call<StabilityResponse> call, Throwable t) {
                    hideProgress();
                    handleImageError("Сетевая ошибка: " + t.getMessage());
                }
            });

        } catch (JSONException e) {
            hideProgress();
            handleImageError("Ошибка формирования запроса: " + e.getMessage());
        }
    }

    private void displayStabilityImage(String base64Image) {
        try {
            byte[] imageBytes = Base64.decode(base64Image, Base64.DEFAULT);
            runOnUiThread(() -> {
                generatedImage.setVisibility(View.VISIBLE);
                Glide.with(this).load(imageBytes).into(generatedImage);
                imageResultTextView.setText("Изображение сгенерировано:");
            });
        } catch (Exception e) {
            handleImageError("Ошибка изображения: " + e.getMessage());
        }
    }

    private void handleImageApiError(Response<StabilityResponse> response) {
        try {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "null";
            handleImageError("Ошибка API (" + response.code() + "): " + errorBody);
        } catch (IOException e) {
            handleImageError("Ошибка чтения ошибки: " + e.getMessage());
        }
    }

    private void handleInterpretationError(Response<ResponseBody> response) {
        try {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "null";
            handleInterpretationError("Ошибка API: " + response.code() + " - " + errorBody);
        } catch (IOException e) {
            handleInterpretationError("Ошибка чтения тела ответа");
        }
    }

    private void handleInterpretationError(String error) {
        Log.e(TAG, error);
        runOnUiThread(() -> {
            interpretationTextView.setText("Ошибка получения толкования");
            showToast(error.length() > 50 ? error.substring(0, 50) + "..." : error);
        });
    }

    private void handleImageError(String error) {
        Log.e(TAG, error);
        runOnUiThread(() -> {
            imageResultTextView.setText("Ошибка генерации");
            showToast(error.length() > 50 ? error.substring(0, 50) + "..." : error);
        });
    }

    private boolean isEmpty(String text) {
        if (text == null || text.trim().isEmpty()) {
            showToast("Текст сна отсутствует");
            return true;
        }
        return false;
    }

    private String extractDescription(String dreamText) {
        String[] lines = dreamText.split("\n");
        String description = lines.length > 1 ? lines[1].trim() : lines[0].trim();
        Log.d(TAG, "Извлечено описание сна:\n" + description);
        return description;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}