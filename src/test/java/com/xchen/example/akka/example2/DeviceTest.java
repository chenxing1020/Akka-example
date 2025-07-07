package com.xchen.example.akka.example2;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import com.xchen.example.akka.example2.model.*;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class DeviceTest {

    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource();

    @Test
    public void testReplyWithEmptyReadingIfNoTemperatureIsKnown() {
        TestProbe<RespondTemperature> probe = testKit.createTestProbe(RespondTemperature.class);
        ActorRef<Command> deviceActor = testKit.spawn(Device.create("group", "device"));
        deviceActor.tell(new ReadTemperature(42L, probe.getRef()));
        RespondTemperature response = probe.receiveMessage();
        assertEquals(42L, response.getRequestId());
        assertEquals(Optional.empty(), response.getValue());

    }

    @Test
    public void testReplyWithLatestTemperatureReading() {
        TestProbe<TemperatureRecorded> recordProbe = testKit.createTestProbe(TemperatureRecorded.class);
        TestProbe<RespondTemperature> readProbe = testKit.createTestProbe(RespondTemperature.class);
        ActorRef<Command> deviceActor = testKit.spawn(Device.create("group", "device"));

        deviceActor.tell(new RecordTemperature(1L, 24.0, recordProbe.getRef()));
        assertEquals(1L, recordProbe.receiveMessage().getRequestId());

        deviceActor.tell(new ReadTemperature(2L, readProbe.getRef()));
        RespondTemperature response1 = readProbe.receiveMessage();
        assertEquals(2L, response1.getRequestId());
        assertEquals(Optional.of(24.0), response1.getValue());

        deviceActor.tell(new RecordTemperature(3L, 55.0, recordProbe.getRef()));
        assertEquals(3L, recordProbe.receiveMessage().getRequestId());

        deviceActor.tell(new ReadTemperature(4L, readProbe.getRef()));
        RespondTemperature response2 = readProbe.receiveMessage();
        assertEquals(4L, response2.getRequestId());
        assertEquals(Optional.of(55.0), response2.getValue());
    }
}
