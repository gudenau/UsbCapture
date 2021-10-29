package net.gudenau.usbcap.event;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import net.gudenau.usbcap.internal.BufferHelper;
import org.jetbrains.annotations.NotNull;

/**
 * A data event for a SETUP request.
 *
 * @param address The address of the device
 * @param endpoint The endpoint of the device
 * @param data The payload of the request
 */
public record SetupDataEvent(
    int address,
    int endpoint,
    @NotNull ByteBuffer data
) implements Event{
    public static final String SETUP_DATA_EVENT = "setup_data_event";
    
    public SetupDataEvent{
        if((address & ~0b01111111) != 0){
            throw new IllegalArgumentException("Address was out of range, range is \"128 < address <= 0\" and got " + address);
        }
        if((endpoint & ~0b00011111) != 0){
            throw new IllegalArgumentException("Endpoint was out of range, range is \"32 < address <= 0\" and got " + endpoint);
        }
        Objects.requireNonNull(data, "data was null");
        data.order(ByteOrder.LITTLE_ENDIAN);
    }
    
    @Override
    public String getId(){
        return SETUP_DATA_EVENT;
    }
    
    @Override
    public String toString(){
        var builder = new StringBuilder("SetupDataEvent[address=")
            .append(address)
            .append(",endpoint=")
            .append(endpoint)
            .append(",data=[");
        BufferHelper.toString(builder, data());
        builder.append("]]");
        
        return builder.toString();
    }
}
