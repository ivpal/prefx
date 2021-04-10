package com.github.ivpal.prefx;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class IgniteCompletionRepository implements CompletionRepository {
    private final Ignite ignite;
    private final Vertx vertx;

    public IgniteCompletionRepository(Ignite ignite, Vertx vertx) {
        this.ignite = ignite;
        this.vertx = vertx;
    }

    @Override
    public Future<Collection<String>> findForPrefix(String prefix) {
        return vertx.executeBlocking(promise -> {
            var cache = ignite.<String, LinkedHashMap<String, Long>>getOrCreateCache("completions");
            var items = cache.get(prefix);
            if (items == null) {
                items = new LinkedHashMap<>();
            }
            promise.complete(items.keySet());
        });
    }

    @Override
    public Future<Void> add(String prefix, String completion) {
        return vertx.executeBlocking(promise -> {
            var cache = ignite.<String, LinkedHashMap<String, Long>>getOrCreateCache("completions");
            upsert(cache, prefix, completion);
            promise.complete();
        });
    }

    @Override
    public Future<Void> addAll(List<String> prefixes, String completion) {
        return vertx.executeBlocking(promise -> {
            var cache = ignite.<String, LinkedHashMap<String, Long>>cache("completions");
            for (var prefix : prefixes) {
                upsert(cache, prefix, completion);
            }
            promise.complete();
        });
    }

    private void upsert(IgniteCache<String, LinkedHashMap<String, Long>> cache, String prefix, String completion) {
        var transactions = ignite.transactions();
        try (var tx = transactions.txStart()) {
            var items = cache.get(prefix);
            if (items == null) {
                items = new LinkedHashMap<>();
            }

            var rank = items.getOrDefault(completion, 0L);
            items.put(completion, rank + 1);
            var newItems = items.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(10)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            cache.put(prefix, newItems);
            tx.commit();
        }
    }
}
