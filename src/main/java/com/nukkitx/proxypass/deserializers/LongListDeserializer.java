package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;

import java.io.IOException;
import java.util.Iterator;

public class LongListDeserializer extends StdDeserializer<LongList> {

    public LongListDeserializer() {
        this(null);
    }

    public LongListDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public LongList deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = ProxyPlayerSession.jsonSerializer.readTree(jp);

        LongList result = new LongArrayList();

        for (Iterator<JsonNode> i = node.elements(); i.hasNext();) {
            result.add(i.next().longValue());
        }

        return result;
    }
}