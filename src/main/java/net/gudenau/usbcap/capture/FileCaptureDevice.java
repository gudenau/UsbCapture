package net.gudenau.usbcap.capture;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import net.gudenau.usbcap.UsbSpeed;
import net.gudenau.usbcap.event.Event;
import net.gudenau.usbcap.event.PacketEvent;
import net.gudenau.usbcap.event.ResetEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Reads USB packets that where previously captured from a file.
 *
 * The format is [signed int length][payload length bytes long]
 *
 * A length of -1 is for a reset event.
 */
public final class FileCaptureDevice extends AbstractCaptureDevice{
    private final Path path;
    private final ByteBuffer lengthBuffer = ByteBuffer.allocateDirect(Integer.BYTES);
    private SeekableByteChannel channel;
    
    /**
     * Creates a new capture device from the provided path.
     *
     * @param path The path of the file to read
     */
    public FileCaptureDevice(@NotNull Path path){
        Objects.requireNonNull(path, "path was null");
        this.path = path.toAbsolutePath();
    }
    
    @Override
    public void doOpen() throws IOException{
        channel = Files.newByteChannel(path, StandardOpenOption.READ);
    }
    
    // NOP
    @Override
    protected void doBeginCapture(UsbSpeed speed){}
    
    @Override
    protected void doCaptureEvent(List<Event> eventBuffer) throws IOException{
        if(channel.position() == channel.size()){
            throw new IOException("End of recorded data");
        }
        
        lengthBuffer.clear();
        read(lengthBuffer);
        int eventSize = lengthBuffer.getInt(0);
        if(eventSize == -1){
            eventBuffer.add(new ResetEvent());
        }else{
            var payload = ByteBuffer.allocate(eventSize);
            read(payload);
            eventBuffer.add(new PacketEvent(payload));
        }
    }
    
    private void read(ByteBuffer buffer) throws IOException{
        while(buffer.hasRemaining()){
            channel.read(buffer);
        }
    }
    
    @Override
    protected void doClose() throws IOException{
        channel.close();
    }
}
