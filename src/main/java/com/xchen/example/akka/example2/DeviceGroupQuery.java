package com.xchen.example.akka.example2;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import com.xchen.example.akka.example2.model.*;

import java.time.Duration;
import java.util.*;

public class DeviceGroupQuery extends AbstractBehavior<Command> {

    public static Behavior<Command> create(
            Map<String, ActorRef<Command>> deviceIdToActor,
            long requestId,
            ActorRef<DeviceManager.RespondAllTemperatures> requester,
            Duration timeout) {
        return Behaviors.setup(context -> Behaviors.withTimers(
                timers ->
                        new DeviceGroupQuery(deviceIdToActor, requestId, requester, timeout, context, timers)
        ));
    }

    private final long requestId;
    private final ActorRef<DeviceManager.RespondAllTemperatures> requester;

    private DeviceGroupQuery(Map<String, ActorRef<Command>> deviceIdToActor,
                             long requestId,
                             ActorRef<DeviceManager.RespondAllTemperatures> requester,
                             Duration timeout,
                             ActorContext<Command> context,
                             TimerScheduler<Command> timers) {
        super(context);
        this.requestId = requestId;
        this.requester = requester;

        timers.startSingleTimer(DeviceGroupQueryCollectionTimeout.INSTANCE, timeout);

        ActorRef<DeviceRespondTemperature> respondTemperatureAdapter =
                context.messageAdapter(DeviceRespondTemperature.class, DeviceGroupQueryRespondTemperature::new);
        for (Map.Entry<String, ActorRef<com.xchen.example.akka.example2.model.Command>> entry : deviceIdToActor.entrySet()) {
            context.watchWith(entry.getValue(), new DeviceGroupQueryDeviceTerminated(entry.getKey()));
            entry.getValue().tell(new DeviceReadTemperature(0L, respondTemperatureAdapter));
        }
        stillWaiting = new HashSet<>(deviceIdToActor.keySet());
    }

    private Map<String, DeviceManager.TemperatureReading> repliesSoFar = new HashMap<>();
    private final Set<String> stillWaiting;

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(DeviceGroupQueryRespondTemperature.class, this::onRespondTemperature)
                .onMessage(DeviceGroupQueryDeviceTerminated.class, this::onDeviceTerminated)
                .onMessage(DeviceGroupQueryCollectionTimeout.class, this::onCollectionTimeout)
                .build();
    }

    private Behavior<Command> onCollectionTimeout(DeviceGroupQueryCollectionTimeout timeout) {
        for (String deviceId : stillWaiting) {
            repliesSoFar.put(deviceId, DeviceManager.DeviceTimedOut.INSTANCE);
        }
        stillWaiting.clear();
        return respondWhenAllCollected();
    }

    private Behavior<Command> onDeviceTerminated(DeviceGroupQueryDeviceTerminated terminated) {
        if (stillWaiting.contains(terminated.deviceId())) {
            repliesSoFar.put(terminated.deviceId(), DeviceManager.DeviceNotAvailable.INSTANCE);
            stillWaiting.remove(terminated.deviceId());
        }
        return respondWhenAllCollected();
    }

    private Behavior<Command> onRespondTemperature(DeviceGroupQueryRespondTemperature r) {
        DeviceManager.TemperatureReading reading =
                r.response()
                        .value()
                        .map(v -> (DeviceManager.TemperatureReading) new DeviceManager.Temperature(v))
                        .orElse(DeviceManager.TemperatureNotAvailable.INSTANCE);
        String deviceId = r.response().deviceId();
        repliesSoFar.put(deviceId, reading);
        stillWaiting.remove(deviceId);
        return respondWhenAllCollected();
    }

    private Behavior<Command> respondWhenAllCollected() {
        if (stillWaiting.isEmpty()) {
            requester.tell(new DeviceManager.RespondAllTemperatures(requestId, repliesSoFar));
            return Behaviors.stopped();
        } else {
            return this;
        }
    }
}
