package top.devgo.graphitej;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

final class NonBlockingTcpSender {
    private final Charset encoding;
    private final SocketChannel clientSocket;
    private final ExecutorService executor;
    private GraphiteJErrorHandler handler;

    NonBlockingTcpSender(String hostname, int port, Charset encoding, GraphiteJErrorHandler handler) throws IOException {
        this.encoding = encoding;
        this.handler = handler;
        this.clientSocket = SocketChannel.open();
        this.clientSocket.connect(new InetSocketAddress(hostname, port));

        this.executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            final ThreadFactory delegate = Executors.defaultThreadFactory();
            @Override public Thread newThread(Runnable r) {
                Thread result = delegate.newThread(r);
                result.setName("GraphiteJ-" + result.getName());
                result.setDaemon(true);
                return result;
            }
        });
    }

    void stop() {
        try {
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            handler.handle(e);
        }
        finally {
            if (clientSocket != null) {
                try {
                    clientSocket.close();
                }
                catch (Exception e) {
                    handler.handle(e);
                }
            }
        }
    }

    void send(final String message) {
        try {
            executor.execute(() -> socketSend(message));
        }
        catch (Exception e) {
            handler.handle(e);
        }
    }

    private void socketSend(String message) {
        try {
            final byte[] sendData = message.getBytes(encoding);
            clientSocket.write(ByteBuffer.wrap(sendData));
        } catch (Exception e) {
            handler.handle(e);
        }
    }
}