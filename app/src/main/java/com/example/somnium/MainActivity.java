package com.example.somnium;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MusicManager.updateMusicFromPrefs(this);
        Log.d("MainActivity", "Music initialized from prefs");

        displayUsername();
        setupMainButtons();
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MusicManager.updateMusicFromPrefs(this);
        MusicManager.resumeMusic();
        Log.d("MainActivity", "Activity resumed, music updated");
    }

    @Override
    protected void onPause() {
        super.onPause();
        MusicManager.pauseMusic();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MusicManager.stopMusic();
    }

    private void displayUsername() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String username = prefs.getString("username", "Пользователь");
        ((TextView) findViewById(R.id.welcomeText)).setText(String.format("Добро пожаловать, %s!", username));
    }

    private void setupMainButtons() {
        findViewById(R.id.recordSleepButton).setOnClickListener(v ->
                startActivity(new Intent(this, RecordSleepActivity.class)));

        findViewById(R.id.viewDiaryButton).setOnClickListener(v ->
                startActivity(new Intent(this, DreamDiaryActivity.class)));

        findViewById(R.id.analyzeSleepButton).setOnClickListener(v ->
                startActivity(new Intent(this, AnalyzeSleepActivity.class)));
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navHome).setOnClickListener(v -> recreate());
        findViewById(R.id.navAnalyze).setOnClickListener(v -> {
            startActivity(new Intent(this, AnalyzeSleepActivity.class));
            finish();
        });
        findViewById(R.id.navSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
            finish();
        });
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });
    }
}