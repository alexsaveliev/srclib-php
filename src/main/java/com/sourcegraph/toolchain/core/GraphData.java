package com.sourcegraph.toolchain.core;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Implementation of graph writer that collects references and definitions and then writes them as JSON
 */
public class GraphData implements GraphWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphData.class);

    private final Map<Def, Def> defs = new LinkedHashMap<>();
    private final Collection<Ref> refs = new LinkedHashSet<>();

    @Override
    public void writeRef(Ref r) throws IOException {
        refs.add(r);
    }

    @Override
    public void writeDef(Def s) throws IOException {
        Def prev = defs.put(s, s);
        if (prev != null) {
            LOGGER.warn("{} already defined in {} at {}:{}, redefinition attempt in {} at {}:{}",
                    prev.defKey.getPath(),
                    prev.file,
                    prev.defStart,
                    prev.defEnd,
                    s.file,
                    s.defStart,
                    s.defEnd);
        }
    }

    @Override
    public void flush() throws IOException {
    }

    static class JSONSerializer implements JsonSerializer<GraphData> {
        @Override
        public JsonElement serialize(GraphData src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject object = new JsonObject();
            object.add("Defs", context.serialize(src.defs.keySet()));
            object.add("Refs", context.serialize(src.refs));
            return object;
        }
    }
}
