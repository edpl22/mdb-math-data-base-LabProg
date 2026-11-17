package com.mdb.mobile.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mdb.mobile.databinding.ItemProblemBinding;
import com.mdb.mobile.model.ProblemSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProblemAdapter extends RecyclerView.Adapter<ProblemAdapter.ViewHolder> {

    public interface OnProblemClickListener {
        void onProblemClick(ProblemSummary problem);
    }

    private final List<ProblemSummary> items = new ArrayList<>();
    private final OnProblemClickListener listener;

    public ProblemAdapter(OnProblemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ProblemSummary> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProblemBinding binding = ItemProblemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProblemSummary problem = items.get(position);
        holder.binding.problemArea.setText(problem.area);
        holder.binding.problemTitle.setText(problem.title);

        String difficultyText = problem.communityDifficulty != null
                ? String.format(Locale.getDefault(), "Dificuldade comunitária: %.1f (%d avaliações)",
                    problem.communityDifficulty, problem.ratingsCount)
                : "Sem avaliações de dificuldade ainda";
        holder.binding.problemDifficulty.setText(difficultyText);

        String tags = problem.tags == null || problem.tags.isEmpty() ? "" : String.join(" · ", problem.tags);
        holder.binding.problemTags.setText(tags);

        holder.itemView.setOnClickListener(v -> listener.onProblemClick(problem));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemProblemBinding binding;

        ViewHolder(ItemProblemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
