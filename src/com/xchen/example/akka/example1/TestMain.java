package com.xchen.example.akka.example1;


import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;

public class TestMain {

    public static void main(String[] args) {
        // 测试场景：actor1是actor2的父actor，并配置actor1为actor2的supervisor，同时配置actor2异常自动重启
        ActorRef<String> testActor1 = ActorSystem.create(TestActor1.create(), "Test");
        // 场景1:通过“failChild”命令触发actor2异常，观察actor2异常重启
        testActor1.tell("failChild");

        // 场景2:通过“stop”命令触发actor1的Behaviors.stopped，验证PostStop先停止actor2，后停止actor1
        testActor1.tell("stop");
    }
}
