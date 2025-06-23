package com.example.somnium;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DreamDiaryActivity extends AppCompatActivity
        implements DreamAdapter.OnDreamClickListener {

    private static final String TAG = "DreamDiaryActivity";
    private static final int FILTER_REQUEST_CODE = 1001;
    private static final String PREFS_NAME = "DreamDiaryPrefs";
    private static final String KEY_CURRENT_FILTER = "current_filter";
    private static final String KEY_CURRENT_SEARCH = "current_search";

    // UI Components
    private RecyclerView dreamsRecyclerView;
    private DreamAdapter adapter;
    private TextView filterIndicator;
    private ImageButton btnClearFilter;
    private TextView emptyStateText;

    // Database
    private AppDatabase db;
    private ExecutorService executorService;

    // Data
    private int currentUserId;
    private boolean isSelectMode;
    private String currentFilter;
    private String currentSearchQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dream_diary);

        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            Log.e(TAG, "Uncaught exception", ex);
            runOnUiThread(() ->
                    Toast.makeText(this, "Произошла ошибка", Toast.LENGTH_LONG).show());
        });

        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            currentFilter = prefs.getString(KEY_CURRENT_FILTER, null);
            currentSearchQuery = prefs.getString(KEY_CURRENT_SEARCH, null);

            isSelectMode = getIntent().getBooleanExtra("select_mode", false);
            checkAuthorization();

            db = AppDatabase.getInstance(this);
            executorService = Executors.newSingleThreadExecutor();

            initViews();
            loadDreams();

        } catch (Exception e) {
            Log.e(TAG, "Initialization failed", e);
            Toast.makeText(this, "Ошибка инициализации", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void checkAuthorization() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        currentUserId = prefs.getInt("user_id", -1);

        if (currentUserId == -1) {
            showAuthErrorAndRedirect();
        }
    }

    private void showAuthErrorAndRedirect() {
        Toast.makeText(this, "Требуется авторизация", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void initViews() {
        try {
            dreamsRecyclerView = findViewById(R.id.dreamsRecyclerView);
            dreamsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
            adapter = new DreamAdapter(new ArrayList<>(), this, isSelectMode);
            dreamsRecyclerView.setAdapter(adapter);

            filterIndicator = findViewById(R.id.filterIndicator);
            btnClearFilter = findViewById(R.id.btnClearFilter);
            emptyStateText = findViewById(R.id.emptyStateText);

            // Установка обработчика на текст "Фильтр"
            TextView filterLabel = findViewById(R.id.filterLabel);
            filterLabel.setOnClickListener(v -> openFilterActivity());

            btnClearFilter.setOnClickListener(v -> clearFilter());

            setupBottomNavigation();

        } catch (Exception e) {
            Log.e(TAG, "View initialization failed", e);
            throw e;
        }
    }

    private void openFilterActivity() {
        try {
            Intent intent = new Intent(this, FilterActivity.class);
            startActivityForResult(intent, FILTER_REQUEST_CODE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open filter", e);
            Toast.makeText(this, "Не удалось открыть фильтр", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadDreams() {
        loadDreams(currentFilter, currentSearchQuery);
    }

    private void loadDreams(String dateFilter, String searchQuery) {
        executorService.execute(() -> {
            try {
                List<SleepRecord> dreams;

                if (searchQuery != null && !searchQuery.isEmpty()) {
                    dreams = db.sleepRecordDao().searchRecords(currentUserId, searchQuery);
                    currentSearchQuery = searchQuery;
                    saveSearchToPrefs(searchQuery);
                }
                else if (dateFilter != null) {
                    Date fromDate = getDateFromFilter(dateFilter);
                    if (fromDate != null) {
                        dreams = db.sleepRecordDao().getRecordsAfterDate(currentUserId, fromDate);
                        currentFilter = dateFilter;
                        saveFilterToPrefs(dateFilter);
                    } else {
                        dreams = db.sleepRecordDao().getAllByUser(currentUserId);
                        clearFilterPrefs();
                    }
                } else {
                    dreams = db.sleepRecordDao().getAllByUser(currentUserId);
                    clearFilterPrefs();
                    clearSearchPrefs();
                }

                runOnUiThread(() -> {
                    adapter.updateDreams(dreams);
                    updateFilterIndicator();

                    if (dreams.isEmpty()) {
                        showEmptyState();
                    } else {
                        hideEmptyState();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading dreams", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showEmptyState() {
        String message;
        if (currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
            message = "Не найдено записей по запросу: \"" + currentSearchQuery + "\"";
        } else if (currentFilter != null) {
            message = "Нет записей для выбранного фильтра";
        } else {
            message = "У вас пока нет записей в дневнике";
        }

        emptyStateText.setText(message);
        emptyStateText.setVisibility(View.VISIBLE);
        dreamsRecyclerView.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        emptyStateText.setVisibility(View.GONE);
        dreamsRecyclerView.setVisibility(View.VISIBLE);
    }

    private Date getDateFromFilter(String filterKey) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            switch (filterKey) {
                case "recent":
                    calendar.add(Calendar.DAY_OF_YEAR, -7);
                    break;
                case "week":
                    calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                    break;
                case "month":
                    calendar.set(Calendar.DAY_OF_MONTH, 1);
                    break;
                default:
                    return null;
            }
            return calendar.getTime();
        } catch (Exception e) {
            Log.e(TAG, "Error calculating filter date", e);
            return null;
        }
    }

    private void updateFilterIndicator() {
        if (filterIndicator != null && btnClearFilter != null) {
            if (currentFilter != null || currentSearchQuery != null) {
                StringBuilder filterText = new StringBuilder();

                if (currentFilter != null) {
                    switch (currentFilter) {
                        case "recent": filterText.append("Последние 7 дней"); break;
                        case "week": filterText.append("Текущая неделя"); break;
                        case "month": filterText.append("Текущий месяц"); break;
                    }
                }

                if (currentSearchQuery != null) {
                    if (filterText.length() > 0) {
                        filterText.append(" + ");
                    }
                    filterText.append("Поиск: \"").append(currentSearchQuery).append("\"");
                }

                filterIndicator.setText(filterText.toString());
                filterIndicator.setVisibility(View.VISIBLE);
                btnClearFilter.setVisibility(View.VISIBLE);
            } else {
                filterIndicator.setVisibility(View.GONE);
                btnClearFilter.setVisibility(View.GONE);
            }
        }
    }

    private void clearFilter() {
        currentFilter = null;
        currentSearchQuery = null;
        clearFilterPrefs();
        clearSearchPrefs();
        loadDreams();
    }

    private void saveFilterToPrefs(String filter) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_CURRENT_FILTER, filter)
                .apply();
    }

    private void saveSearchToPrefs(String searchQuery) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_CURRENT_SEARCH, searchQuery)
                .apply();
    }

    private void clearFilterPrefs() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .remove(KEY_CURRENT_FILTER)
                .apply();
    }

    private void clearSearchPrefs() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .remove(KEY_CURRENT_SEARCH)
                .apply();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILTER_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                String filter = data.hasExtra("date_filter") ? data.getStringExtra("date_filter") : null;
                String searchQuery = data.hasExtra("search_query") ? data.getStringExtra("search_query") : null;
                loadDreams(filter, searchQuery);
            } else {
                clearFilter();
            }
        }
    }

    @Override
    public void onDreamClick(SleepRecord dream) {
        try {
            if (isSelectMode) {
                Intent result = new Intent();
                result.putExtra("selected_dream", formatDreamForResult(dream));
                setResult(RESULT_OK, result);
                finish();
            } else {
                openDreamDetails(dream.id);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling dream click", e);
        }
    }

    private String formatDreamForResult(SleepRecord dream) {
        return (dream.title != null ? dream.title : "Без названия") + "\n" +
                (dream.notes != null ? dream.notes : "");
    }

    private void openDreamDetails(int dreamId) {
        Intent intent = new Intent(this, DreamDetailActivity.class);
        intent.putExtra("dream_id", dreamId);
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(SleepRecord dream) {
        if (!isSelectMode) {
            showDeleteConfirmationDialog(dream);
        }
    }

    private void showDeleteConfirmationDialog(SleepRecord dream) {
        new AlertDialog.Builder(this)
                .setTitle("Удаление записи")
                .setMessage("Удалить '" + (dream.title != null ? dream.title : "эту запись") + "'?")
                .setPositiveButton("Удалить", (d, w) -> deleteDream(dream))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteDream(SleepRecord dream) {
        executorService.execute(() -> {
            try {
                db.sleepRecordDao().delete(dream.id);
                runOnUiThread(() -> {
                    loadDreams();
                    Toast.makeText(this, "Запись удалена", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error deleting dream", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Ошибка удаления", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void setupBottomNavigation() {
        try {
            findViewById(R.id.navHome).setOnClickListener(v -> navigateTo(MainActivity.class));
            findViewById(R.id.navAnalyze).setOnClickListener(v -> navigateTo(AnalyzeSleepActivity.class));
            findViewById(R.id.navSettings).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
            findViewById(R.id.navProfile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        } catch (Exception e) {
            Log.e(TAG, "Bottom nav setup failed", e);
        }
    }

    private void navigateTo(Class<?> cls) {
        startActivity(new Intent(this, cls));
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAuthorization();
        loadDreams();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}