package com.example.somnium;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import androidx.preference.PreferenceManager;

public class MusicManager {
    private static MediaPlayer mediaPlayer;
    private static int currentSoundResId = 0;
    private static final String PREFS_NAME = "AppSettings";
    private static final String SOUND_KEY = "selected_sound";

    public static void startMusic(Context context, int soundResId) {
        if (soundResId == 0) return;

        if (mediaPlayer != null) {
            if (currentSoundResId == soundResId) return;
            stopMusic();
        }

        try {
            mediaPlayer = MediaPlayer.create(context, soundResId);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
                currentSoundResId = soundResId;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stopMusic() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaPlayer = null;
            currentSoundResId = 0;
        }
    }

    public static void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public static void resumeMusic() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public static void updateMusic(Context context, String soundKey) {
        int soundResId = getSoundResource(context, soundKey);
        startMusic(context, soundResId);
    }

    public static void updateMusicFromPrefs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String soundKey = prefs.getString(SOUND_KEY, "sound1");
        updateMusic(context, soundKey);
    }

    private static int getSoundResource(Context context, String soundKey) {
        int resId = 0;
        try {
            resId = context.getResources().getIdentifier(soundKey, "raw", context.getPackageName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resId != 0 ? resId : R.raw.sound1; // fallback to default sound
    }
}