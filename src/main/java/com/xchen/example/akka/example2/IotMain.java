package com.xchen.example.akka.example2;

import akka.actor.typed.ActorSystem;

public class IotMain {

    public static void main(String[] args) {
        ActorSystem.create(IotSupervisor.create(), "iot-system");
    }
}
