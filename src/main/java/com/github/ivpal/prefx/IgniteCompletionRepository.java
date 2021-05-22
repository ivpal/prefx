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
    public static final String CACHE_NAME = "completions";

    private final Ignite ignite;
    private final Vertx vertx;

    public IgniteCompletionRepository(Ignite ignite, Vertx vertx) {
        this.ignite = ignite;
        this.vertx = vertx;
    }

    @Override
    public Future<Collection<String>> findForPrefix(String prefix) {
        return vertx.executeBlocking(promise -> {
            try {
                var cache = ignite.<String, LinkedHashMap<String, Long>>cache(CACHE_NAME);
                var items = cache.get(prefix);
                if (items == null) {
                    items = new LinkedHashMap<>();
                }
                promise.complete(items.keySet());
            } catch (Throwable ex) {
                promise.fail(ex);
            }
        });
    }

    @Override
    public Future<Void> add(String prefix, String completion) {
        return vertx.executeBlocking(promise -> {
            var cache = ignite.<String, LinkedHashMap<String, Long>>getOrCreateCache(CACHE_NAME);
            var transactions = ignite.transactions();
            try (var tx = transactions.txStart()) {
                upsert(cache, prefix, completion);
                tx.commit();
            } catch (Throwable ex) {
                promise.fail(ex);
                return;
            }

            promise.complete();
        });
    }

    @Override
    public Future<Void> addAll(List<String> prefixes, String completion) {
        return vertx.executeBlocking(promise -> {
            var cache = ignite.<String, LinkedHashMap<String, Long>>cache(CACHE_NAME);
            var transactions = ignite.transactions();
            try (var tx = transactions.txStart()) {
                for (var prefix : prefixes) {
                    upsert(cache, prefix, completion);
                }
                tx.commit();
            } catch (Throwable ex) {
                promise.fail(ex);
                return;
            }

            promise.complete();
        });
    }

    private void upsert(IgniteCache<String, LinkedHashMap<String, Long>> cache, String prefix, String completion) {
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
    }
}
