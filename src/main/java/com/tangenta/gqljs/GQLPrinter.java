package com.tangenta.gqljs;

import com.tangenta.gqljs.schemaType.Operation;
import com.tangenta.gqljs.schemaType.Query;
import com.tangenta.gqljs.schemaType.Type;
import com.tangenta.gqljs.schemaType.Union;
import com.tangenta.gqljs.schemaType.util.GqlDef;
import com.tangenta.gqljs.schemaType.util.Util;
import lombok.val;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

public class GQLPrinter {
    private static final String INDENT = "  ";
    private static final String DINDENT = INDENT + INDENT;

    public static String toJavaImpl(Schema schema, String schemaType) {
        val builder = new StringBuilder();
        Operation operation = schema.allOperations()
                .filter(op -> op.getName().equals(schemaType))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("impossible"));

        builder.append(javaMethod(operation)).append("\n");

        operation.getArgs().values().stream()
                .map(typeStr -> schema.findType(Util.strip(typeStr)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(type -> {
                    if (type.getTypename().equals("input")) {
                        builder.append(javaPubStaticClass(type)).append("\n");
                    }
                });

        String retType = operation.getStrippedRetType();
        schema.findUnion(retType).ifPresent(union -> {
            union.getSubTypes().forEach(subType -> {
                schema.findType(subType).ifPresent(type -> {
                    builder.append(javaPlainInterface(type, union.getName())).append("\n");
                });
            });

            builder.append(javaSuperInterface(union)).append("\n");
        });

        schema.findType(retType).ifPresent(type -> {
            builder.append(javaPlainInterface(type, null)).append("\n");
        });

        return builder.toString();
    }

    public static String toDSL(Schema schema, String schemaType) {
        Operation operation = schema.allOperations().filter(op -> op.getName().equals(schemaType))
                .findFirst().orElse(null);
        if (operation == null) throw new RuntimeException(schemaType + " not found");

        val builder = new StringBuilder();

        String retType = operation.getStrippedRetType();
        schema.findUnion(retType).ifPresent(union -> {
            union.getSubTypes().forEach(subType -> {
                Type type = schema.findType(subType).orElseThrow(() -> new RuntimeException("impossible"));
                builder.append(typeDef(type)).append("\n");
            });

            builder.append(unionDef(union)).append("\n");
        });

        schema.findType(retType).ifPresent(type -> {
            builder.append(typeDef(type)).append("\n");
        });

        return builder.toString();
    }

    public static String toJSFunc(Schema schema) {
        return schema.allOperations().map(op -> {
            val allVariables = schema.allVariablesForOper(op.getName());
            return String.format("const %s = %s => sendGQL({\n" +
                            INDENT + "query: %s,\n" +
                            "%s%s" +
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
        if (Type.isScalar(retType)) return "\n";
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

    private static String javaMethod(Operation operation) {
        val argList = operation.getArgs();
        if (operation.needAuth()) argList.put("userToken", "String");
        val argsStr = argList.entrySet().stream()
                .map(entry -> Util.strip(entry.getValue()) + " " + entry.getKey())
                .collect(Collectors.joining(", "));

        return String.format("public static %s %s(%s) {\n" +
                "    return null;\n" +
                "}\n",
                operation.getStrippedRetType(),
                operation.getName(),
                argsStr
                );
    }

    private static String javaSuperInterface(Union union) {
        return String.format("public interface %s {}\n",
                union.getName());
    }

    private static String javaPubStaticClass(Type type) {
        String fields = type.getFieldTypeMap().stream()
                .map(gqlDef -> String.format(DINDENT + "public %s %s%s;",
                        gqlDef.getStrippedRetType(),
                        gqlDef.getDefName(),
                        gqlDef.getParams().entrySet().stream()
                                .map(e -> Util.strip(e.getValue()) + " " + e.getKey())
                                .collect(Collectors.joining(", "))
                ))
                .collect(Collectors.joining("\n"));
        return String.format("public static class %s {\n" +
                        "%s\n" +
                        "}\n",
                type.getName(),
                fields);
    }

    private static String javaPlainInterface(Type type, String implUnion) {
        String methods = type.getFieldTypeMap().stream()
                .map(gqlDef -> String.format(DINDENT + "%s get%s(%s);",
                        gqlDef.getStrippedRetType(),
                        capitalize(gqlDef.getDefName()),
                        gqlDef.getParams().entrySet().stream()
                                .map(e -> Util.strip(e.getValue()) + " " + e.getKey())
                                .collect(Collectors.joining(", "))
                ))
                .collect(Collectors.joining("\n"));
        return String.format("public interface %s %s {\n" +
                "%s\n" +
                "}\n",
                type.getName(),
                implUnion == null ? "" : "extends " + implUnion,
                methods);
    }

    private static String typeDef(Type type) {
        return String.format("%s %s {\n" +
                INDENT + "%s\n" +
                "}\n",
                type.getTypename(),
                type.getName(),
                type.getFieldTypeMap().stream()
                        .map(gqlDef -> gqlDef.getDefName() + ": " + gqlDef.getRetName())
                        .collect(Collectors.joining("\n" + INDENT)));
    }

    private static String unionDef(Union union) {
        return String.format("union %s = %s",
                union.getName(),
                String.join(" | ", union.getSubTypes()));
    }

    private static String varContent(Map<String, String> variables) {
        if (variables.entrySet().isEmpty()) return "";
        return INDENT + "variables: {\n" +
                variables.entrySet().stream()
                        .map(e -> DINDENT + e.getKey() + ": " + e.getKey())
                        .collect(Collectors.joining(",\n")) +
                "\n" + INDENT + "}";
    }

    private static String optionAuth(Operation operation) {
        if (!operation.needAuth()) return "";
        if (!operation.getArgs().isEmpty()) return ",\n" + INDENT + "auth: auth";
        return INDENT + "auth: auth";
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
