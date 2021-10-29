package net.gudenau.usbcap.event;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import net.gudenau.usbcap.internal.BufferHelper;
import org.jetbrains.annotations.NotNull;

/**
 * A generic data transfer event.
 *
 * @param direction The direction of the transfer
 * @param address The address of the USB device
 * @param endpoint The endpoint of the USB device
 * @param data The payload of the transfer
 */
public record DataEvent(
    @NotNull Direction direction,
    int address,
    int endpoint,
    @NotNull ByteBuffer data
) implements Event{
    public static final String DATA_EVENT = "data_event";
    
    public DataEvent{
        Objects.requireNonNull(direction, "direction was null");
        if((address & ~0b01111111) != 0){
            throw new IllegalArgumentException("Address was out of range, range is \"128 < address <= 0\" and got " + address);
        }
        if((endpoint & ~0b00011111) != 0){
            throw new IllegalArgumentException("Endpoint was out of range, range is \"32 < endpoint <= 0\" and got " + endpoint);
        }
        Objects.requireNonNull(data, "data was null");
        data.order(ByteOrder.LITTLE_ENDIAN);
    }
    
    @Override
    public void reset(){
        data().clear();
    }
    
    @Override
    public String getId(){
        return DATA_EVENT;
    }
    
    @Override
    public String toString(){
        var builder = new StringBuilder("DataEvent[direction=")
            .append(direction == Direction.IN ? "in" : "out")
            .append(",address=")
            .append(address)
            .append(",endpoint=")
            .append(endpoint)
            .append(",data=[");
        BufferHelper.toString(builder, data());
        builder.append("]]");
        
        return builder.toString();
    }
    
    /**
     * An enumeration of the directions for a USB transfer.
     */
    public enum Direction{
        /**
         * A transfer from the device to the host.
         */
        IN,
        /**
         * A transfer from the host to the device.
         */
        OUT
    }
}
