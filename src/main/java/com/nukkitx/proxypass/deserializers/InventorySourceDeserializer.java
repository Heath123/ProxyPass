package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nukkitx.protocol.bedrock.data.inventory.InventorySource;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class InventorySourceDeserializer extends StdDeserializer<InventorySource> {

    public InventorySourceDeserializer() {
        this(null);
    }

    public InventorySourceDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public InventorySource deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = ProxyPlayerSession.jsonSerializer.readTree(jp);

        Constructor<InventorySource> constructor;
        try {
            constructor = InventorySource.class.getDeclaredConstructor(InventorySource.Type.class, int.class, InventorySource.Flag.class);
            constructor.setAccessible(true);
            InventorySource source = constructor.newInstance(
                    InventorySource.Type.valueOf(node.get("type").textValue()),
                    (int) node.get("containerId").numberValue(),
                    InventorySource.Flag.valueOf(node.get("flag").textValue())
            );

            return source;
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }
}