package com.blueanvil.kerch.krude;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Cosmin Marginean
 */
public class TypeTest {

@Test
public void testSimpleSerialization() throws Exception {
    new ObjectMapper().writeValue(System.out, random());
}

private Kingdom random() {
    Kingdom kingdom = new Kingdom();
    kingdom.animals.add(new Dog("Winston"));
    kingdom.animals.add(new Horse("Black Beauty"));
    return kingdom;
}

@Test
public void testCustomSerializer() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.setSerializerModifier(new BeanSerializerModifier() {
        @Override
        public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
            if (beanDesc.getBeanClass().equals(Kingdom.class)) {
                return new CustomSerializer((JsonSerializer<Kingdom>) serializer);
            } else {
                return super.modifySerializer(config, beanDesc, serializer);
            }
        }
    });
    objectMapper.registerModule(module);
    objectMapper.writeValue(System.out, random());
}
}

class CustomSerializer extends JsonSerializer<Kingdom> {

    private JsonSerializer<Kingdom> defaultSerializer;

    public CustomSerializer(JsonSerializer<Kingdom> defaultSerializer) {
        this.defaultSerializer = defaultSerializer;
    }

    @Override
    public void serialize(Kingdom value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeFieldName("kingdom");
        defaultSerializer.serialize(value, gen, serializers);
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(Kingdom value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
        gen.writeStartObject();
        gen.writeFieldName("kingdom");
        defaultSerializer.serializeWithType(value, gen, serializers, typeSer);
        gen.writeEndObject();
    }
}

class Kingdom {
    @JsonProperty List<Animal> animals = new ArrayList<>();
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "animalType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Dog.class, name = "dog"),
        @JsonSubTypes.Type(value = Horse.class, name = "horse")
})
abstract class Animal {
    @JsonProperty private String name;

    public Animal(String name) { this.name = name; }
}

class Dog extends Animal {
    public Dog(String name) { super(name); }
}

class Horse extends Animal {
    public Horse(String name) { super(name); }
}