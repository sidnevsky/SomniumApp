package com.example.somnium;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class AnalyzeSleepActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SELECT_DREAM = 1;
    private TextView selectDreamText;
    private String selectedDreamText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analyze_sleep);

        selectDreamText = findViewById(R.id.selectDreamText);

        TextView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        setupDreamSelection();

        TextView startAnalysisButton = findViewById(R.id.startAnalysisButton);
        startAnalysisButton.setOnClickListener(v -> {
            if (selectedDreamText.isEmpty()) {
                Toast.makeText(this, "Пожалуйста, выберите сон для анализа", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(AnalyzeSleepActivity.this, AnalysisActivity.class);
                intent.putExtra("dream_text", selectedDreamText);
                startActivity(intent);
            }
        });
    }

    private void setupDreamSelection() {
        CardView selectDreamButton = findViewById(R.id.selectDreamButton);
        selectDreamButton.setOnClickListener(v -> {
            Intent intent = new Intent(AnalyzeSleepActivity.this, DreamDiaryActivity.class);
            intent.putExtra("select_mode", true);
            startActivityForResult(intent, REQUEST_CODE_SELECT_DREAM);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SELECT_DREAM && resultCode == RESULT_OK) {
            if (data != null && data.hasExtra("selected_dream")) {
                selectedDreamText = data.getStringExtra("selected_dream");
                selectDreamText.setText("Выбран сон: " + selectedDreamText);
            }
        }
    }
}