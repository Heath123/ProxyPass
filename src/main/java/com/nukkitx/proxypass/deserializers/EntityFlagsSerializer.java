package com.nukkitx.proxypass.deserializers;
// TODO: Not a deserializer

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlags;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Set;

public class EntityFlagsSerializer extends StdSerializer<EntityFlags> {

    public EntityFlagsSerializer() {
        this(null);
    }

    public EntityFlagsSerializer(Class<EntityFlags> t) {
        super(t);
    }

    @Override
    public void serialize(EntityFlags value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeStartArray();
        internalSerialize(value, jgen, provider);
        jgen.writeEndArray();
    }

    public void internalSerialize(
            EntityFlags value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {

        // http://tutorials.jenkov.com/java-reflection/private-fields-and-methods.html
        try {
            Field privateStringField = EntityFlags.class.
                    getDeclaredField("flags");

            privateStringField.setAccessible(true);

            Set<EntityFlag> fieldValue = (Set<EntityFlag>) privateStringField.get(value);

            // jgen.writeStartObject();
            // jgen.writeObjectField("flags", fieldValue);
            // jgen.writeEndObject();
            // jgen.writeStartArray();

            for(EntityFlag flag : fieldValue){
                jgen.writeObject(flag);
            }
            // jgen.writeEndObject();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void serializeWithType(EntityFlags value, JsonGenerator gen,
                                  SerializerProvider provider, TypeSerializer typeSer)
            throws IOException, JsonProcessingException {

        WritableTypeId typeId = typeSer.typeId(value, JsonToken.START_ARRAY);

        typeSer.writeTypePrefix(gen, typeId);
        serialize(value, gen, provider); // call your customized serialize method
        typeSer.writeTypeSuffix(gen, typeId);
    }
}