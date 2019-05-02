package com.tangenta.gqljs.schemaType.util;

import lombok.Value;
import lombok.val;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Value
public class GqlDef {
    String defName;
    Map<String, String> params;
    String retName;
    private String strippedRetName;

    public GqlDef(String defName, Map<String, String> params, String retName) {
        val newMap = new LinkedHashMap<String, String>();
        List<Map.Entry<String, String>> oldList = new ArrayList<>(params.entrySet());
        Collections.reverse(oldList);
        oldList.forEach(e -> newMap.put(e.getKey(), e.getValue()));

        this.defName = defName;
        this.params = newMap;
        this.retName = retName;
        this.strippedRetName = Util.strip(retName);
    }

    public String getStrippedRetType() {
        return strippedRetName;
    }
}
