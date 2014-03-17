package com.timpo.batphone.holders;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * An early stab at implementing a RoundRobinHolder, this implementation is much
 * less performant than the ArrayListHolder
 *
 * @param <E>
 */
public class BlockingQueueHolder<E> implements RoundRobinHolder<E> {

    private final BlockingDeque<E> holder;

    public BlockingQueueHolder(E element) {
        this();
        holder.add(element);
    }

    public BlockingQueueHolder() {
        holder = new LinkedBlockingDeque<>();
    }

    @Override
    public void add(E element) {
        if (element == null) {
            throw new IllegalArgumentException("element cannot be null");
        }

        holder.add(element);
    }

    @Override
    public synchronized boolean addIfAbsent(E element) {
        boolean exists = holder.contains(element);

        if (!exists) {
            add(element);
        }

        return exists;
    }

    @Override
    public int size() {
        return holder.size();
    }

    @Override
    public boolean remove(E element) {
        return holder.remove(element);
    }

    @Override
    public void replaceAll(Collection<? extends E> elements) {
        holder.clear();
        holder.addAll(elements);
    }

    @Override
    public E next() {
        try {
            E element = holder.pollFirst(1, TimeUnit.SECONDS);
            if (element == null) {
                throw new NoSuchElementException("this holder contains no elements");
            }

            holder.addLast(element);
            return element;

        } catch (InterruptedException ex) {
            throw new NoSuchElementException("this holder contains no elements");
        }
    }
}
