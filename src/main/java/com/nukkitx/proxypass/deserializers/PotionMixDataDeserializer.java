package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nukkitx.protocol.bedrock.data.inventory.PotionMixData;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;

import java.io.IOException;

public class PotionMixDataDeserializer extends StdDeserializer<PotionMixData> {

    public PotionMixDataDeserializer() {
        this(null);
    }

    public PotionMixDataDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public PotionMixData deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = ProxyPlayerSession.jsonSerializer.readTree(jp);

        int inputId = (Integer) node.get("inputId").numberValue();
        int inputMeta = (Integer) node.get("inputMeta").numberValue();
        int reagentId = (Integer) node.get("reagentId").numberValue();
        int reagentMeta = (Integer) node.get("reagentMeta").numberValue();
        int outputId = (Integer) node.get("outputId").numberValue();
        int outputMeta = (Integer) node.get("outputMeta").numberValue();

        return new PotionMixData(inputId, inputMeta, reagentId, reagentMeta, outputId, outputMeta);
    }
}