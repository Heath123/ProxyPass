package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;

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
        JsonNode node = jp.getCodec().readTree(jp);
        float x = node.get("x").floatValue();
        float y = node.get("y").floatValue();
        float z = node.get("z").floatValue();
        // String itemName = node.get("itemName").asText();
        // int userId = (Integer) ((IntNode) node.get("createdBy")).numberValue();

        return Vector3f.from(x, y, z);
    }
}
