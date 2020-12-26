package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nukkitx.protocol.bedrock.data.skin.PersonaPieceData;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;

import java.io.IOException;

public class PersonaPieceDataDeserializer extends StdDeserializer<PersonaPieceData> {

    public PersonaPieceDataDeserializer() {
        this(null);
    }

    public PersonaPieceDataDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public PersonaPieceData deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = ProxyPlayerSession.jsonSerializer.readTree(jp);

        String id = node.get("id").textValue();
        String type = node.get("type").textValue();
        String packId = node.get("packId").textValue();
        boolean isDefault = node.get("default").asBoolean();
        String productId = node.get("productId").textValue();

        return new PersonaPieceData(id, type, packId, isDefault, productId);
    }
}