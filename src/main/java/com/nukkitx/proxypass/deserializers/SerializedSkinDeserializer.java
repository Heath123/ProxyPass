package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nukkitx.protocol.bedrock.data.skin.*;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;

import java.io.IOException;
import java.util.List;

public class SerializedSkinDeserializer extends StdDeserializer<SerializedSkin> {

    public SerializedSkinDeserializer() {
        this(null);
    }

    public SerializedSkinDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public SerializedSkin deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = ProxyPlayerSession.jsonSerializer.readTree(jp);

        String skinId = node.get("skinId").textValue();
        String skinResourcePatch = node.get("skinResourcePatch").textValue();
        ImageData skinData = ProxyPlayerSession.jsonSerializer.readValue(node.get("skinData").traverse(), ImageData.class);
        List<AnimationData> animations = ProxyPlayerSession.jsonSerializer.readValue(node.get("animations").traverse(), new TypeReference<List<AnimationData>>() {});
        ImageData capeData = ProxyPlayerSession.jsonSerializer.readValue(node.get("capeData").traverse(), ImageData.class);
        String geometryData = node.get("geometryData").textValue();
        String animationData = node.get("animationData").textValue();
        boolean premium = node.get("premium").asBoolean();
        boolean persona = node.get("persona").asBoolean();
        boolean capeOnClassic = node.get("capeOnClassic").asBoolean();
        String capeId = node.get("capeId").textValue();
        String fullSkinId = node.get("fullSkinId").textValue();
        String armSize = node.get("armSize").textValue();
        String skinColor = node.get("skinColor").textValue();
        List<PersonaPieceData> personaPieces = ProxyPlayerSession.jsonSerializer.readValue(node.get("personaPieces").traverse(), new TypeReference<List<PersonaPieceData>>() {});
        List<PersonaPieceTintData> tintColors = ProxyPlayerSession.jsonSerializer.readValue(node.get("tintColors").traverse(), new TypeReference<List<PersonaPieceTintData>>() {});

        return SerializedSkin.of(skinId, skinResourcePatch, skinData,
                animations, capeData, geometryData,
                animationData, premium, persona, capeOnClassic,
                capeId, fullSkinId, armSize, skinColor,
                personaPieces, tintColors);
    }
}