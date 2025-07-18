package com.xchen.example.akka.example2.model.command;

import akka.actor.typed.ActorRef;

public record DeviceGroupDeviceTerminated(ActorRef<Command> device, String groupId, String deviceId) implements Command {
}
