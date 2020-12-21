package com.nukkitx.proxypass.deserializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.nukkitx.nbt.NbtList;

import java.io.IOException;

public class NbtListSerializer extends StdSerializer<NbtList> {

    public NbtListSerializer() {
        this(null);
    }

    public NbtListSerializer(Class<NbtList> t) {
        super(t);
    }

    @Override
    public void serialize(
            NbtList value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        jgen.writeStartObject();
        internalSerialize(value, jgen, provider);
        jgen.writeEndObject();
    }

    public void internalSerialize(
            NbtList value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {

        jgen.writeObjectField("nbtType", value.getType().getEnum());
        jgen.writeArrayFieldStart("data");
        for (Object element : value) {
            jgen.writeObject(element);
        }
        jgen.writeEndArray();
    }

    @Override
    public void serializeWithType(NbtList value, JsonGenerator gen,
                                  SerializerProvider provider, TypeSerializer typeSer)
            throws IOException, JsonProcessingException {

        WritableTypeId typeId = typeSer.typeId(value, JsonToken.START_OBJECT);

        typeSer.writeTypePrefix(gen, typeId);
        internalSerialize(value, gen, provider); // call your customized serialize method
        typeSer.writeTypeSuffix(gen, typeId);
    }
}