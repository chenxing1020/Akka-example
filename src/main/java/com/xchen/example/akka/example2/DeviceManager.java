package com.xchen.example.akka.example2;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DeviceManager extends AbstractBehavior<DeviceManager.Command> {

    public interface Command {
    }

    public static final class RequestTrackDevice implements
            Command, DeviceGroup.Command {
        public final String groupId;
        public final String deviceId;
        public final ActorRef<DeviceRegistered> replyTo;

        public RequestTrackDevice(String groupId, String deviceId, ActorRef<DeviceRegistered> replyTo) {
            this.groupId = groupId;
            this.deviceId = deviceId;
            this.replyTo = replyTo;
        }
    }

    public static final class DeviceRegistered {
        public final ActorRef<Device.Command> device;

        public DeviceRegistered(ActorRef<Device.Command> device) {
            this.device = device;
        }
    }

    public static final class RequestDeviceList implements DeviceManager.Command, DeviceGroup.Command {
        final long requestId;
        final String groupId;
        final ActorRef<ReplyDeviceList> replyTo;

        public RequestDeviceList(long requestId, String groupId, ActorRef<ReplyDeviceList> replyTo) {
            this.requestId = requestId;
            this.groupId = groupId;
            this.replyTo = replyTo;
        }
    }

    public static final class ReplyDeviceList {
        final long requestId;
        final Set<String> ids;

        public ReplyDeviceList(long requestId, Set<String> ids) {
            this.requestId = requestId;
            this.ids = ids;
        }
    }

    private static class DeviceGroupTerminated implements DeviceManager.Command {
        public final String groupId;

        DeviceGroupTerminated(String groupId) {
            this.groupId = groupId;
        }
    }

    public DeviceManager(ActorContext<DeviceManager.Command> context) {
        super(context);
        context.getLog().info("DeviceManger started");
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(DeviceManager::new);
    }

    private final Map<String, ActorRef<DeviceGroup.Command>> groupIdToActor = new HashMap<>();

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(RequestTrackDevice.class, this::onTrackDevice)
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
        getContext().getLog().info("Device group actor for {} has been terminated", t.groupId);
        groupIdToActor.remove(t.groupId);
        return this;
    }

    private Behavior<Command> onRequestDeviceList(RequestDeviceList r) {
        ActorRef<DeviceGroup.Command> ref = groupIdToActor.get(r.groupId);
        if (ref != null) {
            ref.tell(r);
        } else {
            r.replyTo.tell(new ReplyDeviceList(r.requestId, Collections.emptySet()));
        }
        return this;
    }

    private Behavior<Command> onTrackDevice(RequestTrackDevice trackMsg) {
        String groupId = trackMsg.groupId;
        ActorRef<DeviceGroup.Command> ref = groupIdToActor.get(groupId);
        if (ref != null) {
            ref.tell(trackMsg);
        } else {
            getContext().getLog().info("Creating device group actor for {}", groupId);
            ActorRef<DeviceGroup.Command> groupActor = getContext().spawn(DeviceGroup.create(groupId), "group-" + groupId);
            getContext().watchWith(groupActor, new DeviceGroupTerminated(groupId));
            groupActor.tell(trackMsg);
            groupIdToActor.put(groupId, groupActor);
        }
        return this;
    }

}
