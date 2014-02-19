/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.timpo.batphone.responsemappers;

import com.google.common.util.concurrent.ListenableFuture;
import com.timpo.batphone.messages.Message;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class ResponseMapperTest {

    private ResponseMapper<Message> responseMapper;
    private ExecutorService es;

    public ResponseMapperTest() {
        es = Executors.newCachedThreadPool();
    }

    @Before
    public void setUp() {
        responseMapper = new ResponseMapperImpl(es);
    }

    @Test
    public void testResolveResponse() throws Exception {
        
        Map<String, Object> expectedData = new HashMap<>();
        expectedData.put("test", "expected");
        
        String requestID = "simple-request-id";

        Message expectedResponse = new Message();
        expectedResponse.setData(expectedData);
        expectedResponse.setRequestID(requestID);

        ListenableFuture<Message> future = responseMapper.makeFuture(requestID);

        responseMapper.resolveResponse(expectedResponse);

        Message actualResponse = future.get(1, TimeUnit.SECONDS);

        assertEquals(expectedData, actualResponse.getData());
    }
}