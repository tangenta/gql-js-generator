package com.tangenta.gqljs.schemaType;

import java.util.Map;

public interface Operation {
    String getOperType();
    String getName();
    String getRetType();
    String getStrippedRetType();
    Map<String, String> getArgs();
    boolean needAuth();
}
