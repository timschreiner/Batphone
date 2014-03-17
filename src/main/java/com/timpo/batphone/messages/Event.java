package com.timpo.batphone.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.Objects;

@JsonInclude(Include.NON_DEFAULT)
public class Event extends Message {

    private String from;

    public Event() {
        from = "";
    }

    @Override
    public String toString() {
        return "Message{" + "from=" + from + ", data=" + super.getData() + '}';
    }

    //<editor-fold defaultstate="collapsed" desc="generated">
    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + Objects.hashCode(this.from);
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
        final Event other = (Event) obj;
        if (!Objects.equals(this.from, other.from)) {
            return false;
        }
        return true;
    }
    //</editor-fold>
}
