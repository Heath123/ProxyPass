package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nukkitx.protocol.bedrock.data.skin.ImageData;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;

import java.io.IOException;

public class ImageDataDeserializer extends StdDeserializer<ImageData> {

    public ImageDataDeserializer() {
        this(null);
    }

    public ImageDataDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public ImageData deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = ProxyPlayerSession.jsonSerializer.readTree(jp);

        int width = (Integer) node.get("width").numberValue();
        int height = (Integer) node.get("height").numberValue();
        byte[] image = ProxyPlayerSession.jsonSerializer.readValue(node.get("image").traverse(), byte[].class);

        return ImageData.of(width, height, image);
    }
}