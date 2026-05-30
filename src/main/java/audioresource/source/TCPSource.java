package audioresource.source;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class TCPSource implements Source{
    private Socket socket;
    private InputStream inputStream;
    private SocketChannel channel;
    private String host;
    private int port;
    private long position = 0;
    private int bytesRead = 0;


    public TCPSource (String host, int port) {
        try {
            channel = SocketChannel.open(new InetSocketAddress(host, port));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public boolean isSeekable(){return false;}
    @Override
    public void open() {

        try {
            socket = new Socket(host, port);
            inputStream = socket.getInputStream();
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
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
 }
