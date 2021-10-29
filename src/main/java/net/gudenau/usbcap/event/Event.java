package net.gudenau.usbcap.event;

import java.nio.ByteBuffer;

/**
 * An event that a capture device or filter can generate.
 */
public interface Event{
    /**
     * The id of this event. Please try and make custom IDs unique.
     *
     * @return The ID of the event
     */
    String getId();
    
    /**
     * Resets any state the event might have.
     */
    default void reset(){}
    
    /**
     * @hidden An interface for the network filter and network device.
     */
    interface Networked<T extends Event.Networked<T>> extends Event{
        ByteBuffer write();
    }
}
