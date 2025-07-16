package com.xchen.example.akka.example2.model.command;

import akka.actor.typed.ActorRef;
import com.xchen.example.akka.example2.model.DeviceTemperatureRecorded;

public record DeviceRecordTemperature(long requestId, double value,
                                      ActorRef<DeviceTemperatureRecorded> replyTo) implements Command {
}
