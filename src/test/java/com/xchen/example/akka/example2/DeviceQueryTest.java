package com.xchen.example.akka.example2;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import com.xchen.example.akka.example2.model.DeviceRespondTemperature;
import com.xchen.example.akka.example2.model.RespondAllTemperatures;
import com.xchen.example.akka.example2.model.TemperatureReading;
import com.xchen.example.akka.example2.model.command.Command;
import com.xchen.example.akka.example2.model.command.DeviceGroupQueryRespondTemperature;
import com.xchen.example.akka.example2.model.command.DeviceReadTemperature;
import org.junit.ClassRule;
import org.junit.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class DeviceQueryTest {

    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource();

    @Test
    public void testReturnTemperatureValueForWorkingDevices() {
        TestProbe<RespondAllTemperatures> requester =
                testKit.createTestProbe(RespondAllTemperatures.class);
        TestProbe<Command> device1 = testKit.createTestProbe(Command.class);
        TestProbe<Command> device2 = testKit.createTestProbe(Command.class);

        Map<String, ActorRef<Command>> deviceIdToActor = new HashMap<>();
        deviceIdToActor.put("device1", device1.getRef());
        deviceIdToActor.put("device2", device2.getRef());

        ActorRef<Command> queryActor =
                testKit.spawn(
                        DeviceGroupQuery.create(
                                deviceIdToActor, 1L, requester.getRef(), Duration.ofSeconds(3)
                        )
                );
        device1.expectMessageClass(DeviceReadTemperature.class);
        device2.expectMessageClass(DeviceReadTemperature.class);

        queryActor.tell(
                new DeviceGroupQueryRespondTemperature(
                        new DeviceRespondTemperature(0L, "device1", Optional.of(1.0))
                )
        );

        queryActor.tell(
                new DeviceGroupQueryRespondTemperature(
                        new DeviceRespondTemperature(0L, "device2", Optional.of(2.0))
                )
        );
        
        RespondAllTemperatures response = requester.receiveMessage();
        assertEquals(1L, response.requestId());

        Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", new TemperatureReading.Temperature(1.0));
        expectedTemperatures.put("device2", new TemperatureReading.Temperature(2.0));

        assertEquals(expectedTemperatures, response.temperatures());
    }

    @Test
    public void testReturnTemperatureNotAvailableForDevicesWithNoReadings() {
        TestProbe<RespondAllTemperatures> requester =
                testKit.createTestProbe(RespondAllTemperatures.class);

        TestProbe<Command> device1 = testKit.createTestProbe(Command.class);
        TestProbe<Command> device2 = testKit.createTestProbe(Command.class);

        Map<String, ActorRef<Command>> deviceIdToActor = new HashMap<>();
        deviceIdToActor.put("device1", device1.getRef());
        deviceIdToActor.put("device2", device2.getRef());

        ActorRef<Command> queryActor =
                testKit.spawn(
                        DeviceGroupQuery.create(
                                deviceIdToActor, 1L, requester.getRef(), Duration.ofSeconds(3)
                        )
                );

        assertEquals(0L, device1.expectMessageClass(DeviceReadTemperature.class).requestId());
        assertEquals(0L, device2.expectMessageClass(DeviceReadTemperature.class).requestId());

        queryActor.tell(
                new DeviceGroupQueryRespondTemperature(
                        new DeviceRespondTemperature(0L, "device1", Optional.empty()
                        ))
        );

        queryActor.tell(
                new DeviceGroupQueryRespondTemperature(
                        new DeviceRespondTemperature(0L, "device2", Optional.of(2.0))
                )
        );

        RespondAllTemperatures response = requester.receiveMessage();
        assertEquals(1L, response.requestId());

        Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", TemperatureReading.TemperatureNotAvailable.INSTANCE);
        expectedTemperatures.put("device2", new TemperatureReading.Temperature(2.0));
        assertEquals(expectedTemperatures, response.temperatures());
    }

    @Test
    public void testReturnDeviceNotAvailableIfDeviceStopsBeforeAnswering() {
        TestProbe<RespondAllTemperatures> requester =
                testKit.createTestProbe(RespondAllTemperatures.class);

        TestProbe<Command> device1 = testKit.createTestProbe(Command.class);
        TestProbe<Command> device2 = testKit.createTestProbe(Command.class);

        Map<String, ActorRef<Command>> deviceIdToActor = new HashMap<>();
        deviceIdToActor.put("device1", device1.getRef());
        deviceIdToActor.put("device2", device2.getRef());
        ActorRef<Command> queryActor =
                testKit.spawn(
                        DeviceGroupQuery.create(
                                deviceIdToActor, 1L, requester.getRef(), Duration.ofSeconds(3)
                        )
                );

        assertEquals(0L, device1.expectMessageClass(DeviceReadTemperature.class).requestId());
        assertEquals(0L, device2.expectMessageClass(DeviceReadTemperature.class).requestId());

        queryActor.tell(new DeviceGroupQueryRespondTemperature(
                new DeviceRespondTemperature(0L, "device1", Optional.of(1.0))
        ));

        device2.stop();

        RespondAllTemperatures response = requester.receiveMessage();
        assertEquals(1L, response.requestId());

        Map<String, TemperatureReading> expectedTempreatures = new HashMap<>();
        expectedTempreatures.put("device1", new TemperatureReading.Temperature(1.0));
        expectedTempreatures.put("device2", TemperatureReading.DeviceNotAvailable.INSTANCE);
        assertEquals(expectedTempreatures, response.temperatures());
    }

    @Test
    public void testReturnDeviceNotAvailableIfDeviceStopsAfterAnswering() {
        TestProbe<RespondAllTemperatures> requester =
                testKit.createTestProbe(RespondAllTemperatures.class);

        TestProbe<Command> device1 = testKit.createTestProbe(Command.class);
        TestProbe<Command> device2 = testKit.createTestProbe(Command.class);

        Map<String, ActorRef<Command>> deviceIdToActor = new HashMap<>();
        deviceIdToActor.put("device1", device1.getRef());
        deviceIdToActor.put("device2", device2.getRef());
        ActorRef<Command> queryActor =
                testKit.spawn(
                        DeviceGroupQuery.create(
                                deviceIdToActor, 1L, requester.getRef(), Duration.ofSeconds(3)
                        )
                );

        assertEquals(0L, device1.expectMessageClass(DeviceReadTemperature.class).requestId());
        assertEquals(0L, device2.expectMessageClass(DeviceReadTemperature.class).requestId());

        queryActor.tell(new DeviceGroupQueryRespondTemperature(
                new DeviceRespondTemperature(0L, "device1", Optional.of(1.0))
        ));

        queryActor.tell(new DeviceGroupQueryRespondTemperature(
                new DeviceRespondTemperature(0L, "device2", Optional.of(2.0))
        ));

        device2.stop();

        RespondAllTemperatures response = requester.receiveMessage();
        assertEquals(1L, response.requestId());

        Map<String, TemperatureReading> expectedTempreatures = new HashMap<>();
        expectedTempreatures.put("device1", new TemperatureReading.Temperature(1.0));
        expectedTempreatures.put("device2", new TemperatureReading.Temperature(2.0));
        assertEquals(expectedTempreatures, response.temperatures());
    }

    @Test
    public void testReturnDeviceNotAvailableIfDeviceDoesNotAnswerInTime() {
        TestProbe<RespondAllTemperatures> requester =
                testKit.createTestProbe(RespondAllTemperatures.class);

        TestProbe<Command> device1 = testKit.createTestProbe(Command.class);
        TestProbe<Command> device2 = testKit.createTestProbe(Command.class);

        Map<String, ActorRef<Command>> deviceIdToActor = new HashMap<>();
        deviceIdToActor.put("device1", device1.getRef());
        deviceIdToActor.put("device2", device2.getRef());
        ActorRef<Command> queryActor =
                testKit.spawn(
                        DeviceGroupQuery.create(
                                deviceIdToActor, 1L, requester.getRef(), Duration.ofMillis(200)
                        )
                );

        assertEquals(0L, device1.expectMessageClass(DeviceReadTemperature.class).requestId());
        assertEquals(0L, device2.expectMessageClass(DeviceReadTemperature.class).requestId());

        queryActor.tell(new DeviceGroupQueryRespondTemperature(
                new DeviceRespondTemperature(0L, "device1", Optional.of(1.0))
        ));

        // no reply from device2

        RespondAllTemperatures response = requester.receiveMessage();
        assertEquals(1L, response.requestId());

        Map<String, TemperatureReading> expectedTempreatures = new HashMap<>();
        expectedTempreatures.put("device1", new TemperatureReading.Temperature(1.0));
        expectedTempreatures.put("device2", TemperatureReading.DeviceTimedOut.INSTANCE);
        assertEquals(expectedTempreatures, response.temperatures());
    }
}
