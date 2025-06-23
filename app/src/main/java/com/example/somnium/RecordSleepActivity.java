package com.example.somnium;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecordSleepActivity extends AppCompatActivity {
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int SPEECH_REQUEST_CODE = 100;
    private static final String TAG = "RecordSleepActivity";

    private EditText sleepNotes;
    private ImageView micIcon;
    private LinearLayout bottomNavigation;
    private AppDatabase db;
    private ExecutorService executorService;
    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_record);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        currentUserId = prefs.getInt("user_id", -1);

        if (currentUserId == -1) {
            showAuthErrorAndRedirect();
            return;
        }

        // Инициализация базы данных
        db = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();

        initViews();
        setupMicButton();
        setupKeyboardListener();
    }

    private void setupKeyboardListener() {
        final View decorView = getWindow().getDecorView();
        bottomNavigation = findViewById(R.id.bottom_navigation);

        decorView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect rect = new Rect();
            decorView.getWindowVisibleDisplayFrame(rect);
            int screenHeight = decorView.getHeight();
            int keypadHeight = screenHeight - rect.bottom;

            boolean isKeyboardVisible = keypadHeight > screenHeight * 0.15;

            runOnUiThread(() -> {
                if (isKeyboardVisible) {
                    bottomNavigation.animate()
                            .translationY(bottomNavigation.getHeight())
                            .setDuration(100)
                            .start();
                } else {
                    bottomNavigation.animate()
                            .translationY(0)
                            .setDuration(100)
                            .start();
                }
            });
        });
    }

    private void showAuthErrorAndRedirect() {
        Toast.makeText(this, "Требуется авторизация", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void initViews() {
        sleepNotes = findViewById(R.id.sleepNotes);
        micIcon = findViewById(R.id.micIcon);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        TextView backButton = findViewById(R.id.backButton);
        TextView saveButton = findViewById(R.id.saveButton);

        backButton.setOnClickListener(v -> showExitConfirmationDialog());
        saveButton.setOnClickListener(v -> {
            if (currentUserId == -1) {
                showAuthErrorAndRedirect();
            } else {
                showSaveConfirmationDialog();
            }
        });
        setupBottomNavigation();
    }

    private void setupMicButton() {
        micIcon.setOnClickListener(v -> {
            if (currentUserId == -1) {
                showAuthErrorAndRedirect();
            } else if (checkAudioPermission()) {
                startVoiceInput();
            }
        });
    }

    private boolean checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
            return false;
        }
        return true;
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите...");

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка голосового ввода: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Voice input error", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String spokenText = result.get(0);
                String currentText = sleepNotes.getText().toString();
                sleepNotes.setText(currentText + (currentText.isEmpty() ? "" : " ") + spokenText);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceInput();
            } else {
                Toast.makeText(this, "Для использования микрофона нужно разрешение", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showExitConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Подтверждение")
                .setMessage("Вы точно хотите выйти без сохранения?")
                .setPositiveButton("Да", (dialog, which) -> finish())
                .setNegativeButton("Нет", null)
                .show();
    }

    private void showSaveConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Сохранение записи");
        builder.setMessage("Дайте название вашему сну:");

        final EditText input = new EditText(this);
        input.setHint("Например: Странный сон про кота");
        builder.setView(input);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String title = input.getText().toString().trim();
            String notes = sleepNotes.getText().toString().trim();

            if (title.isEmpty()) {
                title = "Сон от " + new SimpleDateFormat("dd.MM", Locale.getDefault()).format(new Date());
            }

            saveSleepRecord(title, notes);
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void saveSleepRecord(String title, String notes) {
        executorService.execute(() -> {
            try {
                SleepRecord record = new SleepRecord(
                        title,
                        notes,
                        new Date(),
                        currentUserId
                );

                db.sleepRecordDao().insert(record);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Сон сохранён", Toast.LENGTH_SHORT).show();
                    navigateToDiary();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error saving sleep record", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Ошибка при сохранении сна", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void navigateToDiary() {
        startActivity(new Intent(this, DreamDiaryActivity.class));
        finish();
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        findViewById(R.id.navAnalyze).setOnClickListener(v -> {
            startActivity(new Intent(this, AnalyzeSleepActivity.class));
            finish();
        });

        findViewById(R.id.navSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}