package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nukkitx.math.vector.Vector2f;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;

import java.io.IOException;

public class Vector2fDeserializer extends StdDeserializer<Vector2f> {

    public Vector2fDeserializer() {
        this(null);
    }

    public Vector2fDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Vector2f deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = ProxyPlayerSession.jsonSerializer.readTree(jp);

        float x = node.get("x").floatValue();
        float y = node.get("y").floatValue();

        return Vector2f.from(x, y);
    }
}
