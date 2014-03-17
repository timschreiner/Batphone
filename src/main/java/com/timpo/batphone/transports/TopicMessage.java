package com.timpo.batphone.transports;

import com.timpo.batphone.messages.Message;

public class TopicMessage<M extends Message> {

    private final String topic;
    private final M message;

    //<editor-fold defaultstate="collapsed" desc="generated-code">
    public TopicMessage(String topic, M message) {
        this.topic = topic;
        this.message = message;
    }

    public String getTopic() {
        return topic;
    }

    public M getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "TopicMessage{" + "topic=" + topic + ", message=" + message + '}';
    }
    //</editor-fold>
}
