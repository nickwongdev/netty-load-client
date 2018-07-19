package com.yskts.netty.load.client;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;

public class NettyLoadTestClientTest {

    @Test
    public void start() {
        final NettyLoadTestClient nettyLoadTestClient =
                new NettyLoadTestClient.Builder("http://www.google.com:80/index.html", 1, 20)
                        .withRamp(10, 1)
                        .build();

        final CompletableFuture<Void> completableFuture = nettyLoadTestClient.start();
        completableFuture.join();
    }

    private void handleResult(final Result result) {
        System.out.println(result.toString());
    }
}