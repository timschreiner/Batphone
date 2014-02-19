package com.timpo.batphone.other;

public class Complex extends Simple {

    protected double c;

    public Complex() {
    }

    public double getC() {
        return c;
    }

    public void setC(double c) {
        this.c = c;
    }

    public Complex(String a, long b, double c) {
        super(a, b);
        this.c = c;
    }

    @Override
    public String toString() {
        return "Complex{" + "a=" + a + ", b=" + b + ", c=" + c + '}';
    }
}