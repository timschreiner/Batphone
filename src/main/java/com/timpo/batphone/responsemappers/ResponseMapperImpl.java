package com.timpo.batphone.responsemappers;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.timpo.batphone.messages.Message;
import com.timpo.batphone.other.Utils;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;

public class ResponseMapperImpl implements ResponseMapper<Message> {

    private static final Logger LOG = Utils.logFor(ResponseMapperImpl.class);
    //
    private final ConcurrentMap<String, BlockingCallable<Message>> holder;
    private final ListeningExecutorService pool;

    public ResponseMapperImpl(ExecutorService es) {
        holder = new ConcurrentHashMap<>();
        pool = MoreExecutors.listeningDecorator(es);
    }

    //TODO: clear out old responses
    @Override
    public void resolveResponse(Message m) throws IOException {
        BlockingCallable<Message> rc = holder.remove(m.getRequestID());
        if (rc != null) {
            rc.unblock(m);
        } else {
            LOG.warn("unable to resolve requestID={}", m.getRequestID());
        }
    }

    @Override
    public ListenableFuture<Message> makeFuture(String requestID) {
        BlockingCallable<Message> rc = new BlockingCallable<>();

        holder.put(requestID, rc);

        return pool.submit(rc);
    }
}
