package com.xchen.example.akka.example1;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class TestActor1 extends AbstractBehavior<String> {

    static Behavior<String> create() {
        return Behaviors.setup(TestActor1::new);
    }

    private final ActorRef<String> child;

    private TestActor1(ActorContext<String> context) {
        super(context);
        System.out.println("actor1 started");
        child = context.spawn(
                Behaviors.supervise(TestActor2.create())
                        .onFailure(SupervisorStrategy.restart()),
                "actor2");
    }

    @Override
    public Receive<String> createReceive() {
        return newReceiveBuilder()
                .onMessageEquals("stop", Behaviors::stopped)
                .onMessageEquals("failChild", this::onFailChild)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<String> onFailChild() {
        child.tell("fail");
        return this;
    }

    private Behavior<String> onPostStop() {
        System.out.println("actor1 stopped");
        return this;
    }
}
