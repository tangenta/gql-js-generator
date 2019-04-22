package com.tangenta.gqljs;

import com.tangenta.gqljs.schemaType.Operation;
import com.tangenta.gqljs.schemaType.Query;
import com.tangenta.gqljs.schemaType.Type;
import com.tangenta.gqljs.schemaType.util.GqlDef;
import lombok.val;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

public class GQLPrinter {
    private static final String INDENT = "  ";
    private static final String DINDENT = INDENT + INDENT;

    public static String toGQL(Schema schema) {
        return "not-impl";
    }

    public static String toJSFunc(Schema schema) {
        return schema.allOperations().map(op -> {
            val allVariables = schema.allVariablesForOper(op.getName());
            return String.format("const %s = %s => sendGQL({\n" +
                            INDENT + "query: %s,\n" +
                            INDENT + "variables: %s%s" +
                            "\n});\n",
                    op.getName(),
                    jsParamList(op, allVariables),
                    fromBigCamel_TO_ALL_CAPITAL(op.getName()),
                    varContent(allVariables),
                    optionAuth(op)
                    );
        }).collect(Collectors.joining("\n"));
    }

    public static String toJSConst(Schema schema) {
        return schema.allOperations()
                .map(op -> String.format("const %s = `\n" +
                                INDENT + "%s %s%s {\n" +
                                DINDENT + "%s%s%s" +
                                INDENT + "}\n" +
                                "`;\n",
                        fromBigCamel_TO_ALL_CAPITAL(op.getName()),
                        op.getOperType(),
                        capitalize(op.getName()),
                        paramList(schema.allVariablesForOper(op.getName())),
                        op.getName(),
                        argList(op.getArgs()),
                        retContent(op.getStrippedRetType(), schema, DINDENT)
                )).collect(Collectors.joining("\n"));
    }

    private static String paramList(Map<String, String> params) {
        return argList(params, entry -> "$" + entry.getKey() + ": " + entry.getValue());
    }

    private static String argList(Map<String, String> args) {
        return argList(args, entry -> entry.getKey() + ": $" + entry.getKey());
    }

    private static String retContent(String retType, Schema schema, String indent) {
        val builder = new StringBuilder();
        builder.append(" {\n");
        schema.findUnion(retType).ifPresent(union -> {
            union.getSubTypes().forEach(subType -> {
                builder.append(indent).append(INDENT).append("... on ").append(subType);
                builder.append(retContent(subType, schema, indent + INDENT));
            });
        });

        schema.findType(retType).ifPresent(type -> {
            type.getFieldTypeMap().forEach(gqlDef -> {
                builder.append(indent).append(INDENT).append(gqlDef.getDefName());
                if (Type.isScalar(gqlDef.getStrippedRetName())) {
                    builder.append("\n");
                } else {
                    builder.append(retContArgList(gqlDef));
                    builder.append(retContent(gqlDef.getStrippedRetName(), schema, indent + INDENT));
                }
            });
        });
        builder.append(indent).append("}\n");
        return builder.toString();
    }

    private static String varContent(Map<String, String> variables) {
        return "{\n" +
                variables.entrySet().stream()
                        .map(e -> DINDENT + e.getKey() + ": " + e.getKey())
                        .collect(Collectors.joining(",\n")) +
                "\n" + INDENT + "}";
    }

    private static String optionAuth(Operation operation) {
        if (!operation.needAuth()) return "";
        return ",\n" + INDENT + "\nauth: auth\n";
    }

    private static String jsParamList(Operation op, Map<String, String> allVariables) {
        if (op.needAuth()) allVariables.put("auth", "");
        String result = "(" + String.join(", ", allVariables.keySet()) + ")";
        allVariables.remove("auth");
        return result;
    }

    private static String retContArgList(GqlDef gqlDef) {
        return argList(gqlDef.getParams(), entry -> entry.getKey() + ": " + entry.getKey());
    }

    private static String argList(Map<String, String> kvMap, Function<Map.Entry<String, String>, String> transformer) {
        String result = kvMap.entrySet().stream()
                .map(transformer).collect(Collectors.joining(", "));
        if (result.isEmpty()) return "";
        else return "(" + result + ")";
    }

    private static String capitalize(String origin) {
        if (origin.isEmpty()) return "";
        val builder = new StringBuilder(origin.length());
        val iter = origin.chars().iterator();
        char firstChar = (char)iter.next().intValue();
        builder.append(Character.toUpperCase(firstChar));
        iter.forEachRemaining((IntConsumer) i -> builder.append((char)i));
        return builder.toString();
    }

    private static String fromBigCamel_TO_ALL_CAPITAL(String originString) {
        val builder = new StringBuilder();
        originString.chars().forEachOrdered(ch -> {
            if (Character.isUpperCase(ch)) {
                builder.append("_").append((char)ch);
            } else {
                builder.append(Character.toUpperCase((char)ch));
            }
        });
        return builder.toString();
    }

    public static void main(String[] args) {
        System.out.println(fromBigCamel_TO_ALL_CAPITAL("allStringAndTest"));
        System.out.println(capitalize("tasdfsadfet"));

        val qry = new Query("tst", "TestType!", new HashMap<String, String>(){{
            put("username", "String!");
            put("password", "String2!");
        }}, false);

        System.out.println(paramList(qry.getArgs()));
        System.out.println(argList(qry.getArgs()));
    }
}
