package com.tangenta.gqljs.schemaType;

import lombok.Value;

import java.util.List;

@Value
public class Enume {
    String name;
    List<String> enums;
}
