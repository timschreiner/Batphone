package com.timpo.batphone.handlers;

import com.timpo.batphone.messages.Message;

public interface EventHandler {

    public void handle(Message event, String channel);
}
