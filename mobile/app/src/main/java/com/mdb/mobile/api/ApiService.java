package com.mdb.mobile.api;

import com.mdb.mobile.model.AreaDto;
import com.mdb.mobile.model.AuthModels;
import com.mdb.mobile.model.DifficultyRequest;
import com.mdb.mobile.model.PageResponse;
import com.mdb.mobile.model.ProblemDetail;
import com.mdb.mobile.model.ProblemSummary;
import com.mdb.mobile.model.StatusRequest;
import com.mdb.mobile.model.UserProblemsResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface ApiService {

    @POST("/api/auth/register")
    Call<AuthModels.AuthResponse> register(@Body AuthModels.RegisterRequest request);

    @POST("/api/auth/login")
    Call<AuthModels.AuthResponse> login(@Body AuthModels.LoginRequest request);

    @GET("/api/problems")
    Call<PageResponse<ProblemSummary>> searchProblems(@QueryMap Map<String, String> params);

    @GET("/api/problems/{id}")
    Call<ProblemDetail> getProblem(@Path("id") long id);

    @PUT("/api/problems/{id}/status")
    Call<Void> setStatus(@Path("id") long id, @Body StatusRequest request);

    @DELETE("/api/problems/{id}/status")
    Call<Void> removeStatus(@Path("id") long id, @Query("status") String status);

    @POST("/api/problems/{id}/difficulty")
    Call<Void> createDifficulty(@Path("id") long id, @Body DifficultyRequest request);

    @PUT("/api/problems/{id}/difficulty")
    Call<Void> updateDifficulty(@Path("id") long id, @Body DifficultyRequest request);

    @GET("/api/areas")
    Call<List<AreaDto>> listAreas();

    @GET("/api/users/me/problems")
    Call<UserProblemsResponse> myProblems();
}
