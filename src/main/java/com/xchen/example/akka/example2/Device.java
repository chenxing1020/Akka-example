package com.xchen.example.akka.example2;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Optional;

public class Device extends AbstractBehavior<Device.Command> {

    public interface Command {}

    private final String groupId;
    private final String deviceId;
    private Optional<Double> lastTemperatureReading = Optional.empty();

    private Device(ActorContext<Device.Command> context, String groupId, String deviceId) {
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
                .onMessage(Passivate.class, m -> Behaviors.stopped())
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<Command> onRecordTemperature(RecordTemperature r) {
        getContext().getLog().info("Record temperature reading {} with {}", r.value, r.requestId);
        lastTemperatureReading = Optional.of(r.value);
        r.replyTo.tell(new TemperatureRecorded(r.requestId));
        return this;
    }

    private Behavior<Command> onPostStop() {
        getContext().getLog().info("Device actor {}-{} stopped", groupId, deviceId);
        return Behaviors.stopped();
    }

    private Behavior<Command> onReadTemperature(ReadTemperature r) {
        r.replyTo.tell(new RespondTemperature(r.requestId, lastTemperatureReading));
        return this;
    }

    public static class ReadTemperature implements Device.Command {
        final long requestId;
        final ActorRef<RespondTemperature> replyTo;

        public ReadTemperature(long requestId, ActorRef<RespondTemperature> replyTo) {
            this.requestId = requestId;
            this.replyTo = replyTo;
        }
    }

    public static class RecordTemperature implements Device.Command {
        final double value;
        final long requestId;
        final ActorRef<TemperatureRecorded> replyTo;
        public RecordTemperature(long requestId, double value, ActorRef<TemperatureRecorded> replyTo) {
            this.requestId = requestId;
            this.value = value;
            this.replyTo = replyTo;
        }
    }

    public static class RespondTemperature {
        final long requestId;
        final Optional<Double> value;
        public RespondTemperature(long requestId, Optional<Double> value) {
            this.requestId = requestId;
            this.value = value;
        }
    }

    public static class TemperatureRecorded {
        final long requestId;
        public TemperatureRecorded(long requestId) {
            this.requestId = requestId;
        }
    }

    public enum Passivate implements Command {
        INSTANCE
    }
}
