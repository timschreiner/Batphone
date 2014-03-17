package com.timpo.batphone.handlers;

import com.google.common.base.Optional;
import com.timpo.batphone.messages.Request;
import com.timpo.batphone.other.Utils;
import java.util.Map;

public abstract class TypedRequestHandler<Req, Res> implements RequestHandler {

    private Class<Req> requestClass;

    public TypedRequestHandler(Class<Req> requestClass) {
        this.requestClass = requestClass;
    }

    public abstract Res handle(Req request, String topic);

    @Override
    public Optional<Map<String, Object>> handle(Request request, String topic) {
        return Utils.response(handle(request.dataAs(requestClass), topic));
    }
}
