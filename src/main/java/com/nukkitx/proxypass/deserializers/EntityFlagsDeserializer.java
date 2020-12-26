package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlags;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;

import java.io.IOException;
import java.util.Iterator;

public class EntityFlagsDeserializer extends StdDeserializer<EntityFlags> {

    public EntityFlagsDeserializer() {
        this(null);
    }

    public EntityFlagsDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public EntityFlags deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = ProxyPlayerSession.jsonSerializer.readTree(jp);

        EntityFlags flags = new EntityFlags();

        for (Iterator<JsonNode> i = node.elements(); i.hasNext();) {
            flags.setFlag(EntityFlag.valueOf(
                    i.next().textValue()
            ), true);
        }

        return flags;
    }
}