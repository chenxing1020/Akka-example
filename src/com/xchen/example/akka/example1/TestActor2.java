package com.xchen.example.akka.example1;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.PreRestart;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class TestActor2 extends AbstractBehavior<String> {

    static Behavior<String> create() {
        return Behaviors.setup(TestActor2::new);
    }

    private TestActor2(ActorContext<String> context) {
        super(context);
        System.out.println("actor2 started");
    }

    @Override
    public Receive<String> createReceive() {
        return newReceiveBuilder()
                .onMessageEquals("fail", this::fail)
                .onSignal(PreRestart.class, signal -> preRestart())
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<String> fail() {
        System.out.println("actor2 fails now");
        throw new RuntimeException("actor2 failed");
    }

    private Behavior<String> preRestart() {
        System.out.println("actor2 restarted");
        return this;
    }

    private Behavior<String> onPostStop() {
        System.out.println("actor2 stopped");
        return this;
    }
}
