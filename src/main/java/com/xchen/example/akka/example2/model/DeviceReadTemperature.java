package com.xchen.example.akka.example2.model;

import akka.actor.typed.ActorRef;

public record DeviceReadTemperature(long requestId, ActorRef<DeviceRespondTemperature> replyTo) implements Command {
}
