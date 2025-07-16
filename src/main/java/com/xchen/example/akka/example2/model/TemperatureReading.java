package com.xchen.example.akka.example2.model;

public interface TemperatureReading {


    record Temperature(double value) implements TemperatureReading {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Temperature that = (Temperature) o;
            return Double.compare(that.value, value) == 0;
        }

        @Override
        public String toString() {
            return "Temperature{" + "value=" + value + '}';
        }
    }

    enum TemperatureNotAvailable implements TemperatureReading {
        INSTANCE
    }

     enum DeviceNotAvailable implements TemperatureReading {
        INSTANCE
    }

    enum DeviceTimedOut implements TemperatureReading {
        INSTANCE
    }
}
