package com.example.somnium;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.Executors;

public class DreamDetailActivity extends AppCompatActivity {

    private static final String TAG = "DreamDetailActivity";
    private int dreamId;
    private AppDatabase db;
    private SleepRecord currentDream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dream_detail);

        db = AppDatabase.getInstance(this);
        dreamId = getIntent().getIntExtra("dream_id", -1);

        if (dreamId == -1) {
            Toast.makeText(this, "Ошибка: неверный ID сна", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        ImageButton deleteButton = findViewById(R.id.deleteBtn);
        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());

        loadDreamDetails();
    }

    private void loadDreamDetails() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                currentDream = db.sleepRecordDao().getById(dreamId);
                runOnUiThread(() -> {
                    if (currentDream != null) {
                        displayDream(currentDream);
                    } else {
                        Toast.makeText(this, "Запись не найдена", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading dream details", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Ошибка загрузки записи", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void displayDream(SleepRecord dream) {
        try {
            TextView title = findViewById(R.id.dreamTitle);
            TextView date = findViewById(R.id.dreamDate);
            TextView content = findViewById(R.id.dreamContent);

            title.setText(dream.title != null ? dream.title : "Без названия");
            date.setText(dream.createdAt != null
                    ? new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(dream.createdAt)
                    : "Дата неизвестна");
            content.setText(dream.notes != null ? dream.notes : "");

        } catch (Exception e) {
            Log.e(TAG, "Error displaying dream", e);
            Toast.makeText(this, "Ошибка отображения записи", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Удаление записи")
                .setMessage("Вы уверены, что хотите удалить эту запись?")
                .setPositiveButton("Удалить", (dialog, which) -> deleteDream())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteDream() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                db.sleepRecordDao().delete(dreamId);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Запись удалена", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK); // Уведомляем вызывающую активность об удалении
                    finish();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error deleting dream", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Ошибка при удалении записи", Toast.LENGTH_SHORT).show());
            }
        });
    }
}