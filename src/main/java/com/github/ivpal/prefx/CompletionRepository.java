package com.github.ivpal.prefx;

import io.vertx.core.Future;

import java.util.Collection;
import java.util.List;

public interface CompletionRepository {
    Future<Collection<String>> findForPrefix(String prefix);
    Future<Void> add(String prefix, String completion);
    Future<Void> addAll(List<String> prefixes, String completion);
}
