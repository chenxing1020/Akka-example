package com.xchen.example.akka.example2.model;

import akka.actor.typed.ActorRef;
import lombok.Data;

@Data
public class ReadTemperature implements Command {

    final long requestId;
    final ActorRef<RespondTemperature> replyTo;

    public ReadTemperature(long requestId, ActorRef<RespondTemperature> replyTo) {
        this.requestId = requestId;
        this.replyTo = replyTo;
    }
}
