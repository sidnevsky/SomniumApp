package com.example.somnium;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class FilterActivity extends AppCompatActivity {
    private String selectedDateFilter = null;
    private EditText searchEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        try {
            findViewById(R.id.backButton).setOnClickListener(v -> finish());

            searchEditText = findViewById(R.id.searchEditText);

            initDateButtons();

            findViewById(R.id.saveButton).setOnClickListener(v -> {
                applyFilters();
                finish();
            });

            // Кнопка "Сбросить"
            findViewById(R.id.resetButton).setOnClickListener(v -> {
                resetFilters();
                applyFilters();
                finish();
            });

        } catch (Exception e) {
            Log.e("FilterActivity", "Initialization error", e);
            finish();
        }
    }

    private void initDateButtons() {
        int[] dateButtons = {R.id.btnRecent, R.id.btnThisWeek, R.id.btnThisMonth};

        View.OnClickListener dateButtonListener = v -> {
            resetDateButtonsUI();
            v.setBackgroundResource(R.drawable.filter_button_selected);
            selectedDateFilter = getDateFilterKey(v.getId());
        };

        for (int id : dateButtons) {
            Button button = findViewById(id);
            if (button != null) {
                button.setOnClickListener(dateButtonListener);
            }
        }
    }

    private void resetDateButtonsUI() {
        int[] dateButtons = {R.id.btnRecent, R.id.btnThisWeek, R.id.btnThisMonth};
        for (int id : dateButtons) {
            Button button = findViewById(id);
            if (button != null) {
                button.setBackgroundResource(R.drawable.filter_button_unselected);
            }
        }
    }

    private void resetFilters() {
        resetDateButtonsUI();
        selectedDateFilter = null;
        searchEditText.setText(""); // Очищаем поисковое поле
    }

    private String getDateFilterKey(int buttonId) {
        if (buttonId == R.id.btnRecent) {
            return "recent";
        } else if (buttonId == R.id.btnThisWeek) {
            return "week";
        } else if (buttonId == R.id.btnThisMonth) {
            return "month";
        }
        return null;
    }

    private void applyFilters() {
        Intent result = new Intent();

        if (selectedDateFilter != null) {
            result.putExtra("date_filter", selectedDateFilter);
        }

        String searchQuery = searchEditText.getText().toString().trim();
        if (!searchQuery.isEmpty()) {
            result.putExtra("search_query", searchQuery);
        }

        setResult(RESULT_OK, result);
    }
}