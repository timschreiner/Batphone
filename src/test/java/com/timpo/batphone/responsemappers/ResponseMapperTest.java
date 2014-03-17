/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.timpo.batphone.responsemappers;

import com.google.common.util.concurrent.ListenableFuture;
import com.timpo.batphone.messages.Message;
import com.timpo.batphone.messages.Request;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class ResponseMapperTest {

    private ResponseMapper<Request> responseMapper;
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

        Request expectedResponse = new Request();
        expectedResponse.setData(expectedData);
        expectedResponse.setRequestID(requestID);

        ListenableFuture<Request> future = responseMapper.makeFuture(requestID);

        responseMapper.resolveResponse(expectedResponse);

        Message actualResponse = future.get(1, TimeUnit.SECONDS);

        assertEquals(expectedData, actualResponse.getData());
    }
}