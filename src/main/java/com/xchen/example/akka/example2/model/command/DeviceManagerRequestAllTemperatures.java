package com.xchen.example.akka.example2.model.command;

import akka.actor.typed.ActorRef;
import com.xchen.example.akka.example2.model.RespondAllTemperatures;

public record DeviceManagerRequestAllTemperatures(long requestId, String groupId,
                                                  ActorRef<RespondAllTemperatures> replyTo) implements Command {

}
