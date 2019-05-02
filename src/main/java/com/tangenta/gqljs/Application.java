package com.tangenta.gqljs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Application {
    public static void main(String[] args) throws IOException {
        Schema schema = buildSchemaFromFile("D:\\bbs-gql-test\\bbsgql.txt");
        printJSConst(schema, "D:\\bbs-gql-test\\bbsgql-const.js");
        printJSFunc(schema, "D:\\bbs-gql-test\\bbsgql-func.js");
//        printJava(schema, "cancelClaimFound");
    }

    public static Schema buildSchemaFromFile(String fileName) throws IOException {
        byte[] allBytes = Files.readAllBytes(Paths.get(fileName));
        return SchemaParser.parseSchema(new String(allBytes));
    }

    public static void printJava(Schema schema, String schemaType) {
        System.out.println(GQLPrinter.toDSL(schema, schemaType));
        System.out.println(GQLPrinter.toJavaImpl(schema, schemaType));
    }

    public static void printJSConst(Schema schema, String filePath) throws IOException {
        Files.write(Paths.get(filePath),
                GQLPrinter.toJSConst(schema).getBytes(StandardCharsets.UTF_8));
    }

    public static void printJSFunc(Schema schema, String filePath) throws IOException {
        Files.write(Paths.get(filePath),
                GQLPrinter.toJSFunc(schema).getBytes(StandardCharsets.UTF_8));
    }
}
