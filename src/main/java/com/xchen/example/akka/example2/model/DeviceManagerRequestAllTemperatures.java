package com.xchen.example.akka.example2.model;

import akka.actor.typed.ActorRef;
import com.xchen.example.akka.example2.DeviceManager;

public record DeviceManagerRequestAllTemperatures(long requestId, String groupId,
                                                  ActorRef<DeviceManager.RespondAllTemperatures> replyTo) implements Command {

}
