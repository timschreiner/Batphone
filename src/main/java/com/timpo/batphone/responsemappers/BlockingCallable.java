package com.timpo.batphone.responsemappers;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

public class BlockingCallable<T> implements Callable<T> {

	private final BlockingQueue<T> queue;

	public BlockingCallable() {
		queue = new ArrayBlockingQueue<>(1);
	}

	public void unblock(T t) {
		queue.add(t);
	}

        @Override
	public T call() throws Exception {
		return queue.take();
	}
}
