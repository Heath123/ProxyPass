package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nukkitx.protocol.bedrock.data.AttributeData;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;

import java.io.IOException;

public class AttributeDataDeserializer extends StdDeserializer<AttributeData> {

    public AttributeDataDeserializer() {
        this(null);
    }

    public AttributeDataDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public AttributeData deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = ProxyPlayerSession.jsonSerializer.readTree(jp);

        String name = node.get("name").textValue();
        float minimum = node.get("minimum").floatValue();
        float maximum = node.get("maximum").floatValue();
        float value = node.get("value").floatValue();
        float defaultValue = node.get("defaultValue").floatValue();


        return new AttributeData(name, minimum, maximum, value, defaultValue);
    }
}