package com.xchen.example.akka.example2.model;

import java.util.Map;

public record RespondAllTemperatures(long requestId, Map<String, TemperatureReading> temperatures) {
}
