package net.gudenau.usbcap.capture;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import net.gudenau.usbcap.UsbSpeed;
import net.gudenau.usbcap.event.Event;
import net.gudenau.usbcap.event.PacketEvent;
import net.gudenau.usbcap.event.ResetEvent;

/**
 * Creates a server for a client to connect to, allowing for remote packet capture over a network.
 */
public final class NetworkCaptureDevice extends AbstractCaptureDevice{
    private static final long MAGIC_A = 0x7653B03E21444C9AL;
    private static final long MAGIC_B = 0xC35457DD810F2342L;
    
    private Socket socket = null;
    private final int port;
    private InputStream input;
    private OutputStream output;
    
    /**
     * Creates a new capture device that listens on the default port, 8765.
     */
    public NetworkCaptureDevice(){
        this(8765);
    }
    
    /**
     * Creates a new capture device that listens on the provided port.
     *
     * @param port The port to listen on
     */
    public NetworkCaptureDevice(int port){
        this.port = port;
    }
    
    @Override
    public void doOpen() throws IOException{
        // We only want a single connection.
        try(var server = new ServerSocket(port)){
            socket = server.accept();
        }
        try{
            input = socket.getInputStream();
            output = socket.getOutputStream();
            byte[] data = new byte[8];
            // Small handshake to ensure some sanity.
            ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            buffer.putLong(0, MAGIC_A);
            write(data);
            read(data);
            if(buffer.getLong(0) != MAGIC_B){
                throw new IOException("Bad magic received");
            }
        }catch(IOException e){
            try{
                close();
            }catch(IOException ignored){}
            throw e;
        }
    }
    
    /**
     * Small helper to read a byte array from the socket.
     *
     * @param data Buffer to read into
     *
     * @throws IOException If the buffer could not be read fully
     */
    private void read(byte[] data) throws IOException{
        if(input.readNBytes(data, 0, data.length) != data.length){
            throw new IOException("Unexpected end of stream");
        }
    }
    
    /**
     * Small helper to write a byte array to the socket.
     *
     * @param data The data to write
     *
     * @throws IOException If the buffer could not be written fully
     */
    private void write(byte[] data) throws IOException{
        output.write(data);
        output.flush();
    }
    
    @Override
    protected void doBeginCapture(UsbSpeed speed){}
    
    @Override
    protected void doCaptureEvent(List<Event> eventBuffer) throws IOException{
        int length;
        byte type;
        {
            byte[] data = new byte[5];
            read(data);
            var buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            type = buffer.get();
            length = buffer.getInt();
        }
        
        ByteBuffer buffer = null;
        if(length != -1){
            byte[] data = new byte[length];
            read(data);
            buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        }
        
        eventBuffer.add(switch(type){
            case 0 -> new ResetEvent();
            case 1 -> new PacketEvent(buffer);
            default -> throw new IOException("Unknown event type: " + type);
        });
    }
    
    @Override
    public void doClose() throws IOException{
        if(socket != null){
            socket.close();
        }
    }
}
