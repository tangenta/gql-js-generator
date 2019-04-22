package com.tangenta.gqljs.schemaType;

import com.tangenta.gqljs.schemaType.util.GqlDef;
import lombok.Value;

import java.util.List;

@Value
public class Type {
    private static final String[] SCALARS = {
            "ID", "Int", "Long", "String"
    };
    String name;
    List<GqlDef> fieldTypeMap;

    public static boolean isScalar(String typename) {
        for (String scalar : SCALARS) {
            if (typename.equals(scalar)) {
                return true;
            }
        }
        return false;
    }
}