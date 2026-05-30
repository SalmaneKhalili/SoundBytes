package audioresource.source;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class TCPSource implements Source{

    private SocketChannel channel;
    private long position = 0;
    private String host;
    private int port;



    public TCPSource (String host, int port) {
        this.host = host;
        this.port = port;
    }
    @Override
    public boolean isSeekable(){return false;}
    @Override
    public void open() {
        try {
            channel = SocketChannel.open(new InetSocketAddress(host, port));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public int read(ByteBuffer buffer) {
        try {
            return channel.read(buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public long seek(long bytePosition) {
        throw new IllegalArgumentException("TCP Stream is not seekable");
    }
    @Override
    public long getPosition() {
        return this.position;
    }
    @Override
    public void close() {
        try{
            if (channel != null) {
                channel.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
 }
