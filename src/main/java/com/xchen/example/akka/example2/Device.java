package com.xchen.example.akka.example2;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.xchen.example.akka.example2.model.*;
import com.xchen.example.akka.example2.model.command.Command;
import com.xchen.example.akka.example2.model.command.DeviceReadTemperature;
import com.xchen.example.akka.example2.model.command.DeviceRecordTemperature;
import com.xchen.example.akka.example2.model.command.DeviceStopped;

import java.util.Optional;

public class Device extends AbstractBehavior<Command> {

    private final String groupId;
    private final String deviceId;
    private Optional<Double> lastTemperatureReading = Optional.empty();

    private Device(ActorContext<Command> context, String groupId, String deviceId) {
        super(context);
        this.groupId = groupId;
        this.deviceId = deviceId;

        getContext().getLog().info("Device actor {}-{} started", groupId, deviceId);
    }

    public static Behavior<Command> create(String groupId, String deviceId) {
        return Behaviors.setup(context -> new Device(context, groupId, deviceId));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(DeviceReadTemperature.class, this::onReadTemperature)
                .onMessage(DeviceRecordTemperature.class, this::onRecordTemperature)
                .onMessage(DeviceStopped.class, m -> Behaviors.stopped())
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<Command> onRecordTemperature(DeviceRecordTemperature r) {
        getContext().getLog().info("Record temperature reading {} with {}", r.value(), r.requestId());
        lastTemperatureReading = Optional.of(r.value());
        r.replyTo().tell(new DeviceTemperatureRecorded(r.requestId()));
        return this;
    }

    private Behavior<Command> onPostStop() {
        getContext().getLog().info("Device actor {}-{} stopped", groupId, deviceId);
        return Behaviors.stopped();
    }

    private Behavior<Command> onReadTemperature(DeviceReadTemperature r) {
        r.replyTo().tell(new DeviceRespondTemperature(r.requestId(), deviceId, lastTemperatureReading));
        return this;
    }
}
