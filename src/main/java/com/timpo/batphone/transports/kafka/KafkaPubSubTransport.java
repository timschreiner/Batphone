package com.timpo.batphone.transports.kafka;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.timpo.batphone.other.Constants;
import com.timpo.batphone.other.Utils;
import com.timpo.batphone.transports.AbstractTransport;
import com.timpo.batphone.transports.BinaryMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.ConsumerTimeoutException;
import kafka.consumer.KafkaStream;
import kafka.consumer.Whitelist;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.javaapi.producer.Producer;
import kafka.message.MessageAndMetadata;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

public class KafkaPubSubTransport extends AbstractTransport {

    private static final Joiner topicJoiner = Joiner.on("|").skipNulls();
    private static final Joiner addressJoiner = Joiner.on(",").skipNulls();
    //
    private final Producer<byte[], byte[]> producer;
    private final ConsumerConnector consumer;

    public KafkaPubSubTransport(int numThreads, ExecutorService es, String serviceGroup, List<BrokerAddress> brokers, List<ZookeeperAddress> zookeepers) {
        super(numThreads, es);

        producer = makeProducer(defaultProducerProps(brokers));
        consumer = makeConsumer(defaultConsumerProps(zookeepers), serviceGroup);
    }

    public KafkaPubSubTransport(int numThreads, ExecutorService es, String serviceGroup, Properties producerProps, Properties consumerProps) {
        super(numThreads, es);

        producer = makeProducer(producerProps);
        consumer = makeConsumer(consumerProps, serviceGroup);
    }

    @Override
    protected List<? extends MessageConsumer> makeConsumers(Set<String> listenFor, int numThreads) {
        List<KafkaMessageConsumer> list = new ArrayList<>();

        String topics = topicJoiner.join(listenFor);

        List<KafkaStream<byte[], byte[]>> streams =
                consumer.createMessageStreamsByFilter(new Whitelist(topics), numThreads);

        for (final KafkaStream<byte[], byte[]> stream : streams) {
            list.add(new KafkaMessageConsumer(stream));
        }

        return list;
    }

    @Override
    public void send(BinaryMessage bm) throws Exception {
        //the hashcode of the partition key determines which partition this gets 
        //written to. for now, just use random partitioning
        //TODO: determine if this partions randomly enough do to the hashing
        byte[] partitionKey = new byte[1];
        Utils.RAND.nextBytes(partitionKey);

        //TODO: figure out why the first time you send a message to a topic it dies
        producer.send(new KeyedMessage<>(bm.getKey(), partitionKey, bm.getPayload()));
    }

    private static Properties defaultProducerProps(List<BrokerAddress> brokers) {
        Properties props = new Properties();
        props.put("metadata.broker.list", addressJoiner.join(brokers));
        props.put("request.required.acks", "1");
        return props;
    }

    private static Properties defaultConsumerProps(List<ZookeeperAddress> zookeepers) {
        Properties props = new Properties();
        props.put("zookeeper.connect", addressJoiner.join(zookeepers));
        props.put("zookeeper.session.timeout.ms", "400");
        props.put("zookeeper.sync.time.ms", "200");
        props.put("auto.commit.interval.ms", "1000");
        return props;
    }

    private Producer<byte[], byte[]> makeProducer(Properties producerProps) {
        return new Producer<>(new ProducerConfig(producerProps));
    }

    private ConsumerConnector makeConsumer(Properties consumerProps, String serviceGroup) {
        //set the timeout for blocking on the next message, otherwise it will 
        //block forever and we can't gracefully shutdown the service
        if (consumerProps.getProperty("consumer.timeout.ms") == null) {
            String blockingTimeout = String.valueOf(
                    Utils.toMillis(Constants.BLOCKING_TIMEOUT, TimeUnit.MILLISECONDS));

            consumerProps.setProperty("consumer.timeout.ms", blockingTimeout);
        }

        //this makes sure we can load balance messages across multiple instances of a service
        if (consumerProps.getProperty("group.id") == null) {
            consumerProps.setProperty("group.id", serviceGroup);
        }

        return Consumer.createJavaConsumerConnector(new ConsumerConfig(consumerProps));
    }

    @Override
    public void shutdown() {
        super.shutdown();

        consumer.shutdown();
    }

    private class KafkaMessageConsumer extends MessageConsumer {

        private final ConsumerIterator<byte[], byte[]> it;

        public KafkaMessageConsumer(KafkaStream<byte[], byte[]> stream) {
            it = stream.iterator();
        }

        @Override
        protected Optional<BinaryMessage> nextMessage() {
            try {
                if (it.hasNext()) {
                    MessageAndMetadata<byte[], byte[]> mAndM = it.next();

                    return Optional.of(new BinaryMessage(mAndM.topic(), mAndM.message()));
                }
            } catch (ConsumerTimeoutException e) {
                //this runs if the it.hasNext() call times out. the 
                //timeout is configured in 'consumer.timeout.ms', 
                //without it this would block forever and couldn't 
                //be shutdown. it can be safely ignored
            }

            return Optional.absent();
        }
    }
}
