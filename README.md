# netty-load-client

Use the builder to make a client that sends load to a URL for the specified duration. Supports ramping up connections from a base value. All specified times are in seconds, however timing is done in nanoseconds. 

```
new NettyLoadTestClient.Builder("http://www.google.com:80/index.html", 1, 20)
        .withRamp(10, 1)
        .build();
```

You can also supply a request supplier function:
```
.withRequestFunction(uri -> new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
```

And you can supply a Result consumer:
```
.withResultConsumer(this::handleResult)
```

The Result object has all the timing info in it and your request response if you want to do response validation:

```
Result {
    private long start;
    private long sendLatency;
    private long responseLatency;
    private long totalLatency;
    private FullHttpRequest httpRequest;
    private FullHttpResponse httpResponse;
}
```
