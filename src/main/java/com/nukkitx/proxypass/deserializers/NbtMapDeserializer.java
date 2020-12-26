package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;

public class NbtMapDeserializer extends StdDeserializer<NbtMap> {

    public NbtMapDeserializer() {
        this(null);
    }

    public NbtMapDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public NbtMap deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = ProxyPlayerSession.jsonSerializer.readTree(jp);

        Constructor<NbtMap> constructor;
        try {
            constructor = NbtMap.class.getDeclaredConstructor(LinkedHashMap.class);
            constructor.setAccessible(true);
            NbtMap source = constructor.newInstance(
                    ProxyPlayerSession.jsonSerializer.readValue(node.traverse(), LinkedHashMap.class)
            );

            return source;
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }
}
