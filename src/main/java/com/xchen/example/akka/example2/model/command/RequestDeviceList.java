package com.xchen.example.akka.example2.model.command;

import akka.actor.typed.ActorRef;
import com.xchen.example.akka.example2.model.ReplyDeviceList;

public record RequestDeviceList(long requestId, String groupId, ActorRef<ReplyDeviceList> replyTo) implements Command {
}
