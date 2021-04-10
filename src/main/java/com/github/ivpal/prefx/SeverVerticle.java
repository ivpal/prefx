package com.github.ivpal.prefx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SeverVerticle extends AbstractVerticle {
    private final Logger logger = LoggerFactory.getLogger(SeverVerticle.class);

    private final CompletionRepository completionRepository;

    public SeverVerticle(CompletionRepository completionRepository) {
        this.completionRepository = completionRepository;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        var router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.get("/completions").handler(this::handleGetCompletions);
        router.post("/completions").handler(this::handleCreateCompletions);

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8000)
            .onSuccess(server -> {
                logger.info("Started");
                startPromise.complete();
            })
            .onFailure(startPromise::fail);
    }

    private void handleGetCompletions(RoutingContext ctx) {
        var prefix = ctx.request().params().get("prefix");
        completionRepository.findForPrefix(prefix)
            .onSuccess(result -> ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(Json.encode(result)));
    }

    private void handleCreateCompletions(RoutingContext ctx) {
        // TODO: truncate query and completion
        var query = ctx.getBodyAsJson().getString("query");
        var completion = ctx.getBodyAsJson().getString("completion");
        completionRepository.addAll(prefixes(query), completion)
            .onSuccess(result -> ctx.response().setStatusCode(200).end());
    }

    private List<String> prefixes(String query) {
        var prefixes = new ArrayList<String>();
        for (int i = 1; i <= query.length(); i++) {
            prefixes.add(query.substring(0, i));
        }
        return prefixes;
    }
}
