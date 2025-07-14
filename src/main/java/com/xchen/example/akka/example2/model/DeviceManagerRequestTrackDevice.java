package com.xchen.example.akka.example2.model;

import akka.actor.typed.ActorRef;
import com.xchen.example.akka.example2.DeviceManager;

public record DeviceManagerRequestTrackDevice(String groupId, String deviceId,
                                              ActorRef<DeviceManager.DeviceRegistered> replyTo) implements Command {

}
