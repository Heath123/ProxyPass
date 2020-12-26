package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;

import java.io.IOException;

public class StartGamePacket$ItemEntryDeserializer extends StdDeserializer<StartGamePacket.ItemEntry> {

    public StartGamePacket$ItemEntryDeserializer() {
        this(null);
    }

    public StartGamePacket$ItemEntryDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public StartGamePacket.ItemEntry deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = ProxyPlayerSession.jsonSerializer.readTree(jp);

        String identifier = node.get("identifier").textValue();
        short id = node.get("id").shortValue();
        boolean componentBased = node.get("componentBased").asBoolean();

        return new StartGamePacket.ItemEntry(identifier, id, componentBased);
    }
}