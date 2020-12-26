package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nukkitx.protocol.bedrock.data.command.CommandEnumData;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;

import java.io.IOException;

public class CommandEnumDataDeserializer extends StdDeserializer<CommandEnumData> {

    public CommandEnumDataDeserializer() {
        this(null);
    }

    public CommandEnumDataDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public CommandEnumData deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = ProxyPlayerSession.jsonSerializer.readTree(jp);

        String name = node.get("name").textValue();
        String[] values = ProxyPlayerSession.jsonSerializer.readValue(node.get("values").traverse(), String[].class);
        boolean isSoft = node.get("soft").asBoolean();

        return new CommandEnumData(name, values, isSoft);
    }
}