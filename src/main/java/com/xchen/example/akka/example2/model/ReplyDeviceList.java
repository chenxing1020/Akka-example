package com.xchen.example.akka.example2.model;

import java.util.Set;

public record ReplyDeviceList(long requestId, Set<String>ids) {
}
