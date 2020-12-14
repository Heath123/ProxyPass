package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nukkitx.math.vector.Vector3i;

import java.io.IOException;

public class Vector3iDeserializer extends StdDeserializer<Vector3i> {

    public Vector3iDeserializer() {
        this(null);
    }

    public Vector3iDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Vector3i deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        int x = (Integer) node.get("x").numberValue();
        int y = (Integer) node.get("y").numberValue();
        int z = (Integer) node.get("z").numberValue();
        // String itemName = node.get("itemName").asText();
        // int userId = (Integer) ((IntNode) node.get("createdBy")).numberValue();

        return Vector3i.from(x, y, z);
    }
}