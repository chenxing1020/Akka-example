package com.xchen.example.akka.example2.model;

import akka.actor.typed.ActorRef;
import lombok.Data;

@Data
public class RecordTemperature implements Command {
    final double value;
    final long requestId;
    final ActorRef<TemperatureRecorded> replyTo;
    public RecordTemperature(long requestId, double value, ActorRef<TemperatureRecorded> replyTo) {
        this.requestId = requestId;
        this.value = value;
        this.replyTo = replyTo;
    }
}
