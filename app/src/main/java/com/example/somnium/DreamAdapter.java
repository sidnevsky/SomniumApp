package com.example.somnium;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DreamAdapter extends RecyclerView.Adapter<DreamAdapter.DreamViewHolder> {
    private static final String TAG = "DreamAdapter";
    private List<SleepRecord> dreams;
    private final OnDreamClickListener listener;
    private final boolean isSelectMode;

    public interface OnDreamClickListener {
        void onDreamClick(SleepRecord dream);
        void onDeleteClick(SleepRecord dream);
    }

    public DreamAdapter(List<SleepRecord> dreams, OnDreamClickListener listener, boolean isSelectMode) {
        this.dreams = dreams != null ? new ArrayList<>(dreams) : new ArrayList<>();
        this.listener = listener;
        this.isSelectMode = isSelectMode;
        Log.d(TAG, "Adapter created with " + this.dreams.size() + " dreams");
    }

    @NonNull
    @Override
    public DreamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dream, parent, false);
        return new DreamViewHolder(view, isSelectMode);
    }

    @Override
    public void onBindViewHolder(@NonNull DreamViewHolder holder, int position) {
        if (position < 0 || position >= dreams.size()) {
            Log.w(TAG, "Invalid position: " + position);
            return;
        }

        SleepRecord dream = dreams.get(position);
        holder.bind(dream, listener);
    }

    @Override
    public int getItemCount() {
        return dreams.size();
    }

    public void updateDreams(List<SleepRecord> newDreams) {
        this.dreams = newDreams != null ? new ArrayList<>(newDreams) : new ArrayList<>();
        Log.d(TAG, "Updating dreams, new count: " + dreams.size());
        try {
            notifyDataSetChanged();
        } catch (Exception e) {
            Log.e(TAG, "Error notifying dataset changed", e);
        }
    }

    static class DreamViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleText;
        private final TextView dateLabelText;
        private final TextView dateValueText;
        private final TextView shortNoteText;
        private final ImageButton deleteBtn;

        DreamViewHolder(@NonNull View itemView, boolean isSelectMode) {
            super(itemView);
            titleText = itemView.findViewById(R.id.dreamTitle);
            dateLabelText = itemView.findViewById(R.id.dreamDateLabel);
            dateValueText = itemView.findViewById(R.id.dreamDateValue);
            shortNoteText = itemView.findViewById(R.id.dreamShortNote);
            deleteBtn = itemView.findViewById(R.id.deleteBtn);

            deleteBtn.setVisibility(isSelectMode ? View.GONE : View.VISIBLE);
        }

        void bind(SleepRecord dream, OnDreamClickListener listener) {
            try {
                // Установка данных
                titleText.setText(dream.title != null ? dream.title : "Без названия");
                dateLabelText.setText("Дата сна");

                if (dream.createdAt != null) {
                    dateValueText.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                            .format(dream.createdAt));
                } else {
                    dateValueText.setText("неизвестна");
                }

                String notes = dream.notes != null ? dream.notes : "";
                shortNoteText.setText(notes.length() > 50 ?
                        notes.substring(0, 50) + "..." : notes);

                // Обработчики кликов
                itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onDreamClick(dream);
                });

                deleteBtn.setOnClickListener(v -> {
                    if (listener != null) listener.onDeleteClick(dream);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error binding dream", e);
                titleText.setText("Ошибка данных");
                dateValueText.setText("");
                shortNoteText.setText("");
            }
        }
    }
}