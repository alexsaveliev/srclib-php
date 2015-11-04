package com.sourcegraph.toolchain.core;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Definition object
 */
public class Def {

    /**
     * Definition key
     */
    public DefKey defKey;

    /**
     * Definition kind
     */
    public String kind;

    /**
     * Definition name
     */
    public String name;

    /**
     * Source file
     */
    public String file;

    /**
     * Ident start
     */
    public int identStart;

    /**
     * Ident end
     */
    public int identEnd;

    /**
     * Definition start
     */
    public int defStart;

    /**
     * Definition end
     */
    public int defEnd;

    /**
     * Modifiers
     */
    public List<String> modifiers;

    public String pkg;

    public String doc;

    public String typeExpr;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Def def = (Def) o;

        // alexsaveliev: using only defKey to compare definitions because
        // there may be cases when two _files_ may want to define the same object.
        // for example, two C files may have main() function
        return !(defKey != null ? !defKey.equals(def.defKey) : def.defKey != null);

    }

    @Override
    public int hashCode() {
        // alexsaveliev: using only defKey to compare definitions because
        // there may be cases when two _files_ may want to define the same object.
        // for example, two C files may have main() function
        return defKey != null ? defKey.hashCode() : 0;
    }

    /**
     * JSON serialization rules for definition objects
     */
    static class JSONSerializer implements JsonSerializer<Def> {

        @Override
        public JsonElement serialize(Def sym, Type arg1, JsonSerializationContext arg2) {
            JsonObject object = new JsonObject();

            if (sym.file != null) {
                object.add("File", new JsonPrimitive(PathUtil.relativizeCwd(sym.file)));
            }

            object.add("Name", new JsonPrimitive(sym.name));

            object.add("DefStart", new JsonPrimitive(sym.defStart));
            object.add("DefEnd", new JsonPrimitive(sym.defEnd));

            boolean exported;
            if (sym.modifiers != null) {
                exported = sym.modifiers.contains("public");
                object.add("Exported", new JsonPrimitive(exported));
            } else {
                exported = false;
                object.add("Exported", new JsonPrimitive(false));
            }

            object.add("Local", new JsonPrimitive(!exported &&
                    !(sym.kind.equals("PACKAGE") ||
                            sym.kind.equals("ENUM") ||
                            sym.kind.equals("CLASS") ||
                            sym.kind.equals("ANNOTATION_TYPE") ||
                            sym.kind.equals("INTERFACE") ||
                            sym.kind.equals("ENUM_CONSTANT") ||
                            sym.kind.equals("FIELD") ||
                            sym.kind.equals("METHOD") ||
                            sym.kind.equals("CONSTRUCTOR"))));

            switch (sym.kind) {
                case "ENUM":
                case "CLASS":
                case "INTERFACE":
                case "ANNOTATION_TYPE":
                    object.add("Kind", new JsonPrimitive("type"));
                    break;
                case "METHOD":
                case "CONSTRUCTOR":
                    object.add("Kind", new JsonPrimitive("func"));
                    break;
                case "PACKAGE":
                    object.add("Kind", new JsonPrimitive("package"));
                    break;
                default:
                    object.add("Kind", new JsonPrimitive("var"));
                    break;
            }

            object.add("Path", new JsonPrimitive(sym.defKey.formatPath()));
            object.add("TreePath", new JsonPrimitive(sym.defKey.formatTreePath()));

            return object;
        }

    }
}
