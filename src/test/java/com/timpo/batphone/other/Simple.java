package com.timpo.batphone.other;

public class Simple {

    protected String a;
    protected long b;

    public Simple(String a, long b) {
        this.a = a;
        this.b = b;
    }

    public String getA() {
        return a;
    }

    public void setA(String a) {
        this.a = a;
    }

    public long getB() {
        return b;
    }

    public void setB(long b) {
        this.b = b;
    }

    public Simple() {
    }

    @Override
    public String toString() {
        return "Simple{" + "a=" + a + ", b=" + b + '}';
    }
}