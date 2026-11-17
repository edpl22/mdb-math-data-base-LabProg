package com.mdb.mobile.model;

import java.util.List;

public class ProblemDetail {
    public Long id;
    public String title;
    public String statement;
    public String hint;
    public String solution;
    public String source;
    public String area;
    public List<String> tags;
    public Integer initialDifficulty;
    public Double communityDifficulty;
    public long ratingsCount;
    public Integer userDifficulty;
    public List<String> userStatuses;
    public String createdByName;
    public String createdAt;
    public String updatedAt;

    public boolean hasStatus(String status) {
        return userStatuses != null && userStatuses.contains(status);
    }
}
