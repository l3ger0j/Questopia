package org.qp.android.helpers;

import android.net.Uri;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

@JsonSerialize(using = UriSerializer.class)
@JsonDeserialize(using = UriDeserializer.class)
public interface UriMixIn {}

class UriDeserializer extends StdDeserializer<Uri> {
    public UriDeserializer() {
        super(Uri.class);
    }

    @Override
    public Uri deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return Uri.parse(p.getValueAsString());
    }
}

class UriSerializer extends StdSerializer<Uri> {
    public UriSerializer() {
        super(Uri.class);
    }

    @Override
    public void serialize(Uri value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.toString());
    }
}