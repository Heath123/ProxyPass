package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;

import java.io.IOException;

public class Vector3fDeserializer extends StdDeserializer<Vector3f> {

    public Vector3fDeserializer() {
        this(null);
    }

    public Vector3fDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Vector3f deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = ProxyPlayerSession.jsonSerializer.readTree(jp);

        float x = node.get("x").floatValue();
        float y = node.get("y").floatValue();
        float z = node.get("z").floatValue();
        // String itemName = node.get("itemName").asText();
        // int userId = (Integer) ((IntNode) node.get("createdBy")).numberValue();

        return Vector3f.from(x, y, z);
    }
}
