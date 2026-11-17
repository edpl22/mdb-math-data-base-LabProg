package com.mdb.mobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.mdb.mobile.api.ApiClient;
import com.mdb.mobile.api.ApiService;
import com.mdb.mobile.api.TokenManager;
import com.mdb.mobile.databinding.ActivityCatalogBinding;
import com.mdb.mobile.model.AreaDto;
import com.mdb.mobile.model.PageResponse;
import com.mdb.mobile.model.ProblemSummary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CatalogActivity extends AppCompatActivity {

    private ActivityCatalogBinding binding;
    private ApiService api;
    private TokenManager tokenManager;
    private ProblemAdapter adapter;

    private final List<String> areaOptions = new ArrayList<>();
    private static final List<String> DIFFICULTY_OPTIONS =
            Arrays.asList("Qualquer", "1", "2", "3", "4", "5");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCatalogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        api = ApiClient.getInstance(this);
        tokenManager = new TokenManager(this);

        adapter = new ProblemAdapter(this::openProblem);
        binding.problemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.problemsRecyclerView.setAdapter(adapter);

        binding.swipeRefresh.setOnRefreshListener(this::loadProblems);
        binding.searchButton.setOnClickListener(v -> loadProblems());
        binding.profileButton.setOnClickListener(v -> {
            if (tokenManager.isLoggedIn()) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else {
                Toast.makeText(this, "Entre para acessar seu perfil.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
            }
        });
        binding.logoutButton.setOnClickListener(v -> {
            tokenManager.clear();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        setupDifficultySpinners();
        loadAreas();
        loadProblems();
    }

    private void setupDifficultySpinners() {
        ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, DIFFICULTY_OPTIONS);
        binding.minDifficultySpinner.setAdapter(difficultyAdapter);
        binding.maxDifficultySpinner.setAdapter(difficultyAdapter);
        binding.maxDifficultySpinner.setSelection(0);
    }

    private void loadAreas() {
        areaOptions.clear();
        areaOptions.add("Todas as áreas");
        api.listAreas().enqueue(new Callback<List<AreaDto>>() {
            @Override
            public void onResponse(Call<List<AreaDto>> call, Response<List<AreaDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (AreaDto area : response.body()) areaOptions.add(area.name);
                }
                ArrayAdapter<String> areaAdapter = new ArrayAdapter<>(
                        CatalogActivity.this, android.R.layout.simple_spinner_dropdown_item, areaOptions);
                binding.areaSpinner.setAdapter(areaAdapter);
            }

            @Override
            public void onFailure(Call<List<AreaDto>> call, Throwable t) {
                Toast.makeText(CatalogActivity.this, "Não foi possível carregar as áreas.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProblems() {
        binding.swipeRefresh.setRefreshing(true);
        binding.emptyStateText.setVisibility(View.GONE);

        Map<String, String> params = new HashMap<>();
        String text = binding.searchInput.getText().toString().trim();
        if (!text.isEmpty()) params.put("text", text);

        if (binding.areaSpinner.getSelectedItemPosition() > 0) {
            params.put("area", (String) binding.areaSpinner.getSelectedItem());
        }
        if (binding.minDifficultySpinner.getSelectedItemPosition() > 0) {
            params.put("minDifficulty", (String) binding.minDifficultySpinner.getSelectedItem());
        }
        if (binding.maxDifficultySpinner.getSelectedItemPosition() > 0) {
            params.put("maxDifficulty", (String) binding.maxDifficultySpinner.getSelectedItem());
        }
        params.put("size", "30");

        api.searchProblems(params).enqueue(new Callback<PageResponse<ProblemSummary>>() {
            @Override
            public void onResponse(Call<PageResponse<ProblemSummary>> call, Response<PageResponse<ProblemSummary>> response) {
                binding.swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<ProblemSummary> content = response.body().content;
                    adapter.submitList(content);
                    binding.emptyStateText.setVisibility(content.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    Toast.makeText(CatalogActivity.this, "Erro ao buscar problemas.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<ProblemSummary>> call, Throwable t) {
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(CatalogActivity.this, "Falha de conexão: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openProblem(ProblemSummary problem) {
        Intent intent = new Intent(this, ProblemDetailActivity.class);
        intent.putExtra(ProblemDetailActivity.EXTRA_PROBLEM_ID, problem.id);
        startActivity(intent);
    }
}
