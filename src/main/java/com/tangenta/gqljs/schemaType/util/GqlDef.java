package com.tangenta.gqljs.schemaType.util;

import lombok.Value;

import java.util.Map;

@Value
public class GqlDef {
    String defName;
    Map<String, String> params;
    String retName;
    private String strippedRetName;

    public GqlDef(String defName, Map<String, String> params, String retName) {
        this.defName = defName;
        this.params = params;
        this.retName = retName;
        this.strippedRetName = Util.strip(retName);
    }

    public String getStrippedRetType() {
        return strippedRetName;
    }
}
