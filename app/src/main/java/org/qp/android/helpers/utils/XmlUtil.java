package org.qp.android.helpers.utils;

import android.net.Uri;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import org.qp.android.helpers.UriMixIn;

import java.io.File;
import java.io.IOException;

public final class XmlUtil {
    public static void objectToXml(File file, Object o) throws IOException {
        new XmlMapper()
                .addMixIn(Uri.class, UriMixIn.class)
                .enable(JsonParser.Feature.AUTO_CLOSE_SOURCE)
                .enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION)
                .writeValue(file, o);
    }

    public static <T> T xmlToObject(String xml, Class<T> clazz) throws IOException {
        return new XmlMapper()
                .addMixIn(Uri.class, UriMixIn.class)
                .enable(JsonParser.Feature.AUTO_CLOSE_SOURCE)
                .enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION)
                .readValue(xml, clazz);
    }

    public static <T> T xmlToObject(File file, TypeReference<T> ref) throws IOException {
        return new XmlMapper()
                .addMixIn(Uri.class, UriMixIn.class)
                .enable(JsonParser.Feature.AUTO_CLOSE_SOURCE)
                .enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION)
                .readValue(file, ref);
    }
}