package com.nukkitx.proxypass.deserializers;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nukkitx.protocol.bedrock.data.inventory.CraftingData;
import com.nukkitx.protocol.bedrock.data.inventory.CraftingDataType;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class CraftingDataDeserializer extends StdDeserializer<CraftingData> {

    public CraftingDataDeserializer() {
        this(null);
    }

    public CraftingDataDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public CraftingData deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = ProxyPlayerSession.jsonSerializer.readTree(jp);

        CraftingDataType type = ProxyPlayerSession.jsonSerializer.readValue(node.get("type").traverse(), CraftingDataType.class);
        String recipeId = node.get("recipeId").textValue();
        int width = (Integer) node.get("width").numberValue();
        int height = (Integer) node.get("height").numberValue();
        int inputId = (Integer) node.get("inputId").numberValue();
        int inputDamage = (Integer) node.get("inputDamage").numberValue();
        List<ItemData> inputs = ProxyPlayerSession.jsonSerializer.readValue(node.get("inputs").traverse(), new TypeReference<List<ItemData>>() {});
        List<ItemData> outputs = ProxyPlayerSession.jsonSerializer.readValue(node.get("outputs").traverse(), new TypeReference<List<ItemData>>() {});
        UUID uuid = ProxyPlayerSession.jsonSerializer.readValue(node.get("uuid").traverse(), UUID.class);
        String craftingTag = node.get("craftingTag").textValue();
        int priority = (Integer) node.get("priority").numberValue();
        int networkId = (Integer) node.get("networkId").numberValue();

        return new CraftingData(type, recipeId, width, height, inputId, inputDamage, inputs, outputs, uuid, craftingTag,
                priority, networkId);

    }
}