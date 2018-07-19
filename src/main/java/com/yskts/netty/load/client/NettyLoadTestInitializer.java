package com.yskts.netty.load.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class NettyLoadTestInitializer extends ChannelInitializer<Channel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyLatencyTimerChannelHandler.class);

    private final Supplier<FullHttpRequest> fullHttpRequestSupplier;
    private final Consumer<Result> resultConsumer;
    private final CompletableFuture<Void> testComplete;

    public NettyLoadTestInitializer(Supplier<FullHttpRequest> fullHttpRequestSupplier, Consumer<Result> resultConsumer, CompletableFuture<Void> testComplete) {
        this.fullHttpRequestSupplier = fullHttpRequestSupplier;
        this.resultConsumer = resultConsumer;
        this.testComplete = testComplete;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {

        LOGGER.info("Initializing Pipeline...");


        final ChannelPipeline p = channel.pipeline();

        p.addLast(new HttpClientCodec());
        p.addLast(new HttpObjectAggregator(1024 * 1014));
        p.addLast(new NettyLatencyTimerChannelHandler(fullHttpRequestSupplier, resultConsumer, testComplete));
    }
}
