package com.timpo.batphone;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

    final static Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws IOException, InterruptedException, Exception {
        
        DateFormat autoDirFormat = new SimpleDateFormat("YYYY MM");
        Date exifDate = new Date();
        String format = autoDirFormat.format(exifDate);
        System.out.println("format = " + format);
        
        System.exit(0);
        
        
        int runs = 100;

        Random random = new Random();

        int threadLimit = Runtime.getRuntime().availableProcessors() + 1;

        ExecutorService es = Executors.newFixedThreadPool(threadLimit,
                new ThreadFactoryBuilder().setDaemon(true).setPriority(Thread.MAX_PRIORITY - 2)
                .setNameFormat("es").build());

        for (int sampleSize = 1; sampleSize <= 10000; sampleSize *= 10) {
            MetricRegistry mr = new MetricRegistry();

            List<Integer> testValues = new ArrayList<>();

            int startAt = 0; //random.nextInt();
            int endAt = startAt + sampleSize;
            for (int i = startAt; i < endAt; i++) {
                testValues.add(i);
            }

            ConsoleReporter reporter = ConsoleReporter.forRegistry(mr)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .build();

            for (int i = 0; i < runs; i++) {
                Collections.shuffle(testValues, random);

                testConcurrentSkipListSet(testValues, mr.timer("ConcurrentSkipListSet"), es);

                testSynchronizedTreeSet(testValues, mr.timer("SynchronizedTreeSet"), es);

                testCopyOnWriteArrayList(testValues, mr.timer("CopyOnWriteArrayList"), es);

                testSynchronizedList(testValues, mr.timer("SynchronizedList"), es);
            }

            System.out.println("startAt=" + startAt + " samples=" + sampleSize);
            reporter.report();
            System.out.println("\n\n");
        }

        es.shutdown();
        es.awaitTermination(5, TimeUnit.SECONDS);
    }

    private static void testConcurrentSkipListSet(List<Integer> testValues, Timer timer, ExecutorService es) throws InterruptedException {
        final SortedSet<Integer> set = new ConcurrentSkipListSet<>();

        List<Callable<Void>> tasks = prepareTasks(testValues, set);

        Context timerContext = timer.time();
        es.invokeAll(tasks);
        Lists.newArrayList(set);
        timerContext.stop();
    }

    private static void testSynchronizedTreeSet(List<Integer> testValues, Timer timer, ExecutorService es) throws InterruptedException {
        final SortedSet<Integer> set = Collections.synchronizedSortedSet(new TreeSet<Integer>());

        List<Callable<Void>> tasks = prepareTasks(testValues, set);

        Context timerContext = timer.time();
        es.invokeAll(tasks);
        Lists.newArrayList(set);
        timerContext.stop();
    }

    private static void testCopyOnWriteArrayList(List<Integer> testValues, Timer timer, ExecutorService es) throws InterruptedException {
        final List<Integer> list = new CopyOnWriteArrayList<>();

        List<Callable<Void>> tasks = prepareTasks(testValues, list);

        Context timerContext = timer.time();
        es.invokeAll(tasks);
        List<Integer> list2 = Lists.newArrayList(list);
        Collections.sort(list2);
        timerContext.stop();
    }

    private static void testSynchronizedList(List<Integer> testValues, Timer timer, ExecutorService es) throws InterruptedException {
        final List<Integer> list = Collections.synchronizedList(new ArrayList<Integer>());

        List<Callable<Void>> tasks = prepareTasks(testValues, list);

        Context timerContext = timer.time();
        es.invokeAll(tasks);
        Collections.sort(list);
        timerContext.stop();
    }

    private static List<Callable<Void>> prepareTasks(List<Integer> testValues, final Collection<Integer> col) {
        List<Callable<Void>> tasks = new ArrayList<>();
        for (final Integer integer : testValues) {
            tasks.add(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    col.add(integer);

                    for (int i = 0; i < integer; i++) {
                        
                    }
                    
                    return null;
                }
            });
        }
        return tasks;
    }
}
