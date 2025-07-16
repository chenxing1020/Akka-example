package com.xchen.example.akka.example2.model.command;

import com.xchen.example.akka.example2.model.DeviceRespondTemperature;

public record DeviceGroupQueryRespondTemperature(DeviceRespondTemperature response) implements Command {
}
