package com.suse.matcher.deduction;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Generates stable numeric identifiers for fact groups within a single matching run.
 *
 * Equal namespace/object tuples always receive the same id from one generator instance.
 * Different tuples receive different ids, assigned sequentially starting at zero.
 */
public class FactIdGenerator {

    private static final String DEFAULT_NAMESPACE = "root";

    private final Map<Key, Integer> ids;

    public FactIdGenerator() {
        ids = new HashMap<>();
    }

    /**
     * Generates an id in the default namespace.
     *
     * @param objects the values identifying the fact group
     * @return a stable id for the specified values
     */
    public int generate(Object... objects) {
        return generate(DEFAULT_NAMESPACE, objects);
    }

    /**
     * Generates an id in a namespace derived from a class name.
     *
     * @param namespaceClass the class whose simple name is used as the namespace
     * @param objects the values identifying the fact group
     * @return a stable id for the specified class namespace and values
     * @throws NullPointerException if the namespace class or one of the values is {@code null}
     */
    public int generate(Class<?> namespaceClass, Object... objects) {
        return generate(Objects.requireNonNull(namespaceClass).getSimpleName(), objects);
    }

    /**
     * Generates an id for a namespaced tuple of values.
     *
     * @param namespace the namespace separating this tuple from otherwise equal tuples
     * @param objects the values identifying the fact group
     * @return a stable id for the specified namespace and values
     * @throws NullPointerException if the namespace or one of the values is {@code null}
     */
    public int generate(String namespace, Object... objects) {
        Key key = new Key(Objects.requireNonNull(namespace), Arrays.asList(objects));
        return ids.computeIfAbsent(key, k -> ids.size());
    }

    private record Key(String namespace, List<Object> objects) {}
}
