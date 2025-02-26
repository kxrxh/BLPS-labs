package com.itmo.blps.lab1.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.itmo.blps.lab1.entities.RealEstateType;

import java.io.IOException;

public class RealEstateTypeDeserializer extends JsonDeserializer<RealEstateType> {
    @Override
    public RealEstateType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null || value.isEmpty()) {
            return null;
        }

        value = value.toUpperCase();

        try {
            return RealEstateType.valueOf(value);
        } catch (IllegalArgumentException e) {
            for (RealEstateType enumValue : RealEstateType.values()) {
                if (enumValue.name().equalsIgnoreCase(value)) {
                    return enumValue;
                }
            }
            throw e;
        }
    }
} 