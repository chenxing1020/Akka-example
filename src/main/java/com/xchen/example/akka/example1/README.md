参考文档：[Akka:Actor Architecture](https://doc.akka.io/libraries/akka-core/current/typed/guide/tutorial_1.html)

* 测试场景：actor1是actor2的父actor，并配置actor1为actor2的supervisor，同时配置actor2异常自动重启
* 测试案例1：通过`failChild`命令触发actor2异常，观察actor2异常重启
* 测试案例2：通过`stop`命令触发actor1的`Behaviors.stopped`，验证PostStop先停止actor2，后停止actor1