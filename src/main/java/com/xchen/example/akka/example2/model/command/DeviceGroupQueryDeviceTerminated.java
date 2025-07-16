package com.xchen.example.akka.example2.model.command;

public record DeviceGroupQueryDeviceTerminated(String deviceId) implements Command {
}
