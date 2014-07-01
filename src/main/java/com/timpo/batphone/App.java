package com.timpo.batphone;

import com.timpo.batphone.codecs.Codec;
import com.timpo.batphone.codecs.impl.JSONCodec;
import com.timpo.batphone.handlers.EventHandler;
import com.timpo.batphone.handlers.Handler;
import com.timpo.batphone.holders.BlockingQueueHolder;
import com.timpo.batphone.holders.RoundRobinHolder;
import com.timpo.batphone.messages.Event;
import com.timpo.batphone.messages.Request;
import com.timpo.batphone.messengers.Messenger;
import com.timpo.batphone.messengers.MessengerImpl;
import com.timpo.batphone.other.Utils;
import com.timpo.batphone.transports.TopicMessage;
import com.timpo.batphone.transports.Transport;
import com.timpo.batphone.transports.rabbit.RabbitAddress;
import com.timpo.batphone.transports.rabbit.RabbitPubSubTransport;
import com.timpo.batphone.transports.redis.RedisDirectTransport;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import redis.clients.jedis.JedisPool;

public class App {

  static ExecutorService executorService = Executors.newCachedThreadPool();

  static Messenger newMessenger(String serviceGroup) throws Exception {
    String serviceID = serviceGroup + "." + Utils.simpleID();

    Codec codec = new JSONCodec();
    int numThreads = 1;

    RoundRobinHolder<JedisPool> holder = new BlockingQueueHolder<>(new JedisPool("localhost"));

    Transport<TopicMessage<Request>> fakeTransport = new Transport<TopicMessage<Request>>() {
      //<editor-fold defaultstate="collapsed" desc="generated">
      @Override
      public void listenFor(String topic) {
      }

      @Override
      public void onMessage(Handler<TopicMessage<Request>> handler) {
      }

      @Override
      public void send(TopicMessage<Request> message) throws Exception {
      }

      @Override
      public void start() {
      }

      @Override
      public void stop() {
      }

      @Override
      public void shutdown() {
      }
      //</editor-fold>
    };

    Transport<TopicMessage<Request>> requestTransport = new RedisDirectTransport(codec, numThreads, executorService, holder);
    Transport<TopicMessage<Request>> responseTransport = new RedisDirectTransport(codec, numThreads, executorService, holder);

    Transport<TopicMessage<Event>> eventTransport = new RabbitPubSubTransport<>(codec, Event.class, numThreads, executorService, serviceGroup, new RabbitAddress());

    return new MessengerImpl(serviceID, serviceGroup,
//            requestTransport,
//            responseTransport,
            fakeTransport,
            fakeTransport,
            eventTransport,
            executorService);
  }

  public static void main(String[] args) throws Exception {
    final AtomicInteger pingCounter = new AtomicInteger(0);
    final AtomicInteger pongCounter = new AtomicInteger(0);

    final String pingService = "pinger";
    String pongService = "ponger";

    Messenger pingMessenger = newMessenger(pingService);
    final Messenger pongMessenger = newMessenger(pongService);

    pongMessenger.onEvent(new EventHandler() {
      @Override
      public void handle(Event event, String topic) {
        System.out.println("ping " + pingCounter.incrementAndGet());
//        System.out.println("event = " + event);
        Map<String, Object> response = new HashMap<>();
        response.put("pong", System.currentTimeMillis());

        try {
          pongMessenger.notify(response, pingService);

        } catch (Exception ex) {
          Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }, pongService);

    pingMessenger.onEvent(new EventHandler() {
      @Override
      public void handle(Event event, String topic) {
        System.out.println("pong " + pongCounter.incrementAndGet());
//        System.out.println("event = " + event);
      }
    }, pingService);

    pingMessenger.start();
    pongMessenger.start();

    Utils.sleep(10, TimeUnit.MILLISECONDS);

    for (int i = 0; i < 100; i++) {
      Map<String, Object> event = new HashMap<>();
      event.put("ping", System.currentTimeMillis());

      pingMessenger.notify(event, pongService);
    }

    Utils.sleep(3, TimeUnit.SECONDS);
    
    pingMessenger.shutdown();
    pongMessenger.shutdown();
    executorService.shutdownNow();
  }
}
