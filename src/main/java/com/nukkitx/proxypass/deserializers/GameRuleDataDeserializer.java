package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nukkitx.protocol.bedrock.data.GameRuleData;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;

import java.io.IOException;

public class GameRuleDataDeserializer extends StdDeserializer<GameRuleData> {

    public GameRuleDataDeserializer() {
        this(null);
    }

    public GameRuleDataDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public GameRuleData deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = ProxyPlayerSession.jsonSerializer.readTree(jp);

        String name = node.get("name").textValue();
        Object value = ProxyPlayerSession.jsonSerializer.readValue(node.get("value").traverse(), Object.class);

        if (value instanceof Boolean) {
            return new GameRuleData<>(name, (boolean) value);
        } else if (value instanceof Integer) {
            return new GameRuleData<>(name, (int) value);
        } else if (value instanceof Float) {
            return new GameRuleData<>(name, (float) value);
        } else {
            System.err.println("Error: " + value + "in gamerule data is not Boolean, Integer or Float.");
            return null;
        }
    }
}