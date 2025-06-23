package com.example.somnium;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppSettings";
    private static final String SOUND_KEY = "selected_sound";
    private String selectedSound = "sound1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        selectedSound = prefs.getString(SOUND_KEY, "sound1");
        Log.d("SettingsActivity", "Loaded sound: " + selectedSound);

        initViews();
        restoreSelectedSound();
    }

    private void initViews() {
        findViewById(R.id.backButton).setOnClickListener(v -> navigateToMain());

        findViewById(R.id.sound1).setOnClickListener(v -> handleSoundSelection(v, "sound1"));
        findViewById(R.id.sound2).setOnClickListener(v -> handleSoundSelection(v, "sound2"));
        findViewById(R.id.sound3).setOnClickListener(v -> handleSoundSelection(v, "sound3"));
        findViewById(R.id.sound4).setOnClickListener(v -> handleSoundSelection(v, "sound4"));

        findViewById(R.id.saveButton).setOnClickListener(v -> {
            saveSettings();
            navigateToMain();
        });
    }

    private void handleSoundSelection(View v, String soundKey) {
        resetAllSoundButtons();
        v.setBackgroundResource(R.drawable.filter_button_selected);
        selectedSound = soundKey;
        Log.d("SettingsActivity", "Selected sound: " + selectedSound);
    }

    private void resetAllSoundButtons() {
        int[] buttonIds = {R.id.sound1, R.id.sound2, R.id.sound3, R.id.sound4};
        for (int id : buttonIds) {
            findViewById(id).setBackgroundResource(R.drawable.filter_button_unselected);
        }
    }

    private void restoreSelectedSound() {
        int[] buttonIds = {R.id.sound1, R.id.sound2, R.id.sound3, R.id.sound4};
        String[] soundKeys = {"sound1", "sound2", "sound3", "sound4"};

        for (int i = 0; i < soundKeys.length; i++) {
            if (soundKeys[i].equals(selectedSound)) {
                findViewById(buttonIds[i]).setBackgroundResource(R.drawable.filter_button_selected);
                break;
            }
        }
    }

    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(SOUND_KEY, selectedSound);
        editor.apply();

        Log.d("SettingsActivity", "Saved sound: " + selectedSound);

        MusicManager.updateMusic(this, selectedSound);
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }
}