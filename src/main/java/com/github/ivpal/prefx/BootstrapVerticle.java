package com.github.ivpal.prefx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.apache.ignite.Ignite;

public class BootstrapVerticle extends AbstractVerticle {
    private final Ignite ignite;

    public BootstrapVerticle(Ignite ignite) {
        this.ignite = ignite;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
    }
}
