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
    private final int MAX_PREFIX_LENGTH = 30;
    private final int MAX_COMPLETION_LENGTH = 60;
    private final Logger logger = LoggerFactory.getLogger(SeverVerticle.class);

    private final CompletionRepository completionRepository;

    public SeverVerticle(CompletionRepository completionRepository) {
        this.completionRepository = completionRepository;
    }

    @Override
    public void start(Promise<Void> startPromise) {
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
        var prefix = ctx.request().getParam("prefix");
        if (prefix.length() > MAX_PREFIX_LENGTH) {
            prefix = prefix.substring(0, MAX_PREFIX_LENGTH);
        }
        completionRepository.findForPrefix(prefix)
            .onSuccess(result -> ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(Json.encode(result)))
            .onFailure(ex -> {
                if (logger.isDebugEnabled()) {
                    ex.printStackTrace();
                }
                ctx.response().setStatusCode(500).end(ex.getMessage());
            });
    }

    private void handleCreateCompletions(RoutingContext ctx) {
        var body = ctx.getBodyAsJson();
        var query = body.getString("query");
        if (query.length() > MAX_PREFIX_LENGTH) {
            query = query.substring(0, MAX_PREFIX_LENGTH);
        }

        var completion = body.getString("completion");
        if (completion.length() > MAX_COMPLETION_LENGTH) {
            completion = completion.substring(0, MAX_COMPLETION_LENGTH);
        }

        completionRepository.addAll(prefixes(query), completion)
            .onSuccess(result -> ctx.response().setStatusCode(200).end())
            .onFailure(ex -> {
                if (logger.isDebugEnabled()) {
                    ex.printStackTrace();
                }
                ctx.response().setStatusCode(500).end(ex.getMessage());
            });
    }

    private List<String> prefixes(String query) {
        var prefixes = new ArrayList<String>();
        for (int i = 1; i <= query.length(); i++) {
            prefixes.add(query.substring(0, i));
        }
        return prefixes;
    }
}
