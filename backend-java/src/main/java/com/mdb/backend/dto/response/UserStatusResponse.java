package com.mdb.backend.dto.response;

import java.util.List;

public record UserStatusResponse(
        List<String> statuses
) {
}
