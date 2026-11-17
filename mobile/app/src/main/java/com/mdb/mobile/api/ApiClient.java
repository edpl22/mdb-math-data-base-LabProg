package com.mdb.mobile.api;

import android.content.Context;

import com.mdb.mobile.BuildConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Cliente Retrofit único, apontando para a API Java (núcleo: usuários, problemas, avaliações, status). */
public final class ApiClient {

    private static ApiService instance;

    private ApiClient() {
    }

    public static synchronized ApiService getInstance(Context context) {
        if (instance == null) {
            TokenManager tokenManager = new TokenManager(context);

            Interceptor authInterceptor = chain -> {
                Request original = chain.request();
                String token = tokenManager.getToken();
                if (token == null) {
                    return chain.proceed(original);
                }
                Request authorized = original.newBuilder()
                        .header("Authorization", "Bearer " + token)
                        .build();
                return chain.proceed(authorized);
            };

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .addInterceptor(logging)
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.JAVA_API_BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            instance = retrofit.create(ApiService.class);
        }
        return instance;
    }
}
