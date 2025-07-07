package com.xchen.example.akka.example2.model;

import lombok.Data;

@Data
public class TemperatureRecorded {

    final long requestId;

    public TemperatureRecorded(long requestId) {
        this.requestId = requestId;
    }
}
