package com.apprh.backend.ia.api;

import java.util.List;

public record ChatResponse(String reply, boolean aiAvailable, List<String> sources) {

    public ChatResponse(String reply, boolean aiAvailable) {
        this(reply, aiAvailable, List.of());
    }
}
