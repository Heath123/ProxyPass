package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.data.skin.SerializedSkin;
import com.nukkitx.protocol.bedrock.packet.PlayerListPacket;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;

import java.io.IOException;
import java.util.UUID;

public class PlayerListPacket$EntryDeserializer extends StdDeserializer<PlayerListPacket.Entry> {

    public PlayerListPacket$EntryDeserializer() {
        this(null);
    }

    public PlayerListPacket$EntryDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public PlayerListPacket.Entry deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        PlayerListPacket.Entry entry = new PlayerListPacket.Entry(
                ProxyPlayerSession.jsonSerializer.readValue(node.get("uuid").traverse(), UUID.class)
        );
        entry.setEntityId(node.get("entityId").longValue());
        entry.setName(node.get("name").textValue());
        entry.setXuid(node.get("xuid").textValue());
        entry.setPlatformChatId(node.get("platformChatId").textValue());
        entry.setBuildPlatform((Integer) node.get("buildPlatform").numberValue());
        entry.setSkin(ProxyPlayerSession.jsonSerializer.readValue(node.get("skin").traverse(), SerializedSkin.class));
        entry.setTeacher(node.get("teacher").asBoolean());
        entry.setHost(node.get("host").asBoolean());
        entry.setTrustedSkin(node.get("trustedSkin").asBoolean());

        return entry;

        /*
        private final UUID uuid;
        private long entityId;
        private String name;
        private String xuid;
        private String platformChatId;
        private int buildPlatform;
        private SerializedSkin skin;
        private boolean teacher;
        private boolean host;
        private boolean trustedSkin;
        */
    }
}