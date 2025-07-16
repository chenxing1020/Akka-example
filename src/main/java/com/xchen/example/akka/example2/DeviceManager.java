package com.xchen.example.akka.example2;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.xchen.example.akka.example2.model.ReplyDeviceList;
import com.xchen.example.akka.example2.model.command.Command;
import com.xchen.example.akka.example2.model.command.DeviceGroupTerminated;
import com.xchen.example.akka.example2.model.command.DeviceManagerRequestTrackDevice;
import com.xchen.example.akka.example2.model.command.RequestDeviceList;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DeviceManager extends AbstractBehavior<Command> {



    public DeviceManager(ActorContext<Command> context) {
        super(context);
        context.getLog().info("DeviceManger started");
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(DeviceManager::new);
    }

    private final Map<String, ActorRef<Command>> groupIdToActor = new HashMap<>();

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(DeviceManagerRequestTrackDevice.class, this::onTrackDevice)
                .onMessage(RequestDeviceList.class, this::onRequestDeviceList)
                .onMessage(DeviceGroupTerminated.class, this::onTerminated)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<Command> onPostStop() {
        getContext().getLog().info("DeviceManager stopped");
        return this;
    }

    private Behavior<Command> onTerminated(DeviceGroupTerminated t) {
        getContext().getLog().info("Device group actor for {} has been terminated", t.groupId());
        groupIdToActor.remove(t.groupId());
        return this;
    }

    private Behavior<Command> onRequestDeviceList(RequestDeviceList r) {
        ActorRef<Command> ref = groupIdToActor.get(r.groupId());
        if (ref != null) {
            ref.tell(r);
        } else {
            r.replyTo().tell(new ReplyDeviceList(r.requestId(), Collections.emptySet()));
        }
        return this;
    }

    private Behavior<Command> onTrackDevice(DeviceManagerRequestTrackDevice trackMsg) {
        String groupId = trackMsg.groupId();
        ActorRef<Command> ref = groupIdToActor.get(groupId);
        if (ref != null) {
            ref.tell(trackMsg);
        } else {
            getContext().getLog().info("Creating device group actor for {}", groupId);
            ActorRef<Command> groupActor = getContext().spawn(DeviceGroup.create(groupId), "group-" + groupId);
            getContext().watchWith(groupActor, new DeviceGroupTerminated(groupId));
            groupActor.tell(trackMsg);
            groupIdToActor.put(groupId, groupActor);
        }
        return this;
    }

}
