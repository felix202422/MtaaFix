package com.mtaafix.common.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Simple success response")
public record SimpleResponse(
        @Schema(description = "Success status") boolean success,
        @Schema(description = "Optional message") String message) {

    public static SimpleResponse ok(String message) {
        return new SimpleResponse(true, message);
    }

    public static SimpleResponse ok() {
        return new SimpleResponse(true, null);
    }
}
