package com.timpo.batphone.holders;

public class HolderFactory {

    public static <E> RoundRobinHolder<E> makeRoundRobinHolder() {
        //this is currently the best holder implementation
        return new ArrayListHolder<>();
    }
}
