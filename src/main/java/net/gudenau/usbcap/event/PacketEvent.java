package net.gudenau.usbcap.event;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import net.gudenau.usbcap.internal.BufferHelper;

/**
 * A generic USB packet event.
 *
 * @param buffer The contents of the packet, not including sync bits.
 */
public record PacketEvent(
    ByteBuffer buffer
) implements Event.Networked<PacketEvent>{
    public static final String PACKET_EVENT = "packet_event";
    
    public PacketEvent{
        Objects.requireNonNull(buffer, "buffer was null");
        buffer.order(ByteOrder.LITTLE_ENDIAN);
    }
    
    @Override
    public void reset(){
        buffer.clear();
    }
    
    @Override
    public String getId(){
        return PACKET_EVENT;
    }
    
    @Override
    public String toString(){
        var builder = new StringBuilder("PacketEvent[buffer=[");
        BufferHelper.toString(builder, buffer);
        return builder.append("]]").toString();
    }
    
    @Override
    public ByteBuffer write(){
        return buffer.clear();
    }
}
