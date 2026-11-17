package com.mdb.mobile.api;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.mdb.mobile.model.UserDto;

public class TokenManager {

    private static final String PREFS = "mdb_prefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER = "user";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public TokenManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveSession(String token, UserDto user) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USER, gson.toJson(user))
                .apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public UserDto getUser() {
        String json = prefs.getString(KEY_USER, null);
        return json == null ? null : gson.fromJson(json, UserDto.class);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
