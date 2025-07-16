package com.xchen.example.akka.example2.model;

import akka.actor.typed.ActorRef;
import com.xchen.example.akka.example2.model.command.Command;

public record DeviceRegistered(ActorRef<Command> device) {
}