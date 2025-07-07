package com.xchen.example.akka.example2;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.xchen.example.akka.example2.model.*;

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
                .onMessage(ReadTemperature.class, this::onReadTemperature)
                .onMessage(RecordTemperature.class, this::onRecordTemperature)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<Command> onRecordTemperature(RecordTemperature r) {
        getContext().getLog().info("Record temperature reading {} with {}", r.getValue(), r.getRequestId());
        lastTemperatureReading = Optional.of(r.getValue());
        r.getReplyTo().tell(new TemperatureRecorded(r.getRequestId()));
        return this;
    }

    private Behavior<Command> onPostStop() {
        getContext().getLog().info("Device actor {}-{} stopped", groupId, deviceId);
        return this;
    }

    private Behavior<Command> onReadTemperature(ReadTemperature r) {
        r.getReplyTo().tell(new RespondTemperature(r.getRequestId(), lastTemperatureReading));
        return this;
    }
}
