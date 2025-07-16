package com.xchen.example.akka.example2.model.command;

import akka.actor.typed.ActorRef;
import com.xchen.example.akka.example2.model.DeviceRespondTemperature;

public record DeviceReadTemperature(long requestId, ActorRef<DeviceRespondTemperature> replyTo) implements Command {
}
