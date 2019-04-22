package com.tangenta.gqljs.schemaType;

import lombok.Value;

import java.util.List;

@Value
public class Union {
    String name;
    List<String> subTypes;

    public Union(String name, List<String> subTypes) {
        if (subTypes.contains(name)) {
            throw new RuntimeException("Union type: " + name + " is compose of itself");
        }
        this.name = name;
        this.subTypes = subTypes;
    }
}
