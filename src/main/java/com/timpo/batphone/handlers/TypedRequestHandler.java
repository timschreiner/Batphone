package com.timpo.batphone.handlers;

import com.google.common.base.Optional;
import com.timpo.batphone.messages.Message;
import com.timpo.batphone.other.Utils;
import java.util.Map;

public abstract class TypedRequestHandler<Req, Res> implements RequestHandler {

    private Class<Req> requestClass;

    public TypedRequestHandler(Class<Req> requestClass) {
        this.requestClass = requestClass;
    }

    public abstract Res handle(Req request, String channel);

    @Override
    public Optional<Map<String, Object>> handle(Message request, String channel) {
        return Utils.response(handle(request.dataAs(requestClass), channel));
    }
}
