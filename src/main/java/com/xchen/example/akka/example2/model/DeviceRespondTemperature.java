package com.xchen.example.akka.example2.model;

import java.util.Optional;

public record DeviceRespondTemperature(long requestId, String deviceId, Optional<Double> value) {
}
