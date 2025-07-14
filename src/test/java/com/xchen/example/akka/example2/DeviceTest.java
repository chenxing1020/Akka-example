package com.xchen.example.akka.example2;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import com.xchen.example.akka.example2.model.*;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class DeviceTest {

    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource();

    @Test
    public void testReplyWithEmptyReadingIfNoTemperatureIsKnown() {
        TestProbe<DeviceRespondTemperature> probe = testKit.createTestProbe(DeviceRespondTemperature.class);
        ActorRef<Command> deviceActor = testKit.spawn(Device.create("group", "device"));
        deviceActor.tell(new DeviceReadTemperature(42L, probe.getRef()));
        DeviceRespondTemperature response = probe.receiveMessage();
        assertEquals(42L, response.requestId());
        assertEquals(Optional.empty(), response.value());

    }

    @Test
    public void testReplyWithLatestTemperatureReading() {
        TestProbe<DeviceTemperatureRecorded> recordProbe = testKit.createTestProbe(DeviceTemperatureRecorded.class);
        TestProbe<DeviceRespondTemperature> readProbe = testKit.createTestProbe(DeviceRespondTemperature.class);
        ActorRef<Command> deviceActor = testKit.spawn(Device.create("group", "device"));

        deviceActor.tell(new DeviceRecordTemperature(1L, 24.0, recordProbe.getRef()));
        assertEquals(1L, recordProbe.receiveMessage().requestId());

        deviceActor.tell(new DeviceReadTemperature(2L, readProbe.getRef()));
        DeviceRespondTemperature response1 = readProbe.receiveMessage();
        assertEquals(2L, response1.requestId());
        assertEquals(Optional.of(24.0), response1.value());

        deviceActor.tell(new DeviceRecordTemperature(3L, 55.0, recordProbe.getRef()));
        assertEquals(3L, recordProbe.receiveMessage().requestId());

        deviceActor.tell(new DeviceReadTemperature(4L, readProbe.getRef()));
        DeviceRespondTemperature response2 = readProbe.receiveMessage();
        assertEquals(4L, response2.requestId());
        assertEquals(Optional.of(55.0), response2.value());
    }

    @Test
    public void testReplyToRegistrationRequests() {
        TestProbe<DeviceManager.DeviceRegistered> probe = testKit.createTestProbe(DeviceManager.DeviceRegistered.class);
        ActorRef<Command> groupActor = testKit.spawn(DeviceGroup.create("group"));

        groupActor.tell(new DeviceManagerRequestTrackDevice("group", "device", probe.getRef()));
        DeviceManager.DeviceRegistered registered1 = probe.receiveMessage();

        // another deviceId
        groupActor.tell(new DeviceManagerRequestTrackDevice("group", "device3", probe.getRef()));
        DeviceManager.DeviceRegistered registered2 = probe.receiveMessage();
        assertNotEquals(registered1.device, registered2.device);

        // Check that the device actors are working
        TestProbe<DeviceTemperatureRecorded> recordProbe = testKit.createTestProbe(DeviceTemperatureRecorded.class);
        registered1.device.tell(new DeviceRecordTemperature(0L, 1.0, recordProbe.getRef()));
        assertEquals(0L, recordProbe.receiveMessage().requestId());
        registered2.device.tell(new DeviceRecordTemperature(1L, 2.0, recordProbe.getRef()));
        assertEquals(1L, recordProbe.receiveMessage().requestId());
    }

    @Test
    public void testIgnoreWrongRegistrationRequests() {
        TestProbe<DeviceManager.DeviceRegistered> probe = testKit.createTestProbe(DeviceManager.DeviceRegistered.class);
        ActorRef<Command> groupActor = testKit.spawn(DeviceGroup.create("group"));
        groupActor.tell(new DeviceManagerRequestTrackDevice("wrongGroup", "device1", probe.getRef()));
        probe.expectNoMessage();
    }

    @Test
    public void testReturnSameActorForSameDeviceId() {
        TestProbe<DeviceManager.DeviceRegistered> probe = testKit.createTestProbe(DeviceManager.DeviceRegistered.class);
        ActorRef<Command> groupActor = testKit.spawn(DeviceGroup.create("group"));

        groupActor.tell(new DeviceManagerRequestTrackDevice("group", "device", probe.getRef()));
        DeviceManager.DeviceRegistered registered1 = probe.receiveMessage();

        // registering same again should be idempotent
        groupActor.tell(new DeviceManagerRequestTrackDevice("group", "device", probe.getRef()));
        DeviceManager.DeviceRegistered registered2 = probe.receiveMessage();
        assertEquals(registered1.device, registered2.device);
    }

    @Test
    public void testListActiveDevices() {
        TestProbe<DeviceManager.DeviceRegistered> registeredProbe = testKit.createTestProbe(DeviceManager.DeviceRegistered.class);
        ActorRef<Command> groupActor = testKit.spawn(DeviceGroup.create("group"));

        groupActor.tell(new DeviceManagerRequestTrackDevice("group", "device1", registeredProbe.getRef()));
        registeredProbe.receiveMessage();

        groupActor.tell(new DeviceManagerRequestTrackDevice("group", "device2", registeredProbe.getRef()));
        registeredProbe.receiveMessage();

        TestProbe<DeviceManager.ReplyDeviceList> deviceListProbe = testKit.createTestProbe(DeviceManager.ReplyDeviceList.class);

        groupActor.tell(new DeviceManager.RequestDeviceList(0L, "group", deviceListProbe.getRef()));
        DeviceManager.ReplyDeviceList reply = deviceListProbe.receiveMessage();
        assertEquals(0L, reply.requestId);
        assertEquals(Stream.of("device1", "device2").collect(Collectors.toSet()), reply.ids);
    }

    @Test
    public void testListActiveDevicesAfterOneShutsDown() {
        TestProbe<DeviceManager.DeviceRegistered> registeredProbe = testKit.createTestProbe(DeviceManager.DeviceRegistered.class);
        ActorRef<Command> groupActor = testKit.spawn(DeviceGroup.create("group"));

        groupActor.tell(new DeviceManagerRequestTrackDevice("group", "device1", registeredProbe.getRef()));
        DeviceManager.DeviceRegistered registered1 = registeredProbe.receiveMessage();

        groupActor.tell(new DeviceManagerRequestTrackDevice("group", "device2", registeredProbe.getRef()));
        DeviceManager.DeviceRegistered registered2 = registeredProbe.receiveMessage();

        ActorRef<Command> toShutDown = registered1.device;

        TestProbe<DeviceManager.ReplyDeviceList> deviceListProbe = testKit.createTestProbe(DeviceManager.ReplyDeviceList.class);

        groupActor.tell(new DeviceManager.RequestDeviceList(0L, "group", deviceListProbe.getRef()));
        DeviceManager.ReplyDeviceList reply = deviceListProbe.receiveMessage();

        assertEquals(0L, reply.requestId);
        assertEquals(Stream.of("device1", "device2").collect(Collectors.toSet()), reply.ids);

        toShutDown.tell(DeviceStopped.INSTANCE);
        registeredProbe.expectTerminated(toShutDown, registeredProbe.getRemainingOrDefault());

        // using awaitAssert to retry because it might take longer for the groupActor
        // to see the Terminated, that order is undefined
        registeredProbe.awaitAssert(
                () -> {
                    groupActor.tell(new DeviceManager.RequestDeviceList(1L, "group", deviceListProbe.getRef()));
                    DeviceManager.ReplyDeviceList r = deviceListProbe.receiveMessage();
                    assertEquals(1L, r.requestId);
                    assertEquals(Stream.of("device2").collect(Collectors.toSet()), r.ids);
                    return null;
                }
        );
    }

    @Test
    public void testListActiveDevicesWithDeviceManager() {
        TestProbe<DeviceManager.DeviceRegistered> registeredProbe = testKit.createTestProbe(DeviceManager.DeviceRegistered.class);
        ActorRef<Command> deviceManagerActor = testKit.spawn(DeviceManager.create());
        // group
        deviceManagerActor.tell(new DeviceManagerRequestTrackDevice("group", "device1", registeredProbe.getRef()));
        DeviceManager.DeviceRegistered toShutdown = registeredProbe.receiveMessage();
        deviceManagerActor.tell(new DeviceManagerRequestTrackDevice("group","device2", registeredProbe.getRef()));
        registeredProbe.receiveMessage();
        // group1
        deviceManagerActor.tell(new DeviceManagerRequestTrackDevice("group1", "device1", registeredProbe.getRef()));
        registeredProbe.receiveMessage();
        deviceManagerActor.tell(new DeviceManagerRequestTrackDevice("group1", "device2", registeredProbe.getRef()));
        registeredProbe.receiveMessage();

        // deviceList
        TestProbe<DeviceManager.ReplyDeviceList> reply = testKit.createTestProbe(DeviceManager.ReplyDeviceList.class);
        deviceManagerActor.tell(new DeviceManager.RequestDeviceList(0L, "group", reply.getRef()));
        DeviceManager.ReplyDeviceList r = reply.receiveMessage();
        assertEquals(0L, r.requestId);
        assertEquals(Stream.of("device1", "device2").collect(Collectors.toSet()), r.ids);

        deviceManagerActor.tell(new DeviceManager.RequestDeviceList(1L, "group1", reply.getRef()));
        DeviceManager.ReplyDeviceList r1 = reply.receiveMessage();
        assertEquals(1L, r1.requestId);
        assertEquals(Stream.of("device1", "device2").collect(Collectors.toSet()), r1.ids);

        // shutdown
        toShutdown.device.tell(DeviceStopped.INSTANCE);
        registeredProbe.expectTerminated(toShutdown.device, registeredProbe.getRemainingOrDefault());

        registeredProbe.awaitAssert(
                () -> {
                    deviceManagerActor.tell(new DeviceManager.RequestDeviceList(2L, "group", reply.getRef()));
                    DeviceManager.ReplyDeviceList r3 = reply.receiveMessage();
                    assertEquals(2L, r3.requestId);
                    assertEquals(Stream.of("device2").collect(Collectors.toSet()), r3.ids);
                    return null;
                }
        );
    }
}
