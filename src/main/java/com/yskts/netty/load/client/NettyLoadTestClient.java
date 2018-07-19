package com.yskts.netty.load.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class NettyLoadTestClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyLoadTestClient.class);

    private static final Function<String, FullHttpRequest> DEFAULT_FULL_HTTP_REQUEST_FUNCTION = uri -> new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
    private static final Consumer<Result> DEFAULT_RESULT_CONSUMER = r -> {};

    private final CompletableFuture<Void> testComplete = new CompletableFuture<>();

    private final String url;
    private final int initialConnections;
    private final int  testDuration;
    private final int maxConnections;
    private final int addConnectionDelay;
    private final boolean ramp;
    private final EventLoopGroup eventLoopGroup;

    private final Function<String, FullHttpRequest> requestFunction;
    private final Supplier<FullHttpRequest> fullHttpRequestSupplier;
    private final Consumer<Result> resultConsumer;

    private final Bootstrap bootstrap;

    private final List<Channel> channelList = new ArrayList<>();

    public NettyLoadTestClient(String url, int initialConnections, int testDuration) {
        this(url, initialConnections, testDuration, initialConnections, 0, DEFAULT_RESULT_CONSUMER, DEFAULT_FULL_HTTP_REQUEST_FUNCTION);
    }

    public NettyLoadTestClient(String url, int initialConnections, int testDuration, int maxConnections, int addConnectionDelay, Consumer<Result> resultConsumer, Function<String, FullHttpRequest> requestFunction) {

        this.url = url;
        this.initialConnections = initialConnections;
        this.testDuration = testDuration;
        this.maxConnections = maxConnections > initialConnections ? maxConnections : initialConnections;
        this.addConnectionDelay = addConnectionDelay;
        this.requestFunction = requestFunction;
        this.resultConsumer = resultConsumer;

        this.ramp = maxConnections > initialConnections;

        if (Epoll.isAvailable()) {
            this.eventLoopGroup = new EpollEventLoopGroup(maxConnections + 1);
        } else {
            this.eventLoopGroup = new NioEventLoopGroup(maxConnections + 1);
        }

        // Schedule the test completion
        eventLoopGroup.schedule(() -> testComplete.complete(null), testDuration, TimeUnit.SECONDS);

        try {
            final URI uri = new URI(url);
            final String path = uri.getPath();
            this.fullHttpRequestSupplier = () -> requestFunction.apply(path);

            // Initialize the Bootstrap
            this.bootstrap = new Bootstrap();
            bootstrap.remoteAddress(uri.getHost(), uri.getPort());
            bootstrap.group(eventLoopGroup);
            bootstrap.channel(getChannelClass());
            bootstrap.handler(new NettyLoadTestInitializer(fullHttpRequestSupplier, resultConsumer, testComplete));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CompletableFuture<Void> start() {

        LOGGER.info("Starting Client...");
        for (int i = 0; i < initialConnections; i++) {
            addConnection();
        }

        // Schedule ramp
        if (channelList.size() < maxConnections) {
            eventLoopGroup.schedule(this::ramp, addConnectionDelay, TimeUnit.SECONDS);
        }

        return testComplete;
    }

    private void ramp() {

        addConnection();

        // Schedule adding connections until we max out
        if (channelList.size() < maxConnections) {
            eventLoopGroup.schedule(this::ramp, addConnectionDelay, TimeUnit.SECONDS);
        }
    }

    private void addConnection() {
        try {
            if (channelList.size() < maxConnections) {
                LOGGER.info("Adding Connection...");
                final Channel channel = bootstrap.connect().sync().channel();
                channelList.add(channel);

                // Start the sending
                LOGGER.info("Beginning sending...");
                channel.writeAndFlush(fullHttpRequestSupplier.get());
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Class<? extends Channel> getChannelClass() {
        if (Epoll.isAvailable()) {
            return EpollSocketChannel.class;
        } else {
            return NioSocketChannel.class;
        }
    }

    public static class Builder {

        private final String url;
        private final int initialConnections;
        private final int  testDuration;

        private int maxConnections = 0;
        private int addConnectionDelay = 0;

        private Consumer<Result> resultConsumer = DEFAULT_RESULT_CONSUMER;
        private Function<String, FullHttpRequest> requestFunction = DEFAULT_FULL_HTTP_REQUEST_FUNCTION;

        public Builder(String url, int initialConnections, int testDuration) {
            this.url = url;
            this.initialConnections = initialConnections;
            this.testDuration = testDuration;
        }

        public Builder withRamp(final int maxConnections, final int addConnectionDelay) {
            this.maxConnections = maxConnections;
            this.addConnectionDelay = addConnectionDelay;
            return this;
        }

        public Builder withResultConsumer(final Consumer<Result> resultConsumer) {
            this.resultConsumer = resultConsumer;
            return this;
        }

        public Builder withRequestFunction(final Function<String, FullHttpRequest> requestFunction) {
            this.requestFunction = requestFunction;
            return this;
        }

        public NettyLoadTestClient build() {
            return new NettyLoadTestClient(url, initialConnections, testDuration, maxConnections, addConnectionDelay, resultConsumer, requestFunction);
        }
    }
}
