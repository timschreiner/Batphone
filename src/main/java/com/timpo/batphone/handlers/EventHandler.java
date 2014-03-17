package com.timpo.batphone.handlers;

import com.timpo.batphone.messages.Event;

public interface EventHandler {

    public void handle(Event event, String topic);
}
