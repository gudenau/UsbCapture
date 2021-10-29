package net.gudenau.usbcap.filter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import net.gudenau.usbcap.event.Event;
import net.gudenau.usbcap.event.PacketEvent;
import net.gudenau.usbcap.event.ResetEvent;
import org.jetbrains.annotations.NotNull;

/**
 * A filter that dumps events to a file to be handled later.
 */
public final class CaptureFilter implements Filter, AutoCloseable{
    private final SeekableByteChannel channel;
    private final ByteBuffer lengthBuffer = ByteBuffer.allocateDirect(Integer.BYTES);
    
    /**
     * Creates a new capture filter that writes events to the provided path.
     *
     * @param path The path to write to
     *
     * @throws IOException If the file could not be created or opened for writing
     */
    public CaptureFilter(Path path) throws IOException{
        path = path.toAbsolutePath();
        Files.createDirectories(path.getParent());
        Files.deleteIfExists(path);
        channel = Files.newByteChannel(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
    }
    
    @Override
    public void close() throws IOException{
        channel.close();
    }
    
    @Override
    public boolean handleEvent(@NotNull Event rawEvent){
        if(rawEvent instanceof PacketEvent event){
            var buffer = event.buffer();
            lengthBuffer.putInt(0, buffer.capacity());
            lengthBuffer.clear();
            write(lengthBuffer);
            write(buffer);
        }else if(rawEvent instanceof ResetEvent){
            lengthBuffer.putInt(0, -1);
            lengthBuffer.clear();
            write(lengthBuffer);
        }
        return false;
    }
    
    /**
     * Simple helper to write the contents of a buffer to the file.
     *
     * @param buffer The buffer to write.
     */
    private void write(ByteBuffer buffer){
        try{
            while(buffer.hasRemaining()){
                channel.write(buffer);
            }
        }catch(IOException e){
            throw new RuntimeException("Failed to write to capture file", e);
        }
    }
    
    @Override
    public @NotNull List<@NotNull Event> getPendingEvents(){
        return List.of();
    }
}
