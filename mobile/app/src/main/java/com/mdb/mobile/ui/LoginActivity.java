package com.mdb.mobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.mdb.mobile.api.ApiClient;
import com.mdb.mobile.api.ApiService;
import com.mdb.mobile.api.TokenManager;
import com.mdb.mobile.databinding.ActivityLoginBinding;
import com.mdb.mobile.model.AuthModels;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private ApiService api;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        api = ApiClient.getInstance(this);
        tokenManager = new TokenManager(this);

        if (tokenManager.isLoggedIn()) {
            goToCatalog();
            return;
        }

        binding.loginButton.setOnClickListener(v -> doLogin());
        binding.goToRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        binding.skipLogin.setOnClickListener(v -> goToCatalog());
    }

    private void doLogin() {
        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Preencha email e senha.");
            return;
        }

        setLoading(true);
        api.login(new AuthModels.LoginRequest(email, password)).enqueue(new Callback<AuthModels.AuthResponse>() {
            @Override
            public void onResponse(Call<AuthModels.AuthResponse> call, Response<AuthModels.AuthResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    tokenManager.saveSession(response.body().token, response.body().user);
                    goToCatalog();
                } else {
                    showError("Email ou senha inválidos.");
                }
            }

            @Override
            public void onFailure(Call<AuthModels.AuthResponse> call, Throwable t) {
                setLoading(false);
                showError("Falha de conexão: " + t.getMessage());
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.loginProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.loginButton.setEnabled(!loading);
    }

    private void showError(String message) {
        binding.loginError.setText(message);
        binding.loginError.setVisibility(View.VISIBLE);
    }

    private void goToCatalog() {
        startActivity(new Intent(this, CatalogActivity.class));
        finish();
    }
}
