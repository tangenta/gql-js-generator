package com.tangenta.gqljs;

import com.tangenta.gqljs.schemaType.*;
import com.tangenta.gqljs.schemaType.util.GqlDef;
import javafx.util.Pair;
import lombok.val;

import java.util.*;

public class SchemaParser {
    public static Schema parseSchema(String source) {
        return gqlSchema(new Scanner(source));
    }

    private static Schema gqlSchema(Scanner scanner) {
        Schema.SchemaBuilder builder = Schema.getBuilder();
        while (scanner.hasNext()) {
            switch (scanner.peek().toLowerCase()) {
                case "@query": builder.appendQuery(query(scanner)); break;
                case "+@query": builder.appendQuery(queryA(scanner)); break;
                case "@mutation": builder.appendMutation(mutation(scanner)); break;
                case "+@mutation": builder.appendMutation(mutationA(scanner)); break;
                case "type": builder.appendType(type(scanner)); break;
                case "union": builder.appendUnion(union(scanner)); break;
                case "enum": builder.appendEnume(enume(scanner)); break;
                case "input": builder.appendType(input(scanner)); break;
                default: throw new RuntimeException("unknown schema " + scanner.peek());
            }
        }
        return builder.build();
    }

    private static Query query(Scanner tokens) {
        assertEq(tokens.next().toLowerCase(), "@query");
        GqlDef gqlDef = def(tokens);
        return new Query(gqlDef.getDefName(), gqlDef.getRetName(), gqlDef.getParams(), false);
    }

    private static Query queryA(Scanner tokens) {
        assertEq(tokens.next().toLowerCase(), "+@query");
        GqlDef gqlDef = def(tokens);
        return new Query(gqlDef.getDefName(), gqlDef.getRetName(), gqlDef.getParams(), true);
    }

    private static Mutation mutation(Scanner tokens) {
        assertEq(tokens.next().toLowerCase(), "@mutation");
        GqlDef gqlDef = def(tokens);
        return new Mutation(gqlDef.getDefName(), gqlDef.getRetName(), gqlDef.getParams(), false);
    }

    private static Mutation mutationA(Scanner tokens) {
        assertEq(tokens.next().toLowerCase(), "+@mutation");
        GqlDef gqlDef = def(tokens);
        return new Mutation(gqlDef.getDefName(), gqlDef.getRetName(), gqlDef.getParams(), true);
    }

    private static Union union(Scanner tokens) {
        assertEq(tokens.next().toLowerCase(), "union");
        String unionName = tokens.next();
        assertEq(tokens.next(), ("="));
        List<String> types = typeList(tokens);
        return new Union(unionName, types);
    }

    private static Type type(Scanner tokens) {
        assertEq(tokens.next().toLowerCase(), "type");
        String typeName = tokens.next();
        assertEq(tokens.next(), ("{"));
        List<GqlDef> defs = defs(tokens);
        assertEq(tokens.next(), ("}"));

        return new Type(typeName, defs);
    }

    private static Enume enume(Scanner tokens) {
        assertEq(tokens.next().toLowerCase(), ("enum"));
        String name = tokens.next();
        assertEq(tokens.next(), ("{"));
        List<String> enumVals = enumDefs(tokens);
        assertEq(tokens.next(), ("}"));

        return new Enume(name, enumVals);
    }

    private static Type input(Scanner tokens) {
        assertEq(tokens.next().toLowerCase(), "input");
        String typeName = tokens.next();
        assertEq(tokens.next(), ("{"));
        List<GqlDef> defs = defs(tokens);
        assertEq(tokens.next(), ("}"));

        return new Type(typeName, defs);
    }

    private static Map<String, String> argList(Scanner tokens) {
        if (tokens.peek().equals("(")) {
            tokens.next();
            val kvPair = arg(tokens);
            val restKvMap = cArgList(tokens);
            assertEq(tokens.next(), (")"));
            restKvMap.put(kvPair.getKey(), kvPair.getValue());
            return restKvMap;
        } else {
            return new LinkedHashMap<>();
        }
    }

    private static Pair<String, String> arg(Scanner tokens) {
        val argName = tokens.next();
        assertEq(tokens.next(), ":");
        val argValue = tokens.next();
        return new Pair<>(argName, argValue);
    }

    private static Map<String, String> cArgList(Scanner tokens) {
        if (tokens.peek().equals(",")) {
            tokens.next();
            val paramPair = arg(tokens);
            val restParamMap = cArgList(tokens);
            restParamMap.put(paramPair.getKey(), paramPair.getValue());
            return restParamMap;
        } else {
            return new LinkedHashMap<>();
        }
    }

    private static List<String> typeList(Scanner tokens) {
        String typename = tokens.next();
        List<String> restTypes = cTypeList(tokens);
        restTypes.add(typename);
        return restTypes;
    }

    private static List<String> cTypeList(Scanner tokens) {
        if (tokens.hasNext() && tokens.peek().equals("|")) {
            tokens.next();
            return typeList(tokens);
        } else {
            return new LinkedList<>();
        }
    }

    private static List<GqlDef> defs(Scanner tokens) {
        GqlDef def = def(tokens);
        List<GqlDef> optionDefs = optionDefs(tokens);
        optionDefs.add(def);
        return optionDefs;
    }

    private static List<GqlDef> optionDefs(Scanner tokens) {
        if (!tokens.peek().equals("}")) {
            GqlDef def = def(tokens);
            List<GqlDef> defs = optionDefs(tokens);
            defs.add(def);
            return defs;
        } else {
            return new LinkedList<>();
        }
    }

    private static List<String> enumDefs(Scanner tokens) {
        String name = tokens.next();
        List<String> rest = optionEnumDefs(tokens);
        rest.add(name);
        return rest;
    }

    private static List<String> optionEnumDefs(Scanner tokens) {
        if (!tokens.peek().equals("}")) {
            String enumName = tokens.next();
            List<String> rest = optionEnumDefs(tokens);
            rest.add(enumName);
            return rest;
        } else {
            return new LinkedList<>();
        }
    }

    private static GqlDef def(Scanner scanner) {
        String name = scanner.next();
        Map<String, String> args = argList(scanner);
        assertEq(scanner.next(), ":");
        String retName = scanner.next();
        return new GqlDef(name, args, retName);
    }

    private static void assertEq(String left, String right) {
        if (!left.equals(right)) {
            throw new RuntimeException("assertion error, left: \"" + left + "\" right: \"" + right + "\"");
        }
    }

    public static void main(String[] args) {
        String data = "enum Align {\n" +
                "    start\n" +
                "    center\n" +
                "    end\n" +
                "}";
        Scanner tokens = new Scanner(data);
        System.out.println(enume(tokens));

        Schema schema = parseSchema("# 最热讨论\n" +
                "@query hots: HotsResult! # 依赖于日期，每日更新，缓存\n" +
                "\n" +
                "union HotsResult = Error | Hots\n" +
                "type Hots {\n" +
                "    hots: [HotItem!]!\n" +
                "}\n" +
                "union HotItem = SchoolHeatInfo | EntertainmentInfo | LearningResourceInfo");
    }
}
