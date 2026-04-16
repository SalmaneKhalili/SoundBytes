package audioresource.source;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class TCPSource implements Source{
    private Socket socket;
    private InputStream inputStream;
    private long position = 0;
    private int bytesRead = 0;


    public TCPSource (String host, int port) throws IOException {
        socket = new Socket(host, port);
        inputStream = socket.getInputStream();
    }
    @Override
    public boolean isSeekable(){return false;}
    @Override
    public void open() {
        System.out.println("You do not need to open a socket");
    }
    @Override
    public int read(ByteBuffer buffer) {
        try {
            int remaining = buffer.remaining();
            if (remaining == 0) return 0;
            bytesRead = inputStream.read(buffer.array(), buffer.position(), remaining);
            if (bytesRead > 0) {
                buffer.position(buffer.position() + bytesRead);
                position += bytesRead;
            }
            return bytesRead;
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
