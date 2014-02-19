package com.timpo.batphone.handlers;

import com.google.common.base.Optional;
import com.timpo.batphone.messages.Message;
import java.util.Map;

public interface RequestHandler {

    Optional<Map<String, Object>> handle(Message request, String channel);
}
