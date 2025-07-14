package com.xchen.example.akka.example2.model;

import akka.actor.typed.ActorRef;
import com.xchen.example.akka.example2.Device;

public record DeviceRecordTemperature(long requestId, double value,
                                      ActorRef<DeviceTemperatureRecorded> replyTo) implements Command {
}
