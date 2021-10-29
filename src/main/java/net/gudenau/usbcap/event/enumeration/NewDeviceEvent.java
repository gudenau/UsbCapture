package net.gudenau.usbcap.event.enumeration;

import java.util.Objects;

/**
 * A new device was added to the USB.
 *
 * @param device The opaque handle for the new device
 */
public record NewDeviceEvent(
    Device device
) implements EnumerationEvent{
    public static final String NEW_DEVICE = "new_device";
    
    public NewDeviceEvent{
        Objects.requireNonNull(device, "device was null");
    }
    
    @Override
    public String getId(){
        return NEW_DEVICE;
    }
}
