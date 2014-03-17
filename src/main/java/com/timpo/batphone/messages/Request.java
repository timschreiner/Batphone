package com.timpo.batphone.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;

@JsonInclude(Include.NON_DEFAULT)
public class Request extends Message {

    private List<String> to;
    //
    private List<String> from;
    //
    @JsonProperty("req")
    private String requestID;

    public Request() {
        to = Lists.newArrayList();
        from = Lists.newArrayList();
        requestID = "";
    }

    public void setFrom(String... from) {
        this.from = Lists.newArrayList(from);
    }

    public void setTo(String... to) {
        this.to = Lists.newArrayList(to);
    }

    @Override
    public String toString() {
        return "Message{" + "from=" + from + ", to=" + to + ", requestID=" + requestID + ", data=" + super.getData() + '}';
    }

    //<editor-fold defaultstate="collapsed" desc="generated">
    public List<String> getFrom() {
        return from;
    }

    public List<String> getTo() {
        return to;
    }

    public String getRequestID() {
        return requestID;
    }

    public void setRequestID(String requestID) {
        this.requestID = requestID;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + Objects.hashCode(this.to);
        hash = 67 * hash + Objects.hashCode(this.from);
        hash = 67 * hash + Objects.hashCode(this.requestID);
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
        final Request other = (Request) obj;
        if (!Objects.equals(this.to, other.to)) {
            return false;
        }
        if (!Objects.equals(this.from, other.from)) {
            return false;
        }
        if (!Objects.equals(this.requestID, other.requestID)) {
            return false;
        }
        return true;
    }
    //</editor-fold>
}
