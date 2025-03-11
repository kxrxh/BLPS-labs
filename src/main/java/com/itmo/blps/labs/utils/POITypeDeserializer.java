package com.itmo.blps.labs.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.itmo.blps.labs.entities.POIType;

import java.io.IOException;

public class POITypeDeserializer extends JsonDeserializer<POIType> {
    @Override
    public POIType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null || value.isEmpty()) {
            return null;
        }

        value = value.toUpperCase();

        try {
            return POIType.valueOf(value);
        } catch (IllegalArgumentException e) {
            for (POIType enumValue : POIType.values()) {
                if (enumValue.name().equalsIgnoreCase(value)) {
                    return enumValue;
                }
            }
            throw e;
        }
    }
}