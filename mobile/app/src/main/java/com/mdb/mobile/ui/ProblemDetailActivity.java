package com.mdb.mobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ToggleButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mdb.mobile.api.ApiClient;
import com.mdb.mobile.api.ApiService;
import com.mdb.mobile.api.TokenManager;
import com.mdb.mobile.databinding.ActivityProblemDetailBinding;
import com.mdb.mobile.model.DifficultyRequest;
import com.mdb.mobile.model.ProblemDetail;
import com.mdb.mobile.model.StatusRequest;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProblemDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PROBLEM_ID = "problem_id";

    private ActivityProblemDetailBinding binding;
    private ApiService api;
    private TokenManager tokenManager;
    private long problemId;
    private ProblemDetail current;

    /** Evita reentrar no listener quando alteramos o estado dos toggles programaticamente. */
    private boolean suppressToggleEvents = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProblemDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        api = ApiClient.getInstance(this);
        tokenManager = new TokenManager(this);
        problemId = getIntent().getLongExtra(EXTRA_PROBLEM_ID, -1);

        binding.statementWebView.getSettings().setJavaScriptEnabled(true);

        if (problemId <= 0) {
            showError("Problema inválido.");
            return;
        }
        loadProblem();
    }

    private void loadProblem() {
        binding.loadingProgress.setVisibility(View.VISIBLE);
        binding.errorText.setVisibility(View.GONE);
        binding.contentGroup.setVisibility(View.GONE);

        api.getProblem(problemId).enqueue(new Callback<ProblemDetail>() {
            @Override
            public void onResponse(Call<ProblemDetail> call, Response<ProblemDetail> response) {
                binding.loadingProgress.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    current = response.body();
                    renderProblem(current);
                } else {
                    showError("Não foi possível carregar este problema.");
                }
            }

            @Override
            public void onFailure(Call<ProblemDetail> call, Throwable t) {
                binding.loadingProgress.setVisibility(View.GONE);
                showError("Falha de conexão: " + t.getMessage());
            }
        });
    }

    private void showError(String message) {
        binding.errorText.setText(message);
        binding.errorText.setVisibility(View.VISIBLE);
    }

    private void renderProblem(ProblemDetail problem) {
        binding.contentGroup.setVisibility(View.VISIBLE);
        binding.areaText.setText(problem.area);
        binding.titleText.setText(problem.title);

        String difficultyText = problem.communityDifficulty != null
                ? String.format(Locale.getDefault(), "Dificuldade comunitária: %.1f (%d avaliações) · Inicial: %d",
                    problem.communityDifficulty, problem.ratingsCount, problem.initialDifficulty)
                : String.format(Locale.getDefault(), "Sem avaliações ainda · Dificuldade inicial: %d", problem.initialDifficulty);
        binding.difficultyText.setText(difficultyText);

        binding.tagsText.setText(problem.tags == null || problem.tags.isEmpty() ? "" : String.join(" · ", problem.tags));

        binding.statementWebView.loadDataWithBaseURL(null, buildStatementHtml(problem), "text/html", "UTF-8", null);

        setupStatusToggles(problem);
        setupDifficultyButtons(problem);
    }

    private String buildStatementHtml(ProblemDetail problem) {
        return "<html><head><meta name='viewport' content='width=device-width, initial-scale=1'>" +
                "<script>window.MathJax = {tex: {inlineMath: [['$', '$']]}};</script>" +
                "<script src='https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js'></script>" +
                "<style>body{font-family:sans-serif;padding:8px;color:#1c2333;}" +
                "button{padding:8px 14px;border-radius:6px;border:1px solid #dde1ec;background:#fff;margin-top:10px;}" +
                ".hidden{display:none;}.box{background:#eef0f7;border-radius:8px;padding:12px;margin-top:8px;}</style>" +
                "</head><body>" +
                "<h3>Enunciado</h3><p>" + escapeHtml(problem.statement) + "</p>" +
                "<button onclick=\"var e=document.getElementById('hint');e.classList.toggle('hidden');" +
                "this.textContent=e.classList.contains('hidden')?'Mostrar dica':'Ocultar dica';\">Mostrar dica</button>" +
                "<div id='hint' class='box hidden'>" + escapeHtml(problem.hint) + "</div>" +
                "<br/><button onclick=\"var e=document.getElementById('solution');e.classList.toggle('hidden');" +
                "this.textContent=e.classList.contains('hidden')?'Mostrar solução':'Ocultar solução';\">Mostrar solução</button>" +
                "<div id='solution' class='box hidden'>" + escapeHtml(problem.solution) + "</div>" +
                (problem.source != null && !problem.source.isEmpty()
                        ? "<p style='color:#5b6478;margin-top:14px;'><strong>Fonte:</strong> " + escapeHtml(problem.source) + "</p>"
                        : "") +
                "</body></html>";
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void setupStatusToggles(ProblemDetail problem) {
        suppressToggleEvents = true;
        binding.favoriteToggle.setChecked(problem.hasStatus("FAVORITE"));
        binding.wantToDoToggle.setChecked(problem.hasStatus("WANT_TO_DO"));
        binding.doneToggle.setChecked(problem.hasStatus("DONE"));
        suppressToggleEvents = false;

        binding.favoriteToggle.setOnClickListener(v -> onStatusToggle(binding.favoriteToggle, "FAVORITE"));
        binding.wantToDoToggle.setOnClickListener(v -> onStatusToggle(binding.wantToDoToggle, "WANT_TO_DO"));
        binding.doneToggle.setOnClickListener(v -> onStatusToggle(binding.doneToggle, "DONE"));
    }

    private void onStatusToggle(ToggleButton toggle, String status) {
        if (suppressToggleEvents) return;

        if (!tokenManager.isLoggedIn()) {
            toggle.setChecked(!toggle.isChecked());
            Toast.makeText(this, "Entre para favoritar ou marcar status.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        boolean nowActive = toggle.isChecked();
        toggle.setEnabled(false);

        Callback<Void> callback = new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                toggle.setEnabled(true);
                if (!response.isSuccessful()) {
                    toggle.setChecked(!nowActive);
                    Toast.makeText(ProblemDetailActivity.this, "Não foi possível atualizar o status.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                toggle.setEnabled(true);
                toggle.setChecked(!nowActive);
                Toast.makeText(ProblemDetailActivity.this, "Falha de conexão.", Toast.LENGTH_SHORT).show();
            }
        };

        if (nowActive) {
            api.setStatus(problemId, new StatusRequest(status)).enqueue(callback);
        } else {
            api.removeStatus(problemId, status).enqueue(callback);
        }
    }

    private void setupDifficultyButtons(ProblemDetail problem) {
        LinearLayout row = binding.difficultyButtonsRow;
        row.removeAllViews();

        for (int value = 1; value <= 5; value++) {
            Button button = new Button(this);
            button.setText(String.valueOf(value));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            params.setMarginEnd(8);
            button.setLayoutParams(params);

            boolean selected = problem.userDifficulty != null && problem.userDifficulty == value;
            if (selected) {
                button.setBackgroundTintList(getColorStateList(com.mdb.mobile.R.color.mdb_primary));
                button.setTextColor(getColor(android.R.color.white));
            }

            int finalValue = value;
            button.setOnClickListener(v -> rateDifficulty(finalValue));
            row.addView(button);
        }
    }

    private void rateDifficulty(int value) {
        if (!tokenManager.isLoggedIn()) {
            Toast.makeText(this, "Entre para avaliar a dificuldade.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        Callback<Void> callback = new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ProblemDetailActivity.this, "Avaliação registrada.", Toast.LENGTH_SHORT).show();
                    loadProblem();
                } else {
                    Toast.makeText(ProblemDetailActivity.this, "Não foi possível registrar a avaliação.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ProblemDetailActivity.this, "Falha de conexão.", Toast.LENGTH_SHORT).show();
            }
        };

        if (current != null && current.userDifficulty == null) {
            api.createDifficulty(problemId, new DifficultyRequest(value)).enqueue(callback);
        } else {
            api.updateDifficulty(problemId, new DifficultyRequest(value)).enqueue(callback);
        }
    }
}
