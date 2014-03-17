package com.timpo.batphone.holders;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Uses a CopyOnWriteArrayList to maintain thread-safe iteration while still
 * providing elements very quickly
 *
 * @param <E>
 */
public class ArrayListHolder<E> implements RoundRobinHolder<E> {

    private final List<E> holder;
    private final AtomicReference<Iterator<E>> iterator;

    public ArrayListHolder() {
        holder = new CopyOnWriteArrayList<>();
        iterator = new AtomicReference<>(holder.iterator());
    }

    @Override
    public void add(E element) {
        if (element == null) {
            throw new IllegalArgumentException("element cannot be null");
        }

        Iterator<E> i = iterator.get();

        holder.add(element);

        iterator.compareAndSet(i, holder.iterator());
    }

    @Override
    public synchronized boolean addIfAbsent(E element) {
        if (!holder.contains(element)) {
            add(element);

            return false;
        }

        return true;
    }

    @Override
    public E next() {
        Iterator<E> i = iterator.get();

        //first, try to just get the next element in the iterator
        if (i.hasNext()) {
            try {
                return i.next();
            } catch (NoSuchElementException e) {
                //do nothing, we'll handle this below
            }
        }

        //if that failed, make sure there are elements to get
        if (holder.isEmpty()) {
            throw new NoSuchElementException("this holder contains no elements");
        }

        //if the iterator is exausted, try to resest it to a new one. we don't 
        //care if we succeed, as long as someone does
        iterator.compareAndSet(i, holder.iterator());

        //call ourselves recursively, hoping the next leap will be the one that takes us home
        return next();
    }

    @Override
    public boolean remove(E element) {
        boolean removed = holder.remove(element);

        iterator.set(holder.iterator());

        return removed;
    }

    @Override
    public int size() {
        return holder.size();
    }

    @Override
    public void replaceAll(Collection<? extends E> elements) {
        holder.clear();
        holder.addAll(elements);

        iterator.set(holder.iterator());
    }
}
