package com.mdb.mobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.mdb.mobile.api.ApiClient;
import com.mdb.mobile.api.ApiService;
import com.mdb.mobile.api.TokenManager;
import com.mdb.mobile.databinding.ActivityProfileBinding;
import com.mdb.mobile.model.ProblemSummary;
import com.mdb.mobile.model.UserProblemsResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private ApiService api;
    private ProblemAdapter adapter;
    private UserProblemsResponse data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        TokenManager tokenManager = new TokenManager(this);
        if (!tokenManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        if (tokenManager.getUser() != null) {
            binding.profileTitle.setText(getString(com.mdb.mobile.R.string.profile_title) + " — " + tokenManager.getUser().name);
        }

        api = ApiClient.getInstance(this);
        adapter = new ProblemAdapter(problem -> {
            Intent intent = new Intent(this, ProblemDetailActivity.class);
            intent.putExtra(ProblemDetailActivity.EXTRA_PROBLEM_ID, problem.id);
            startActivity(intent);
        });
        binding.profileRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.profileRecyclerView.setAdapter(adapter);

        binding.tabFavorites.setOnClickListener(v -> showList(data != null ? data.favorites : null, "Nenhum favorito ainda."));
        binding.tabWantToDo.setOnClickListener(v -> showList(data != null ? data.wantToDo : null, "Nada marcado como \"quero fazer\"."));
        binding.tabDone.setOnClickListener(v -> showList(data != null ? data.done : null, "Nenhum problema concluído ainda."));

        loadData();
    }

    private void loadData() {
        api.myProblems().enqueue(new Callback<UserProblemsResponse>() {
            @Override
            public void onResponse(Call<UserProblemsResponse> call, Response<UserProblemsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data = response.body();
                    showList(data.favorites, "Nenhum favorito ainda.");
                } else {
                    Toast.makeText(ProfileActivity.this, "Não foi possível carregar seu perfil.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserProblemsResponse> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Falha de conexão: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showList(List<ProblemSummary> problems, String emptyMessage) {
        adapter.submitList(problems);
        boolean empty = problems == null || problems.isEmpty();
        binding.profileEmptyText.setText(emptyMessage);
        binding.profileEmptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
    }
}
