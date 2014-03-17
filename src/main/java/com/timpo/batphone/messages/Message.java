package com.timpo.batphone.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.Maps;
import com.timpo.batphone.other.Utils;
import java.util.Map;
import java.util.Objects;

@JsonInclude(Include.NON_DEFAULT)
public abstract class Message {

    private Map<String, Object> data;

    public Message() {
        data = Maps.newHashMap();
    }

    @JsonIgnore
    public void setTimeToNow() {
//        time = Utils.timestamp();
    }

    @JsonIgnore
    public <T> T dataAs(Class<T> klass) {
        return Utils.JSON.convertValue(data, klass);
    }

    //<editor-fold defaultstate="collapsed" desc="generated">
    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Objects.hashCode(this.data);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Message other = (Message) obj;
        if (!Objects.equals(this.data, other.data)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Message{" + "data=" + data + '}';
    }
    //</editor-fold>
}
