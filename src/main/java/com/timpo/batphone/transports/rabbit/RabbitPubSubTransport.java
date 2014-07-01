package com.timpo.batphone.transports.rabbit;

import com.google.common.base.Optional;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.MessageProperties;
import com.timpo.batphone.codecs.Codec;
import com.timpo.batphone.messages.Message;
import com.timpo.batphone.other.Utils;
import com.timpo.batphone.transports.polling.PollingTransport;
import com.timpo.batphone.transports.BinaryMessage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

public class RabbitPubSubTransport<M extends Message> extends PollingTransport<M> {

  private static final Logger LOG = Utils.logFor(RabbitPubSubTransport.class);
  //
  private static final String TOPIC_EXCHANGE = "svc_tpc";
  //
  private final String serviceGroup;
  private final Connection conn;
  private final ChannelPool channelPool;

  public RabbitPubSubTransport(Codec codec, Class<M> decodeAs, int numThreads, ExecutorService es, String serviceGroup, Connection conn) throws IOException {
    super(codec, decodeAs, numThreads, es);

    this.conn = conn;
    this.serviceGroup = serviceGroup;

    this.channelPool = new ChannelPool(conn, numThreads);

    init();
  }

  public RabbitPubSubTransport(Codec codec, Class<M> decodeAs, int numThreads, ExecutorService es, String serviceGroup, RabbitAddress address) throws IOException {
    super(codec, decodeAs, numThreads, es);

    this.serviceGroup = serviceGroup;

    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(address.getHost());
    factory.setPort(address.getPort());

    conn = factory.newConnection();
    channelPool = new ChannelPool(conn);

    init();
  }

  @Override
  protected List<? extends MessagePoller> makePollers(Set<String> listenFor, int numThreads) {
    List<RabbitMessageConsumer> list = new ArrayList<>();

    try {
      Channel channel = channelPool.borrowObject();

      try {
        for (String topic : listenFor) {
          channel.queueBind(serviceGroup, TOPIC_EXCHANGE, topic);
        }

        for (int i = 0; i < numThreads; i++) {
          list.add(new RabbitMessageConsumer(serviceGroup));
        }

      } catch (IOException ex) {
        LOG.warn("problem creating consumers", ex);

      } finally {
        channelPool.returnObject(channel);
      }

    } catch (Exception ex) {
      LOG.warn("problem with the channelPool", ex);
    }

    return list;
  }

  @Override
  public void send(BinaryMessage message) throws Exception {
    //PERSISTENT_BASIC ensures that these messages will be persisted to disk, 
    //which we need to ensure all events are processed at least once

    Channel channel = channelPool.borrowObject();
    try {
      channel.basicPublish(TOPIC_EXCHANGE, message.getKey(),
              MessageProperties.PERSISTENT_BASIC, message.getPayload());

    } catch (Exception ex) {
      LOG.warn("problem with the channelPool", ex);

    } finally {
      channelPool.returnObject(channel);
    }
  }

  private void init() throws IOException {
    boolean durable = true;
    boolean exclusive = false;
    boolean autoDelete = false;

    try {
      Channel channel = channelPool.borrowObject();
      try {
        channel.exchangeDeclare(TOPIC_EXCHANGE, "topic", durable);
        channel.queueDeclare(serviceGroup, durable, exclusive, autoDelete, null);
      
      } finally {
        channelPool.returnObject(channel);
      }

    } catch (Exception ex) {
      LOG.warn("problem with the channelPool", ex);
    }

    //TODO: any other config settings here?
  }

  @Override
  public void shutdown() {
    super.shutdown();

    try {
      channelPool.close();
    } catch (Exception e) {
    }

    try {
      conn.close();
    } catch (Exception e) {
    }
  }

  private class RabbitMessageConsumer extends MessagePoller {

    private final String queue;

    public RabbitMessageConsumer(String queue) {
      this.queue = queue;
    }

    @Override
    protected Optional<BinaryMessage> nextMessage() {
      try {
        Channel channel = channelPool.borrowObject();

        try {
          GetResponse gr = channel.basicGet(queue, true);
          if (gr != null) {
            String key = gr.getEnvelope().getRoutingKey();
            byte[] payload = gr.getBody();

            return Optional.of(new BinaryMessage(key, payload));

          } else {
            //TODO: is this ok? do we need this at all?
            Utils.sleep(100, TimeUnit.MILLISECONDS);
          }

        } catch (IOException ex) {
          LOG.warn("problem getting next message", ex);

        } finally {
          channelPool.returnObject(channel);
        }

      } catch (Exception ex) {
        LOG.warn("problem with the channelPool", ex);
      }

      return Optional.absent();
    }
  }
}
