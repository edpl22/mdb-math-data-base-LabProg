package com.mdb.mobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.mdb.mobile.api.ApiClient;
import com.mdb.mobile.api.ApiService;
import com.mdb.mobile.api.TokenManager;
import com.mdb.mobile.databinding.ActivityRegisterBinding;
import com.mdb.mobile.model.AuthModels;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private ApiService api;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        api = ApiClient.getInstance(this);
        tokenManager = new TokenManager(this);

        binding.registerButton.setOnClickListener(v -> doRegister());
    }

    private void doRegister() {
        String name = binding.nameInput.getText().toString().trim();
        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString();

        if (name.isEmpty() || email.isEmpty() || password.length() < 6) {
            showError("Preencha nome, email e uma senha com ao menos 6 caracteres.");
            return;
        }

        setLoading(true);
        api.register(new AuthModels.RegisterRequest(name, email, password))
                .enqueue(new Callback<AuthModels.AuthResponse>() {
            @Override
            public void onResponse(Call<AuthModels.AuthResponse> call, Response<AuthModels.AuthResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    tokenManager.saveSession(response.body().token, response.body().user);
                    startActivity(new Intent(RegisterActivity.this, CatalogActivity.class));
                    finish();
                } else {
                    showError("Não foi possível criar a conta. O email já pode estar em uso.");
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
        binding.registerProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.registerButton.setEnabled(!loading);
    }

    private void showError(String message) {
        binding.registerError.setText(message);
        binding.registerError.setVisibility(View.VISIBLE);
    }
}
