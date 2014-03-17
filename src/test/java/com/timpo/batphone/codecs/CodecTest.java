package com.timpo.batphone.codecs;

import com.codahale.metrics.ConsoleReporter;
import com.timpo.batphone.codecs.impl.JSONCodec;
import com.timpo.batphone.codecs.impl.KryoCodec;
import com.timpo.batphone.codecs.impl.SmileCodec;
import com.timpo.batphone.codecs.impl.SnappyCompressor;
import com.timpo.batphone.messages.Request;
import com.timpo.batphone.metrics.Metrics;
import com.timpo.batphone.other.Utils;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class CodecTest {

    @SuppressWarnings({"unchecked"})
    private Map<String, Object> blah() throws Exception {
        String s =
                "    {\n"
                + "        \"id\": 0,\n"
                + "        \"guid\": \"29245d67-1c4d-4e2e-87bf-e2eed9056901\",\n"
                + "        \"isActive\": false,\n"
                + "        \"balance\": \"$1,596.00\",\n"
                + "        \"picture\": \"http://placehold.it/32x32\",\n"
                + "        \"age\": 31,\n"
                + "        \"name\": \"Lee Hahn\",\n"
                + "        \"gender\": \"female\",\n"
                + "        \"company\": \"Steeltab\",\n"
                + "        \"email\": \"leehahn@steeltab.com\",\n"
                + "        \"phone\": \"+1 (978) 457-2598\",\n"
                + "        \"address\": \"921 Clay Street, Warren, Louisiana, 5426\",\n"
                + "        \"about\": \"Sit anim nulla culpa labore aute esse qui aliqua consequat. Quis aliquip do culpa dolore anim. Tempor quis laboris elit cupidatat nisi ad. Culpa sit ut nisi tempor Lorem. Voluptate cillum excepteur incididunt deserunt eiusmod consequat ea anim velit proident esse in. Sunt eu laborum excepteur pariatur.\\r\\n\",\n"
                + "        \"registered\": \"2010-08-18T17:42:33 +07:00\",\n"
                + "        \"latitude\": -20.048553,\n"
                + "        \"longitude\": -62.367392,\n"
                + "        \"tags\": [\n"
                + "            \"reprehenderit\",\n"
                + "            \"ad\",\n"
                + "            \"fugiat\",\n"
                + "            \"elit\",\n"
                + "            \"veniam\",\n"
                + "            \"aliquip\",\n"
                + "            \"qui\"\n"
                + "        ],\n"
                + "        \"friends\": [\n"
                + "            {\n"
                + "                \"id\": 0,\n"
                + "                \"name\": \"Solis Meyer\"\n"
                + "            },\n"
                + "            {\n"
                + "                \"id\": 1,\n"
                + "                \"name\": \"Good Bonner\"\n"
                + "            },\n"
                + "            {\n"
                + "                \"id\": 2,\n"
                + "                \"name\": \"Luella Ortega\"\n"
                + "            }\n"
                + "        ],\n"
                + "        \"customField\": \"Hello, Lee Hahn! You have 4 unread messages.\"\n"
                + "    }";

        return Utils.JSON.readValue(s, Map.class);
    }

    @Test
    public void testEncodeAndDecode() throws Exception {


        Request expected = new Request();
        expected.setData(blah());
        expected.setFrom("f1", "f2");
        expected.setRequestID("test-request-id");
        expected.setTo("t1", "t2");

        byte[] bytes = null;
        Request actual;
        ConsoleReporter reporter = Metrics.makeReporter(TimeUnit.MICROSECONDS);


        for (int samples = 0; samples < 1; samples++) {
            Metrics.clear();

            final Codec[] codecs = new Codec[]{
                CodecBuilder
                .setCodec(new JSONCodec())
                .setCompressor(new SnappyCompressor())
                .instrument()
                .build(),
                //
                CodecBuilder
                .setCodec(new KryoCodec())
                .instrument()
                .build(),
                //
                CodecBuilder
                .setCodec(new SmileCodec())
                .instrument()
                .build()
            };
            for (Codec c : codecs) {
                for (int runs = 0; runs < 1000; runs++) {
                    bytes = c.encode(expected);
                    actual = c.decode(bytes, Request.class);
                }

                System.out.println(c.toString() + " " + bytes.length);
            }
            System.out.println("");
        }

        reporter.report();
    }
}
