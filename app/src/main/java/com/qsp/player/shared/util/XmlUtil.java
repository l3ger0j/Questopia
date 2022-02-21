package com.qsp.player.shared.util;

import org.simpleframework.xml.core.Persister;

import java.io.ByteArrayOutputStream;

public final class XmlUtil {
    private static final Persister PERSISTER = new Persister();

    public static String objectToXml(Object o) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PERSISTER.write(o, baos);
            return baos.toString();
        }
    }

    public static <T> T xmlToObject(String xml, Class<T> clazz) throws Exception {
        return PERSISTER.read(clazz, xml);
    }
}
