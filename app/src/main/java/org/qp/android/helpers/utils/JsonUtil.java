package org.qp.android.helpers.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class JsonUtil {

    public static void objectToJson(OutputStream out , Object o) throws IOException {
        new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValue(out , o);
    }

    public static <T> T jsonToObject(String json, Class<T> clazz) throws IOException {
        return new ObjectMapper().readValue(json , clazz);
    }

    public static <T> T jsonToObject(File file, Class<T> clazz) throws IOException {
        return new ObjectMapper().readValue(file , clazz);
    }
}
