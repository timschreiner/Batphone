package com.timpo.batphone.codecs;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.timpo.batphone.messages.Message;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CodecTest {

    private Codec codec;

    public CodecTest() {
        codec = CodecBuilder
                .setCodec(new JSONCodec())
                .setCompressor(new SnappyCompressor())
                .instrument()
                .build();
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testEncodeAndDecode() throws Exception {
        Map<String, Object> data = Maps.newHashMap();
        data.put("int", 1);
        data.put("string", "stuff");
        data.put("list", Lists.newArrayList(1, "b"));

        Message expected = new Message();
        expected.setData(data);
        expected.setFrom("f1", "f2");
        expected.setRequestID("test-request-id");
        expected.setTo("t1", "t2");

        byte[] bytes = codec.encode(expected);

        Message actual = codec.decode(bytes, Message.class);

        assertEquals(expected, actual);
    }
}
