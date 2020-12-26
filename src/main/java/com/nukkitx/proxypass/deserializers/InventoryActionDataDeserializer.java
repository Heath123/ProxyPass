package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nukkitx.protocol.bedrock.data.inventory.InventoryActionData;
import com.nukkitx.protocol.bedrock.data.inventory.InventorySource;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;

import java.io.IOException;

public class InventoryActionDataDeserializer extends StdDeserializer<InventoryActionData> {

    public InventoryActionDataDeserializer() {
        this(null);
    }

    public InventoryActionDataDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public InventoryActionData deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = ProxyPlayerSession.jsonSerializer.readTree(jp);

        InventorySource source = ProxyPlayerSession.jsonSerializer.readValue(node.get("source").traverse(), InventorySource.class);
        int slot = (Integer) node.get("slot").numberValue();
        ItemData fromItem = ProxyPlayerSession.jsonSerializer.readValue(node.get("fromItem").traverse(), ItemData.class);
        ItemData toItem = ProxyPlayerSession.jsonSerializer.readValue(node.get("toItem").traverse(), ItemData.class);
        int stackNetworkId = (Integer) node.get("stackNetworkId").numberValue();

        return new InventoryActionData(source, slot, fromItem, toItem, stackNetworkId);
    }
}