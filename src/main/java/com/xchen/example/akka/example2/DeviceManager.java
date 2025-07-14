package com.xchen.example.akka.example2;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;
import com.xchen.example.akka.example2.model.Command;
import com.xchen.example.akka.example2.model.DeviceGroupDeviceTerminated;
import com.xchen.example.akka.example2.model.DeviceGroupTerminated;
import com.xchen.example.akka.example2.model.DeviceManagerRequestTrackDevice;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DeviceManager extends AbstractBehavior<Command> {

    public static final class RespondAllTemperatures {
        final long requestId;
        final Map<String, TemperatureReading> temperatures;

        public RespondAllTemperatures(long requestId, Map<String, TemperatureReading> temperatures) {
            this.requestId = requestId;
            this.temperatures = temperatures;
        }
    }

    public interface TemperatureReading {}

    public static final class Temperature implements  TemperatureReading {
        public final double value;

        public Temperature(double value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Temperature that = (Temperature) o;
            return Double.compare(that.value, value) == 0;
        }

        @Override
        public int hashCode() {
            long temp = Double.doubleToLongBits(value);
            return (int) (temp ^ (temp >>> 32));
        }

        @Override
        public String toString() {
            return "Temperature{" + "value=" + value + '}';
        }
    }

    public enum TemperatureNotAvailable implements TemperatureReading {
        INSTANCE
    }

    public enum DeviceNotAvailable implements TemperatureReading {
        INSTANCE
    }

    public enum DeviceTimedOut implements TemperatureReading {
        INSTANCE
    }

    public static final class DeviceRegistered {
        public final ActorRef<Command> device;

        public DeviceRegistered(ActorRef<Command> device) {
            this.device = device;
        }
    }

    public static final class RequestDeviceList implements Command {
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
        ActorRef<Command> ref = groupIdToActor.get(r.groupId);
        if (ref != null) {
            ref.tell(r);
        } else {
            r.replyTo.tell(new ReplyDeviceList(r.requestId, Collections.emptySet()));
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
