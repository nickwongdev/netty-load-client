package com.yskts.netty.load.client;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class NettyLatencyTimerChannelHandler extends ChannelDuplexHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyLatencyTimerChannelHandler.class);

    private final Supplier<FullHttpRequest> fullHttpRequestSupplier;
    private final Consumer<Result> resultConsumer;
    private final CompletableFuture<Void> testComplete;

    // This is Synchronous, so there's no chance of async issues
    private Result result;
    private long sendStartNanoTime;
    private long sendEndNanoTime;

    public NettyLatencyTimerChannelHandler(Supplier<FullHttpRequest> fullHttpRequestSupplier, Consumer<Result> resultConsumer, CompletableFuture<Void> testComplete) {
        this.fullHttpRequestSupplier = fullHttpRequestSupplier;
        this.resultConsumer = resultConsumer;
        this.testComplete = testComplete;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

        LOGGER.info("Writing to Channel...");

        result = new Result();

        // Setup Timers
        result.setStart(System.currentTimeMillis());
        this.sendStartNanoTime = System.nanoTime();
        promise.addListener(f -> sendEndNanoTime = System.nanoTime());

        final FullHttpRequest request = (FullHttpRequest) msg;
        result.setHttpRequest(request);
        ctx.writeAndFlush(request);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        LOGGER.info("Reading from Channel...");

        // Measure latency
        final long receiveEndTime = System.nanoTime();
        result.setSendLatency(sendEndNanoTime - sendStartNanoTime);
        result.setResponseLatency(receiveEndTime - sendEndNanoTime);
        result.setTotalLatency(receiveEndTime - sendStartNanoTime);
        result.setHttpResponse((FullHttpResponse) msg);
        resultConsumer.accept(result);

        // Force Release
        ((FullHttpResponse) msg).release();

        // Send again immediately if we are running
        if (!testComplete.isDone()) {
            ctx.channel().writeAndFlush(fullHttpRequestSupplier.get());
        } else {
            ctx.channel().close().sync();
        }
    }
}
