package com.xchen.example.akka.example2.model.command;

import akka.actor.typed.ActorRef;
import com.xchen.example.akka.example2.model.DeviceRegistered;

public record DeviceManagerRequestTrackDevice(String groupId, String deviceId,
                                              ActorRef<DeviceRegistered> replyTo) implements Command {

}
