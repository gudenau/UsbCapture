package net.gudenau.usbcap.event;

import java.nio.ByteBuffer;

/**
 * An event for a USB reset.
 */
public final class ResetEvent implements Event.Networked<ResetEvent>{
    public static final String RESET_EVENT = "reset_event";
    
    @Override
    public String getId(){
        return RESET_EVENT;
    }
    
    @Override
    public String toString(){
        return "ResetEvent";
    }
    
    @Override
    public ByteBuffer write(){
        return null;
    }
}
