package net.gudenau.usbcap.event.enumeration;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A USB device handle. This is an opaque type used to help track devices, contains no real state.
 *
 * @param id An ID for this device
 */
public record Device(int id){
    private static final AtomicInteger ID = new AtomicInteger();
    
    /**
     * Creates a device with a unique ID for this session.
     */
    public Device(){
        this(ID.incrementAndGet());
    }
    
    @Override
    public String toString(){
        return "Device[" + id + ']';
    }
}
