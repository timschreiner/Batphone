package com.timpo.batphone.transports.redis;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class RoundRobinHolder<E> {

    private final BlockingDeque<E> holder;

    public RoundRobinHolder(E element) {
        this();
        holder.add(element);
    }

    public RoundRobinHolder() {
        holder = new LinkedBlockingDeque<>();
    }

    public void add(E element) {
        holder.add(element);
    }

    public boolean remove(E element) {
        return holder.remove(element);
    }

    public void set(Collection<? extends E> elements) {
        holder.clear();
        holder.addAll(elements);
    }

    public E next() {
        try {
            E element = holder.pollFirst(1, TimeUnit.SECONDS);
            holder.addLast(element);
            return element;

        } catch (InterruptedException ex) {
            throw new NoSuchElementException("no element in the holder to retrieve");
        }
    }
}
