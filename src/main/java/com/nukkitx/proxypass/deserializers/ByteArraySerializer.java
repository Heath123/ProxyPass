package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;

public class ByteArraySerializer
        extends StdSerializer<byte[]>
{
    public ByteArraySerializer() {
        this(null);
    }

    public ByteArraySerializer(Class<byte[]> t) {
        super(t);
    }

    @Override
    public void serialize(byte[] value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeStartArray();
        internalSerialize(value, jgen, provider);
        jgen.writeEndArray();
    }

    public void internalSerialize(byte[] value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        for (byte b : value) {
            jgen.writeNumber(unsignedToBytes(b));
        }
    }

    private static int unsignedToBytes(byte b) {
        return b & 0xFF;
    }

    @Override
    public void serializeWithType(byte[] value, JsonGenerator gen, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
        WritableTypeId typeId = typeSer.typeId(value, JsonToken.START_ARRAY);

        typeSer.writeTypePrefix(gen, typeId);
        internalSerialize(value, gen, provider);
        typeSer.writeTypeSuffix(gen, typeId);
    }
}

