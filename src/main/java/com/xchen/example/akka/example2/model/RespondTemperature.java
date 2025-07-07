package com.xchen.example.akka.example2.model;

import lombok.Data;

import java.util.Optional;

@Data
public class RespondTemperature {

    final long requestId;
    final Optional<Double> value;

    public RespondTemperature(long requestId, Optional<Double> value) {
        this.requestId = requestId;
        this.value = value;
    }
}
