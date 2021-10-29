package net.gudenau.usbcap.event;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * An event for a control transfer.
 *
 * @param address The address of the control transfer
 * @param endpoint The endpoint of the control transfer
 * @param bmRequestType The request type
 * @param bRequest The request
 * @param wValue The request value
 * @param wIndex The request index
 * @param wLength The maximum size of the request
 * @param data The request payload
 */
public record ControlTransferEvent(
    int address, int endpoint,
    int bmRequestType, int bRequest, int wValue, int wIndex, int wLength,
    ByteBuffer data
) implements Event{
    public static final String CONTROL_TRANSFER = "control_transfer";
    
    public ControlTransferEvent{
        Objects.requireNonNull(data, "data was null");
        data.order(ByteOrder.LITTLE_ENDIAN);
    }
    
    /**
     * Extracts the direction from the request type.
     *
     * @return The request direction
     */
    public int requestDirection(){
        return (bmRequestType() >>> 7) & 0b1;
    }
    
    /**
     * Extracts the type of the request from the request type.
     *
     * @return The request type
     */
    public int requestType(){
        return (bmRequestType() >>> 5) & 0b11;
    }
    
    /**
     * Extracts the recipient of the request from the request type.
     *
     * @return The request recipient
     */
    public int requestRecipient(){
        return bmRequestType() & 0b11111;
    }
    
    /**
     * Extracts the high half of the request value.
     *
     * @return The high half of the request value
     */
    public int wValueHigh(){
        return (wValue() >>> 8) & 0xFF;
    }
    
    /**
     * Extracts the low half of the request value.
     *
     * @return The low half of the request value
     */
    public int wValueLow(){
        return wValue() & 0xFF;
    }
    
    /**
     * Extracts the high half of the request index.
     *
     * @return The high half of the request index
     */
    public int wIndexHigh(){
        return (wIndex() >> 8) & 0xFF;
    }
    
    /**
     * Extracts the low half of the request index.
     *
     * @return The low half of the request index
     */
    public int wIndexLow(){
        return wIndex() & 0xFF;
    }
    
    @Override
    public void reset(){
        data().clear();
    }
    
    @Override
    public String getId(){
        return CONTROL_TRANSFER;
    }
}
