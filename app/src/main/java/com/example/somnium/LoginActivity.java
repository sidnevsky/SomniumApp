package com.example.somnium;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.mindrot.jbcrypt.BCrypt;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final String PREFS_NAME = "user_prefs";

    private AppDatabase db;
    private ExecutorService executorService;
    private EditText usernameInput, passwordInput;
    private TextView errorText;
    private View loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        checkAutoLogin();
        initDatabase();
        initViews();
        setupListeners();
    }

    private void checkAutoLogin() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (prefs.getInt("user_id", -1) != -1) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void initDatabase() {
        try {
            db = AppDatabase.getInstance(this);
            executorService = Executors.newSingleThreadExecutor();
        } catch (Exception e) {
            Log.e(TAG, "Database initialization failed", e);
            Toast.makeText(this, "Ошибка инициализации базы данных", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initViews() {
        usernameInput = findViewById(R.id.username);
        passwordInput = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        errorText = findViewById(R.id.errorText);
    }

    private void setupListeners() {
        TextWatcher errorClearWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                hideError();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        usernameInput.addTextChangedListener(errorClearWatcher);
        passwordInput.addTextChangedListener(errorClearWatcher);

        loginButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (validateInput(username, password)) {
                attemptLogin(username, password);
            }
        });

        findViewById(R.id.createAccount).setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private boolean validateInput(String username, String password) {
        if (username.isEmpty()) {
            showError("Введите никнейм");
            usernameInput.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            showError("Введите пароль");
            passwordInput.requestFocus();
            return false;
        }

        return true;
    }

    private void attemptLogin(String username, String password) {
        loginButton.setEnabled(false);
        showLoading(true);

        executorService.execute(() -> {
            try {
                User user = db.userDao().getUserByUsername(username);

                if (user == null) {
                    runOnUiThread(() -> {
                        showError("Аккаунт не найден");
                        resetLoginButton();
                    });
                    return;
                }

                if (!BCrypt.checkpw(password, user.getPasswordHash())) {
                    runOnUiThread(() -> {
                        showError("Неверный пароль");
                        resetLoginButton();
                    });
                    return;
                }

                runOnUiThread(() -> {
                    handleSuccessfulLogin(user);
                    resetLoginButton();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    showError("Ошибка соединения");
                    resetLoginButton();
                });
                Log.e(TAG, "Login error", e);
            }
        });
    }

    private void handleSuccessfulLogin(User user) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putInt("user_id", user.getId())
                .putString("username", user.getUsername())
                .apply();

        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void showLoading(boolean isLoading) {
        runOnUiThread(() -> {
            if (loginButton instanceof TextView) {
                ((TextView) loginButton).setText(isLoading ? "Вход..." : "Войти");
            }
        });
    }

    private void resetLoginButton() {
        runOnUiThread(() -> {
            showLoading(false);
            loginButton.setEnabled(true);
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            errorText.setText(message);
            errorText.setVisibility(View.VISIBLE);
        });
    }

    private void hideError() {
        runOnUiThread(() -> {
            errorText.setVisibility(View.GONE);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}