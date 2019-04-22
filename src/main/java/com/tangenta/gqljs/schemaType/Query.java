package com.tangenta.gqljs.schemaType;

import com.tangenta.gqljs.schemaType.util.Util;

import java.util.Map;

final public class Query implements Operation {
    private String name;
    private String retType;
    private String strippedRetType;
    private Map<String, String> args;
    private boolean needAuth;

    public Query(String name, String retType, Map<String, String> args, boolean needAuth) {
        this.name = name;
        this.retType = retType;
        this.strippedRetType = Util.strip(retType);
        this.args = args;
        this.needAuth = needAuth;
    }

    @Override
    public String getOperType() {
        return "query";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getRetType() {
        return retType;
    }

    @Override
    public String getStrippedRetType() {
        return strippedRetType;
    }

    @Override
    public Map<String, String> getArgs() {
        return args;
    }

    @Override
    public boolean needAuth() {
        return needAuth;
    }
}