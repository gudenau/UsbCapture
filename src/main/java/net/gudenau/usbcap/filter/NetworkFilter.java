package net.gudenau.usbcap.filter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import net.gudenau.usbcap.event.Event;
import net.gudenau.usbcap.event.PacketEvent;
import net.gudenau.usbcap.event.ResetEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Connects to a {@link net.gudenau.usbcap.capture.NetworkCaptureDevice} and sends events to it.
 */
public final class NetworkFilter implements Filter, AutoCloseable{
    private static final long MAGIC_A = 0x7653B03E21444C9AL;
    private static final long MAGIC_B = 0xC35457DD810F2342L;
    
    private final Socket socket;
    private final InputStream input;
    private final OutputStream output;
    
    /**
     * Connects to the capture device at the provided address listening on port 8765.
     *
     * @param address The address of the server
     *
     * @throws IOException If the connection could not be established
     */
    public NetworkFilter(String address) throws IOException{
        this(address, 8765);
    }
    
    /**
     * Connects to the capture device at the provided address listening on provided port.
     *
     * @param address The address of the server
     * @param port The port to connect to
     *
     * @throws IOException If the connection could not be established
     */
    public NetworkFilter(String address, int port) throws IOException{
        Socket socket = null;
        try{
            socket = new Socket(address, port);
            input = socket.getInputStream();
            output = socket.getOutputStream();
            var data = new byte[8];
            var buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            read(data);
            if(buffer.getLong(0) != MAGIC_A){
                throw new IOException("Received the wrong magic");
            }
            buffer.putLong(0, MAGIC_B);
            write(data);
        }catch(IOException e){
            if(socket != null){
                try{
                    socket.close();
                }catch(IOException ignored){}
            }
            throw e;
        }
        this.socket = socket;
    }
    
    private void read(byte[] buffer) throws IOException{
        if(input.readNBytes(buffer, 0, buffer.length) != buffer.length){
            throw new IOException("Unexpected end of stream");
        }
    }
    
    private void write(byte[] buffer) throws IOException{
        output.write(buffer);
        output.flush();
    }
    
    @Override
    public boolean handleEvent(@NotNull Event event){
        var type = event.getClass();
        if(!(event instanceof Event.Networked<?> networked)){
            throw new IllegalStateException("Unsupported event: %s/%s".formatted(type.getModule().getName(), type.getName()));
        }
        byte eventType = -1;
        if(event instanceof ResetEvent){
            eventType = 0;
        }else if(event instanceof PacketEvent){
            eventType = 1;
        }
        if(eventType == -1){
            throw new RuntimeException("Illegal event type: " + event.getId());
        }
        
        var payload = networked.write();
        var data = new byte[5 + (payload == null ? 0 : payload.remaining())];
        var buffer = ByteBuffer.wrap(data)
            .order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(eventType);
        if(payload != null){
            buffer.putInt(payload.remaining());
            buffer.put(payload);
        }else{
            buffer.putInt(-1);
        }
        try{
            write(data);
        }catch(IOException e){
            throw new RuntimeException("Failed to write event", e);
        }
    
        return true;
    }
    
    @Override
    public @NotNull List<@NotNull Event> getPendingEvents(){
        return List.of();
    }
    
    @Override
    public void close() throws IOException{
        socket.close();
    }
}
