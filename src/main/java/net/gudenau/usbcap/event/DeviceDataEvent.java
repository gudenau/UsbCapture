package net.gudenau.usbcap.event;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import net.gudenau.usbcap.event.enumeration.Device;
import net.gudenau.usbcap.internal.BufferHelper;
import org.jetbrains.annotations.NotNull;

/**
 * A generic data transfer event for a device.
 *
 * @param device The device for the transfer
 * @param direction The direction of the transfer
 * @param endpoint The endpoint of the transfer
 * @param data The transfer payload
 */
public record DeviceDataEvent(
    @NotNull Device device,
    @NotNull DataEvent.Direction direction,
    int endpoint,
    @NotNull ByteBuffer data
) implements Event{
    public static final String DEVICE_DATA = "device_data";
    
    public DeviceDataEvent(Device device, DataEvent data){
        this(
            Objects.requireNonNull(device, "device was null"),
            Objects.requireNonNull(data, "data was null").direction(),
            data.endpoint(),
            data.data().duplicate()
        );
    }
    
    public DeviceDataEvent{
        Objects.requireNonNull(device, "device was null");
        Objects.requireNonNull(direction, "direction was null");
        if((endpoint & ~0b00011111) != 0){
            throw new IllegalArgumentException("Endpoint was out of range, range is \"32 < endpoint <= 0\" and got " + endpoint);
        }
        Objects.requireNonNull(data, "data was null");
        data.order(ByteOrder.LITTLE_ENDIAN);
    }
    
    @Override
    public String getId(){
        return DEVICE_DATA;
    }
    
    @Override
    public String toString(){
        var builder = new StringBuilder("deviceDataEvent[device=")
            .append(device())
            .append(",direction=").append(direction())
            .append(",endpoint=").append(endpoint())
            .append(",data=");
        BufferHelper.toString(builder, data());
        return builder.append(']').toString();
    }
}
