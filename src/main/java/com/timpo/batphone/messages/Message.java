package com.timpo.batphone.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.timpo.batphone.other.Utils;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(Include.NON_DEFAULT)
public class Message {

    private List<String> to;
    //
    private List<String> from;
    //
    @JsonProperty("req")
    private String requestID;
    //
    private Map<String, Object> data;

    public Message() {
//        id = Utils.uniqueID();
        to = Lists.newArrayList();
        from = Lists.newArrayList();
        requestID = "";
        data = Maps.newHashMap();
    }

    @JsonIgnore
    public void setTimeToNow() {
//        time = System.nanoTime();
    }

    public void setFrom(String... from) {
        this.from = Lists.newArrayList(from);
    }

    public void setTo(String... to) {
        this.to = Lists.newArrayList(to);
    }

    @JsonIgnore
    public <T> T dataAs(Class<T> klass) {
        return Utils.JSON.convertValue(data, klass);
    }

    //<editor-fold defaultstate="collapsed" desc="generated">
    public List<String> getFrom() {
        return from;
    }

//    public void setFrom(List<String> from) {
//        this.from = from;
//    }
    public List<String> getTo() {
        return to;
    }

//    public void setTo(List<String> to) {
//        this.to = to;
//    }
    public String getRequestID() {
        return requestID;
    }

    public void setRequestID(String requestID) {
        this.requestID = requestID;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.from);
        hash = 37 * hash + Objects.hashCode(this.to);
        hash = 37 * hash + Objects.hashCode(this.requestID);
        hash = 37 * hash + Objects.hashCode(this.data);
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
        if (!Objects.equals(this.from, other.from)) {
            return false;
        }
        if (!Objects.equals(this.to, other.to)) {
            return false;
        }
        if (!Objects.equals(this.requestID, other.requestID)) {
            return false;
        }
        if (!Objects.equals(this.data, other.data)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Message{" + "from=" + from + ", to=" + to + ", requestID=" + requestID + ", data=" + data + '}';
    }
    //</editor-fold>
}
