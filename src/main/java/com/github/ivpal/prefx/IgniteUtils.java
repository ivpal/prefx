package com.github.ivpal.prefx;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.lang.IgniteInClosure;

public class IgniteUtils {
    public static <V> Future<V> fromIgniteFuture(IgniteFuture<V> igniteFuture) {
        var promise = Promise.<V>promise();
        var igniteInClosure = (IgniteInClosure<IgniteFuture<V>>) completedIgniteFuture -> {
            V result = null;
            Throwable ex = null;
            try {
                result = completedIgniteFuture.get();
            } catch (Throwable e) {
                ex = e;
            }

            if (ex == null) {
                promise.complete(result);
            } else {
                promise.fail(ex);
            }
        };

        igniteFuture.listen(igniteInClosure);
        return promise.future();
    }
}
