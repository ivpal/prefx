package com.github.ivpal.prefx;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.spi.cluster.ignite.IgniteClusterManager;

public class Application {
    public static void main(String[] args) {
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");

        var clusterManager = new IgniteClusterManager();
        var options = new VertxOptions().setClusterManager(clusterManager);
        Vertx.clusteredVertx(options, res -> {
            if (res.succeeded()) {
                var vertx = res.result();
                vertx.deployVerticle(new BootstrapVerticle(clusterManager.getIgniteInstance()));
            }
        });
    }
}
