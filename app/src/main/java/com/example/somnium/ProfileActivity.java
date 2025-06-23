package com.example.somnium;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;

    private AppDatabase db;
    private ExecutorService executorService;
    private ImageView avatarImage;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();
        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);

        initViews();
        loadUserData();
    }

    private void initViews() {
        TextView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> returnToMainMenu());

        avatarImage = findViewById(R.id.avatarImage);
        avatarImage.setOnClickListener(v -> openFileChooser());

        Button logoutBtn = findViewById(R.id.logoutButton);
        logoutBtn.setOnClickListener(v -> showLogoutConfirmation());
    }

    private void returnToMainMenu() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        returnToMainMenu();
    }

    private void loadUserData() {
        String username = prefs.getString("username", "Пользователь");
        int userId = prefs.getInt("user_id", -1);

        TextView usernameText = findViewById(R.id.usernameText);
        usernameText.setText(username);

        String avatarPath = getAvatarPath(userId);
        if (avatarPath != null && new File(avatarPath).exists()) {
            Glide.with(this).load(new File(avatarPath)).into(avatarImage);
        } else {
            avatarImage.setImageResource(R.drawable.round);
        }

        if (userId != -1) {
            executorService.execute(() -> {
                int recordsCount = db.sleepRecordDao().getRecordsCount(userId);
                runOnUiThread(() -> {
                    TextView recordsText = findViewById(R.id.recordsCountText);
                    recordsText.setText(String.valueOf(recordsCount));
                });
            });
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Выберите изображение"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            int userId = prefs.getInt("user_id", -1);
            if (userId != -1) {
                try {
                    String avatarPath = saveAvatarToStorage(userId, imageUri);
                    if (avatarPath != null) {
                        Glide.with(this).load(new File(avatarPath)).into(avatarImage);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String saveAvatarToStorage(int userId, Uri imageUri) throws IOException {
        String oldPath = getAvatarPath(userId);
        if (oldPath != null) {
            new File(oldPath).delete();
        }

        File avatarsDir = new File(getFilesDir(), "avatars");
        if (!avatarsDir.exists()) {
            avatarsDir.mkdirs();
        }

        File avatarFile = new File(avatarsDir, "avatar_" + userId + ".jpg");
        InputStream inputStream = getContentResolver().openInputStream(imageUri);
        FileOutputStream outputStream = new FileOutputStream(avatarFile);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }

        outputStream.close();
        inputStream.close();

        return avatarFile.getAbsolutePath();
    }

    private String getAvatarPath(int userId) {
        if (userId == -1) return null;
        File avatarFile = new File(getFilesDir() + "/avatars", "avatar_" + userId + ".jpg");
        return avatarFile.exists() ? avatarFile.getAbsolutePath() : null;
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Выход")
                .setMessage("Вы уверены, что хотите выйти?")
                .setPositiveButton("Да", (dialog, which) -> logout())
                .setNegativeButton("Нет", null)
                .show();
    }

    private void logout() {
        prefs.edit()
                .remove("username")
                .remove("user_id")
                .remove("logged_in")
                .apply();

        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}