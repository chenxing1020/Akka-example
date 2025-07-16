package com.xchen.example.akka.example2.model.command;

public record DeviceGroupTerminated(String groupId) implements Command {
}
