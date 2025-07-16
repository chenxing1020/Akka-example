package com.xchen.example.akka.example2;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import com.xchen.example.akka.example2.model.DeviceRespondTemperature;
import com.xchen.example.akka.example2.model.RespondAllTemperatures;
import com.xchen.example.akka.example2.model.TemperatureReading;
import com.xchen.example.akka.example2.model.command.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DeviceGroupQuery extends AbstractBehavior<Command> {

    public static Behavior<Command> create(
            Map<String, ActorRef<Command>> deviceIdToActor,
            long requestId,
            ActorRef<RespondAllTemperatures> requester,
            Duration timeout) {
        return Behaviors.setup(context -> Behaviors.withTimers(
                timers ->
                        new DeviceGroupQuery(deviceIdToActor, requestId, requester, timeout, context, timers)
        ));
    }

    private final long requestId;
    private final ActorRef<RespondAllTemperatures> requester;

    private DeviceGroupQuery(Map<String, ActorRef<Command>> deviceIdToActor,
                             long requestId,
                             ActorRef<RespondAllTemperatures> requester,
                             Duration timeout,
                             ActorContext<Command> context,
                             TimerScheduler<Command> timers) {
        super(context);
        this.requestId = requestId;
        this.requester = requester;

        timers.startSingleTimer(DeviceGroupQueryCollectionTimeout.INSTANCE, timeout);

        ActorRef<DeviceRespondTemperature> respondTemperatureAdapter =
                context.messageAdapter(DeviceRespondTemperature.class, DeviceGroupQueryRespondTemperature::new);
        for (Map.Entry<String, ActorRef<Command>> entry : deviceIdToActor.entrySet()) {
            context.watchWith(entry.getValue(), new DeviceGroupQueryDeviceTerminated(entry.getKey()));
            entry.getValue().tell(new DeviceReadTemperature(0L, respondTemperatureAdapter));
        }
        stillWaiting = new HashSet<>(deviceIdToActor.keySet());
    }

    private final Map<String, TemperatureReading> repliesSoFar = new HashMap<>();
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
            repliesSoFar.put(deviceId, TemperatureReading.DeviceTimedOut.INSTANCE);
        }
        stillWaiting.clear();
        return respondWhenAllCollected();
    }

    private Behavior<Command> onDeviceTerminated(DeviceGroupQueryDeviceTerminated terminated) {
        if (stillWaiting.contains(terminated.deviceId())) {
            repliesSoFar.put(terminated.deviceId(), TemperatureReading.DeviceNotAvailable.INSTANCE);
            stillWaiting.remove(terminated.deviceId());
        }
        return respondWhenAllCollected();
    }

    private Behavior<Command> onRespondTemperature(DeviceGroupQueryRespondTemperature r) {
        TemperatureReading reading =
                r.response()
                        .value()
                        .map(v -> (TemperatureReading) new TemperatureReading.Temperature(v))
                        .orElse(TemperatureReading.TemperatureNotAvailable.INSTANCE);
        String deviceId = r.response().deviceId();
        repliesSoFar.put(deviceId, reading);
        stillWaiting.remove(deviceId);
        return respondWhenAllCollected();
    }

    private Behavior<Command> respondWhenAllCollected() {
        if (stillWaiting.isEmpty()) {
            requester.tell(new RespondAllTemperatures(requestId, repliesSoFar));
            return Behaviors.stopped();
        } else {
            return this;
        }
    }
}
