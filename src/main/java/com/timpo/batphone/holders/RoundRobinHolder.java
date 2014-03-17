package com.timpo.batphone.holders;

import java.util.Collection;

/**
 * This holds elements and distributes them in a round-robin fashion when
 * requested. Implementations should make it clear if they are not thread safe.
 *
 * @param <E>
 */
public interface RoundRobinHolder<E> {

    /**
     * add an element to the holder. it should show up in future calls to next
     *
     * @param element
     */
    public void add(E element);

    /**
     * same as add, but ensures that the holder does not contain duplicate
     * elements
     *
     * @param element
     * @return true if the holder did not already contain this element
     */
    public boolean addIfAbsent(E element);

    /**
     * get an element from the holder. if the holder is non-empty, this can be
     * called repeatedly forever (the implementation should have ring-like
     * semantics)
     *
     * @return
     */
    public E next();

    /**
     * remove an element from the holder. useful when the element represents a
     * resource that's no longer used.
     *
     * note: depending on the implementation, the element might not disappear
     * from the holder's rotation immediately.
     *
     * @param element
     * @return true if the holder contained the element to be removed
     */
    public boolean remove(E element);

    /**
     * the number of elements contained in the holder
     *
     * @return
     */
    public int size();

    /**
     * replaces all the elements in the holder with the elements in this
     * collection. implementation should likely be optional (but clearly
     * specified if skipped)
     *
     * @param elements
     */
    public void replaceAll(Collection<? extends E> elements);
}
