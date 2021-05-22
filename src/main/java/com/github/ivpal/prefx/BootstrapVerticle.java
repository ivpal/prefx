package com.github.ivpal.prefx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;

import static com.github.ivpal.prefx.IgniteCompletionRepository.CACHE_NAME;

public class BootstrapVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(BootstrapVerticle.class);

    private final Ignite ignite;

    public BootstrapVerticle(Ignite ignite) {
        this.ignite = ignite;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        var repository = new IgniteCompletionRepository(ignite, vertx);
        ignite.<String, LinkedHashMap<String, Long>>createCache(CACHE_NAME);
        vertx.deployVerticle(new SeverVerticle(repository))
            .onSuccess(result -> {
                logger.info("Started");
                startPromise.complete();
            })
            .onFailure(Throwable::printStackTrace);
    }
}
