package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nukkitx.nbt.NbtList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;

public class NbtListDeserializer extends StdDeserializer<NbtList> {

    public NbtListDeserializer() {
        this(null);
    }

    public NbtListDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public NbtList deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = jp.getCodec().readTree(jp);
        try {
            NbtType nbtType = (NbtType) NbtType.class.getDeclaredField(node.get("nbtType").textValue()).get(null);
            Object[] data = (Object[]) ProxyPlayerSession.jsonSerializer.readValue(node.get("data").traverse(), Array.newInstance(nbtType.getTagClass(), 0).getClass());
            return new NbtList(nbtType, data);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
}