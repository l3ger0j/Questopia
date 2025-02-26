package org.qp.android.helpers.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class JsonUtil {

    public static void objectToJson(OutputStream out, Object o) throws IOException {
        new ObjectMapper()
                .enable(JsonParser.Feature.AUTO_CLOSE_SOURCE)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValue(out, o);
    }

    public static void objectToJson(File file, Object o) throws IOException {
        new ObjectMapper()
                .enable(JsonParser.Feature.AUTO_CLOSE_SOURCE)
                .enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION)
                .writeValue(file, o);
    }

    public static <T> T jsonToObject(String json, Class<T> clazz) throws IOException {
        return new ObjectMapper()
                .enable(JsonParser.Feature.AUTO_CLOSE_SOURCE)
                .enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION)
                .readValue(json, clazz);
    }

    public static <T> T jsonToObject(File file, Class<T> clazz) throws IOException {
        return new ObjectMapper()
                .enable(JsonParser.Feature.AUTO_CLOSE_SOURCE)
                .enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION)
                .readValue(file, clazz);
    }

    public static <T> T jsonToObject(File file, TypeReference<T> ref) throws IOException {
        return new ObjectMapper()
                .enable(JsonParser.Feature.AUTO_CLOSE_SOURCE)
                .enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION)
                .readValue(file, ref);
    }
}
