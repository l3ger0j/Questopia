package org.qp.android.helpers.utils;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.OutputStream;

public final class XmlUtil {

    public static void objectToXml(OutputStream out , Object o) throws Exception {
        new XmlMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValue(out , o);
    }

    public static <T> T xmlToObject(String xml, Class<T> clazz) throws Exception {
        return new XmlMapper().readValue(xml , clazz);
    }
}
