package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;

import java.io.IOException;

public class ItemDataDeserializer extends StdDeserializer<ItemData> {

    public ItemDataDeserializer() {
        this(null);
    }

    public ItemDataDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public ItemData deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = ProxyPlayerSession.jsonSerializer.readTree(jp);

        int netId = (Integer) node.get("netId").numberValue();
        int id = (Integer) node.get("id").numberValue();
        short damage = node.get("damage").shortValue();
        int count = (Integer) node.get("count").numberValue();
        NbtMap tag = ProxyPlayerSession.jsonSerializer.readValue(node.get("tag").traverse(), NbtMap.class);
        String[] canPlace = ProxyPlayerSession.jsonSerializer.readValue(node.get("canPlace").traverse(), String[].class);
        String[] canBreak = ProxyPlayerSession.jsonSerializer.readValue(node.get("canBreak").traverse(), String[].class);
        long blockingTicks = node.get("blockingTicks").longValue();

        return ItemData.fromNet(netId, id, damage, count, tag, canPlace, canBreak, blockingTicks);
    }
}