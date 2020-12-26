package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerMixData;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;

import java.io.IOException;

public class ContainerMixDataDeserializer extends StdDeserializer<ContainerMixData> {

    public ContainerMixDataDeserializer() {
        this(null);
    }

    public ContainerMixDataDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public ContainerMixData deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = ProxyPlayerSession.jsonSerializer.readTree(jp);

        int inputId = (Integer) node.get("inputId").numberValue();
        int reagentId = (Integer) node.get("reagentId").numberValue();
        int outputId = (Integer) node.get("outputId").numberValue();

        return new ContainerMixData(inputId, reagentId, outputId);
    }
}