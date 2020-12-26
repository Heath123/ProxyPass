package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nukkitx.protocol.bedrock.data.skin.AnimatedTextureType;
import com.nukkitx.protocol.bedrock.data.skin.AnimationData;
import com.nukkitx.protocol.bedrock.data.skin.AnimationExpressionType;
import com.nukkitx.protocol.bedrock.data.skin.ImageData;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;

import java.io.IOException;

public class AnimationDataDeserializer extends StdDeserializer<AnimationData> {

    public AnimationDataDeserializer() {
        this(null);
    }

    public AnimationDataDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public AnimationData deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = ProxyPlayerSession.jsonSerializer.readTree(jp);

        ImageData image = ProxyPlayerSession.jsonSerializer.readValue(node.get("image").traverse(), ImageData.class);
        AnimatedTextureType textureType = ProxyPlayerSession.jsonSerializer.readValue(node.get("textureType").traverse(), AnimatedTextureType.class);
        float frames = node.get("frames").floatValue();;
        AnimationExpressionType expressionType = ProxyPlayerSession.jsonSerializer.readValue(node.get("expressionType").traverse(), AnimationExpressionType.class);
        
        return new AnimationData(image, textureType, frames, expressionType);
    }
}