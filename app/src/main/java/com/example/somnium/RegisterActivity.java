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

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private static final String PREFS_NAME = "user_prefs";

    private AppDatabase db;
    private ExecutorService executorService;

    // UI элементы
    private EditText emailEditText, usernameEditText, passwordEditText;
    private TextView emailErrorText, usernameErrorText, passwordErrorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initDatabase();
        initViews();
        setupTextWatchers();
        setupRegisterButton();
        setupBackButton();
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
        emailEditText = findViewById(R.id.email);
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);

        emailErrorText = findViewById(R.id.emailError);
        usernameErrorText = findViewById(R.id.usernameError);
        passwordErrorText = findViewById(R.id.passwordError);
    }

    private void setupBackButton() {
        findViewById(R.id.backButton).setOnClickListener(v -> {
            // Анимация нажатия
            v.animate()
                    .scaleX(0.95f).scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                        navigateToLogin(); // Переход на экран логина
                    })
                    .start();
        });
    }

    private void setupTextWatchers() {
        emailEditText.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateEmail(s.toString().trim());
            }
        });

        usernameEditText.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateUsername(s.toString().trim());
            }
        });

        passwordEditText.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePassword(s.toString());
            }
        });
    }

    private void setupRegisterButton() {
        findViewById(R.id.registerButton).setOnClickListener(v -> {
            v.animate()
                    .scaleX(0.95f).scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                    .start();

            String email = emailEditText.getText().toString().trim();
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (validateAllFields(email, username, password)) {
                attemptRegistration(email, username, password);
            }
        });
    }

    private void validateEmail(String email) {
        if (email.isEmpty()) {
            emailErrorText.setText("Введите email");
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailErrorText.setText("Некорректный email");
        } else {
            emailErrorText.setText("");
            checkEmailExists(email);
        }
    }

    private void validateUsername(String username) {
        if (username.isEmpty()) {
            usernameErrorText.setText("Введите логин");
        } else if (username.length() < 3) {
            usernameErrorText.setText("Минимум 3 символа");
        } else {
            usernameErrorText.setText("");
            checkUsernameExists(username);
        }
    }

    private void validatePassword(String password) {
        if (password.isEmpty()) {
            passwordErrorText.setText("Введите пароль");
        } else if (password.length() < 6) {
            passwordErrorText.setText("Минимум 6 символов");
        } else {
            passwordErrorText.setText("");
        }
    }

    private boolean validateAllFields(String email, String username, String password) {
        validateEmail(email);
        validateUsername(username);
        validatePassword(password);

        return emailErrorText.getText().toString().isEmpty() &&
                usernameErrorText.getText().toString().isEmpty() &&
                passwordErrorText.getText().toString().isEmpty();
    }

    private void checkEmailExists(String email) {
        executorService.execute(() -> {
            boolean exists = db.userDao().getUserByEmail(email) != null;
            runOnUiThread(() -> {
                if (exists) {
                    emailErrorText.setText("Этот email уже зарегистрирован");
                }
            });
        });
    }

    private void checkUsernameExists(String username) {
        executorService.execute(() -> {
            boolean exists = db.userDao().getUserByUsername(username) != null;
            runOnUiThread(() -> {
                if (exists) {
                    usernameErrorText.setText("Этот логин уже занят");
                }
            });
        });
    }

    private void attemptRegistration(String email, String username, String password) {
        executorService.execute(() -> {
            try {
                // Проверка на существование пользователя
                if (db.userDao().getUserByEmail(email) != null ||
                        db.userDao().getUserByUsername(username) != null) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Пользователь уже существует", Toast.LENGTH_SHORT).show());
                    return;
                }

                User newUser = createUser(email, username, password);
                db.userDao().insert(newUser);

                saveUserData(username, email);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                });

            } catch (Exception e) {
                Log.e(TAG, "Registration failed", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Ошибка регистрации", Toast.LENGTH_LONG).show());
            }
        });
    }

    private User createUser(String email, String username, String password) {
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        User newUser = new User(email, username, hashedPassword);
        Log.d(TAG, "User created: " + newUser.getEmail());
        return newUser;
    }

    private void saveUserData(String username, String email) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putString("username", username)
                .putString("email", email)
                .apply();
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    private abstract class TextWatcherAdapter implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void afterTextChanged(Editable s) {}
    }
}