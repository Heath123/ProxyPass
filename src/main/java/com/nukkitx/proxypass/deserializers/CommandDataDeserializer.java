package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nukkitx.protocol.bedrock.data.command.CommandData;
import com.nukkitx.protocol.bedrock.data.command.CommandEnumData;
import com.nukkitx.protocol.bedrock.data.command.CommandParamData;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;

import java.io.IOException;
import java.util.List;

public class CommandDataDeserializer extends StdDeserializer<CommandData> {

    public CommandDataDeserializer() {
        this(null);
    }

    public CommandDataDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public CommandData deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = ProxyPlayerSession.jsonSerializer.readTree(jp);

        String name = node.get("name").textValue();
        String description = node.get("description").textValue();
        List<CommandData.Flag> flags = ProxyPlayerSession.jsonSerializer.readValue(node.get("flags").traverse(), new TypeReference<List<CommandData.Flag>>() {});
        byte permission = node.get("permission").numberValue().byteValue();
        CommandEnumData aliases = ProxyPlayerSession.jsonSerializer.readValue(node.get("aliases").traverse(), CommandEnumData.class);
        CommandParamData[][] overloads = ProxyPlayerSession.jsonSerializer.readValue(node.get("overloads").traverse(), CommandParamData[][].class);

        return new CommandData(name, description, flags, permission, aliases, overloads);
    }
}