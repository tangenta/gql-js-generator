package com.tangenta.gqljs;

import com.tangenta.gqljs.schemaType.*;
import com.tangenta.gqljs.schemaType.util.Util;
import lombok.val;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public class Schema {
    public final Map<String, Query> queries;
    public final Map<String, Mutation> mutations;
    public final Map<String, Type> types;
    public final Map<String, Union> unions;
    public final Map<String, Enume> enums;

    private Schema(List<Query> queries, List<Mutation> mutations, List<Type> types, List<Union> unions, List<Enume> enums) {
        this.queries = Collections.unmodifiableMap(buildMap(queries, Query::getName));
        this.mutations = Collections.unmodifiableMap(buildMap(mutations, Mutation::getName));
        this.types = Collections.unmodifiableMap(buildMap(types, Type::getName));
        this.unions = Collections.unmodifiableMap(buildMap(unions, Union::getName));
        this.enums = Collections.unmodifiableMap(buildMap(enums, Enume::getName));
    }

    private static <T> Map<String, T> buildMap(List<T> list, Function<T, String> keyExtractor) {
        val map = new LinkedHashMap<String, T>();
        list.forEach(item -> {
            map.put(keyExtractor.apply(item), item);
        });
        return map;
    }

    public Stream<Operation> allOperations() {
        return Stream.concat(
                queries.values().stream(),
                mutations.values().stream()
        );
    }

    public Optional<Type> findType(String name) {
        return Optional.ofNullable(types.get(name));
    }

    public Optional<Union> findUnion(String name) {
        return Optional.ofNullable(unions.get(name));
    }

    public Map<String, String> allVariablesForOper(String operName) {
        val map = new LinkedHashMap<String, String>();
        Query query = queries.get(operName);
        Mutation mut = mutations.get(operName);

        if (query != null) {
            map.putAll(query.getArgs());
            map.putAll(allVariablesInType(query.getStrippedRetType()));
            map.putAll(allVariablesInUnion(query.getStrippedRetType()));
        } else if (mut != null) {
            map.putAll(mut.getArgs());
            map.putAll(allVariablesInType(mut.getStrippedRetType()));
            map.putAll(allVariablesInUnion(mut.getStrippedRetType()));
        }
        return map;
    }

    private Map<String, String> allVariablesInType(String typename) {
        val map = new LinkedHashMap<String, String>();
        Type type = types.get(typename);
        if (type == null) return map;

        type.getFieldTypeMap().forEach(gqlDef -> {
            map.putAll(gqlDef.getParams());
            map.putAll(allVariablesInType(gqlDef.getStrippedRetType()));
            map.putAll(allVariablesInUnion(gqlDef.getStrippedRetType()));
        });

        return map;
    }

    private Map<String, String> allVariablesInUnion(String unionName) {
        val map = new LinkedHashMap<String, String>();
        Union union = unions.get(unionName);
        if (union == null) return map;

        union.getSubTypes().forEach(subTypes -> {
            map.putAll(allVariablesInType(subTypes));
        });
        return map;
    }

    public static class SchemaBuilder {
        private List<Query> queries = new LinkedList<>();
        private List<Mutation> mutations = new LinkedList<>();
        private List<Type> types = new LinkedList<>();
        private List<Union> unions = new LinkedList<>();
        private List<Enume> enums = new LinkedList<>();

        public SchemaBuilder appendQuery(Query query) {
            queries.add(query);
            return this;
        }

        public SchemaBuilder appendMutation(Mutation mutation) {
            mutations.add(mutation);
            return this;
        }

        public SchemaBuilder appendType(Type type) {
            types.add(type);
            return this;
        }
        public SchemaBuilder appendUnion(Union union) {
            unions.add(union);
            return this;
        }
        public SchemaBuilder appendEnume(Enume enume) {
            enums.add(enume);
            return this;
        }
        public Schema build() {
            checkValidation();
            return new Schema(queries, mutations, types, unions, enums);
        }

        private void checkValidation() {
            String[] names = concat(
                    types.stream().map(Type::getName),
                    unions.stream().map(Union::getName),
                    enums.stream().map(Enume::getName)
            ).sorted().toArray(String[]::new);

            Arrays.stream(names).reduce("", (last, cur) -> {
                if (cur.equals(last)) {
                    throw new RuntimeException("name: " + cur + " is used for more than once in (union | type | enum)");
                }
                return cur;
            });

            concat(
                    queries.stream().map(Query::getStrippedRetType),
                    queries.stream().flatMap(query -> query.getArgs().values().stream().map(Util::strip)),
                    mutations.stream().map(Mutation::getStrippedRetType),
                    mutations.stream().flatMap(mutation -> mutation.getArgs().values().stream().map(Util::strip)),
                    unions.stream().flatMap(union -> union.getSubTypes().stream())
            ).forEach(type -> {
                if (!Type.isScalar(type) && Arrays.binarySearch(names, type) < 0) {
                    throw new RuntimeException("type: " + type + " is not defined");
                }
            });
        }

        @SafeVarargs
        private static <T> Stream<T> concat(Stream<T>... streams) {
            return Arrays.stream(streams).reduce(Stream.empty(), Stream::concat);
        }
    }

    public static SchemaBuilder getBuilder() {
        return new SchemaBuilder();
    }
}
