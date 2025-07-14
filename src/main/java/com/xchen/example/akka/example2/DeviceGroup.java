package com.xchen.example.akka.example2;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.xchen.example.akka.example2.model.Command;
import com.xchen.example.akka.example2.model.DeviceGroupDeviceTerminated;
import com.xchen.example.akka.example2.model.DeviceManagerRequestTrackDevice;

import java.util.HashMap;
import java.util.Map;

public class DeviceGroup extends AbstractBehavior<Command> {

    public static Behavior<Command> create(String groupId) {
        return Behaviors.setup(context -> new DeviceGroup(context, groupId));
    }

    private final String groupId;
    private final Map<String, ActorRef<Command>> deviceIdToActor = new HashMap<>();

    private DeviceGroup(ActorContext<Command> context, String groupId) {
        super(context);
        this.groupId = groupId;
        context.getLog().info("DeviceGroup {} started", groupId);
    }

    private DeviceGroup onTrackDevice(DeviceManagerRequestTrackDevice trackMsg) {
        if (this.groupId.equals(trackMsg.groupId())) {
            ActorRef<Command> deviceActor = deviceIdToActor.get(trackMsg.deviceId());
            if (deviceActor != null) {
                trackMsg.replyTo().tell(new DeviceManager.DeviceRegistered(deviceActor));
            } else {
                getContext().getLog().info("Creating device actor for {}", trackMsg.deviceId());
                deviceActor = getContext().spawn(Device.create(groupId, trackMsg.deviceId()), "device-" + trackMsg.deviceId());
                getContext().watchWith(deviceActor, new DeviceGroupDeviceTerminated(deviceActor, groupId, trackMsg.deviceId()));
                deviceIdToActor.put(trackMsg.deviceId(), deviceActor);
                trackMsg.replyTo().tell(new DeviceManager.DeviceRegistered(deviceActor));
            }
        } else {
            getContext().getLog().warn("Ignoring TrackDevice request for {}. This actor is responsible for {}.",
                    groupId, this.groupId);
        }
        return this;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(DeviceManagerRequestTrackDevice.class, this::onTrackDevice)
                .onMessage(DeviceGroupDeviceTerminated.class, this::onTerminated)
                .onMessage(DeviceManager.RequestDeviceList.class,
                        r -> r.groupId.equals(groupId),
                        this::onDeviceList)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<Command> onDeviceList(DeviceManager.RequestDeviceList r) {
        r.replyTo.tell(new DeviceManager.ReplyDeviceList(r.requestId, deviceIdToActor.keySet()));
        return this;
    }

    private Behavior<Command> onTerminated(DeviceGroupDeviceTerminated t) {
        getContext().getLog().info("Device actor for {} has been terminated", t.deviceId());
        deviceIdToActor.remove(t.deviceId());
        return this;
    }

    private DeviceGroup onPostStop() {
        getContext().getLog().info("DeviceGroup {} stopped", groupId);
        return this;
    }
}
