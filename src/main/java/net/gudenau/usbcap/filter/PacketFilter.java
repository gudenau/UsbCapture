package net.gudenau.usbcap.filter;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.*;
import net.gudenau.usbcap.event.*;
import org.jetbrains.annotations.NotNull;

/**
 * Takes raw USB packet events and condenses them into transfers.
 */
public final class PacketFilter implements Filter{
    private static final byte BG_USB_PID_OUT = (byte)0xE1;
    private static final byte BG_USB_PID_IN = (byte)0x69;
    private static final byte BG_USB_PID_SOF = (byte)0xA5;
    private static final byte BG_USB_PID_SETUP = (byte)0x2D;
    private static final byte BG_USB_PID_DATA0 = (byte)0xC3;
    private static final byte BG_USB_PID_DATA1 = (byte)0x4B;
    private static final byte BG_USB_PID_DATA2 = (byte)0x87;
    private static final byte BG_USB_PID_MDATA = (byte)0x0F;
    private static final byte BG_USB_PID_ACK = (byte)0xD2;
    private static final byte BG_USB_PID_NAK = (byte)0x5A;
    private static final byte BG_USB_PID_STALL = (byte)0x1E;
    private static final byte BG_USB_PID_NYET = (byte)0x96;
    private static final byte BG_USB_PID_PRE = (byte)0x3C;
    private static final byte BG_USB_PID_ERR = (byte)0x3C;
    private static final byte BG_USB_PID_SPLIT = (byte)0x78;
    private static final byte BG_USB_PID_PING = (byte)0xB4;
    private static final byte BG_USB_PID_EXT = (byte)0xF0;
    private static final byte BG_USB_PID_CORRUPTED = (byte)0xFF;
    
    private final Queue<Event> pendingEvents = new LinkedList<>();
    
    private State state;
    private int expectedData;
    private int frameNumber;
    private boolean isSetup;
    private int address;
    private int endpoint;
    private Event pendingEvent;
    
    public PacketFilter(){
        resetState();
    }
    
    private void resetState(){
        state = State.IDLE;
        expectedData = BG_USB_PID_DATA0;
        frameNumber = 0;
        address = 0;
        endpoint = 0;
        pendingEvent = null;
    }
    
    @Override
    public boolean handleEvent(@NotNull Event rawEvent){
        if(!(rawEvent instanceof PacketEvent event)){
            if(rawEvent instanceof ResetEvent){
                resetState();
            }
            return false;
        }
        
        var payload = event.buffer();
        var pid = payload.get();
        
        // This is all a bit of a mess...
        while(true){
            switch(state){
                case IDLE -> {
                    //TODO Validate this.
                    try{
                        switch(pid){
                            // SOF packets are for USB frames. Should we do more with them?
                            // +-----+-------+-----+
                            // | PID | frame | CRC |
                            // +-----+-------+-----+
                            // |  8  |   11  |  5  |
                            // +-----+-------+-----+
                            case BG_USB_PID_SOF -> {
                                var data = payload.getShort();
                                var crc = (data >>> 11) & 0b11111;
                                if(!verifyCrc5(payload, crc)){
                                    break;
                                }
                                frameNumber = data & 0b0000011111111111;
                                // Doesn't change the state
                            }
        
                            // SETUP/IN packets start a data transfer from the device to the host.
                            // TODO Spit this into to cases once we know the code is good
                            // +-----+---------+----------+-----+
                            // | PID | address | endpoint | CRC |
                            // +-----+---------+----------+-----+
                            // |  8  |    7    |     4    |  5  |
                            // +-----+---------+----------+-----+
                            case BG_USB_PID_IN, BG_USB_PID_SETUP -> {
                                boolean isSetup = pid == BG_USB_PID_SETUP;
                                // SETUP packets are always followed by a DATA0
                                if(isSetup){
                                    expectedData = BG_USB_PID_DATA0;
                                }
            
                                var data = payload.getShort();
            
                                int address = data & 0b00000000_01111111;
                                int endpoint = (data >>> 7) & 0b00000000_00001111;
                                int crc = (data >>> 11) & 0b00000000_00011111;
                                if(!verifyCrc5(payload, crc)){
                                    break;
                                }
            
                                this.isSetup = isSetup;
                                this.address = address;
                                this.endpoint = endpoint;
                                state = State.IN;
                            }
        
                            // OUT packets start a data transfer from the host to the device.
                            // +-----+---------+----------+-----+
                            // | PID | address | endpoint | CRC |
                            // +-----+---------+----------+-----+
                            // |  8  |    7    |     4    |  5  |
                            // +-----+---------+----------+-----+
                            case BG_USB_PID_OUT -> {
                                var data = payload.getShort();
            
                                int address = data & 0b00000000_01111111;
                                int endpoint = (data >>> 7) & 0b00000000_00001111;
                                int crc = (data >>> 11) & 0b00000000_00011111;
                                if(!verifyCrc5(payload, crc)){
                                    break;
                                }
            
                                this.address = address;
                                this.endpoint = endpoint;
                                state = State.OUT;
                            }
                        }
                    }catch(BufferUnderflowException e){
                        //TODO validate this
                        state = State.OUT;
                    }
                }
    
                // Handles the DATA packet that follows an IN packet
                // +-----+------+-----+
                // | PID | data | CRC |
                // +-----+------+-----+
                // |  8  |  n   |  5  |
                // +-----+------+-----+
                case IN -> {
                    if(pid != expectedData){
                        //TODO Make this some sort of event or drop it
                        if(pid != BG_USB_PID_DATA0 && pid != BG_USB_PID_DATA1){
                            address = 0;
                            endpoint = 0;
                            state = State.IDLE;
                            continue;
                        }
                    }
                    expectedData = expectedData == BG_USB_PID_DATA0 ? BG_USB_PID_DATA1 : BG_USB_PID_DATA0;
        
                    int crc = payload.getShort(payload.capacity() - 2);
                    if(!verifyCrc16(payload, crc)){
                        address = 0;
                        endpoint = 0;
                        state = State.IDLE;
                        break;
                    }
        
                    var data = payload.slice(1, payload.capacity() - 3);
                    pendingEvent = isSetup ?
                        new SetupDataEvent(address, endpoint, data) :
                        new DataEvent(DataEvent.Direction.IN, address, endpoint, data);
        
                    address = 0;
                    endpoint = 0;
                    state = State.ACK;
                }
                
                // Handles the DATA packet that follows an OUT packet
                // +-----+------+-----+
                // | PID | data | CRC |
                // +-----+------+-----+
                // |  8  |  n   |  5  |
                // +-----+------+-----+
                case OUT -> {
                    if(pid != expectedData){
                        //TODO Make this some sort of event or drop it
                        if(pid != BG_USB_PID_DATA0 && pid != BG_USB_PID_DATA1){
                            address = 0;
                            endpoint = 0;
                            state = State.IDLE;
                            continue;
                        }
                    }
                    expectedData = expectedData == BG_USB_PID_DATA0 ? BG_USB_PID_DATA1 : BG_USB_PID_DATA0;
        
                    int crc = payload.getShort(payload.capacity() - 2);
                    if(!verifyCrc16(payload, crc)){
                        address = 0;
                        endpoint = 0;
                        state = State.IDLE;
                        break;
                    }
        
                    pendingEvent = new DataEvent(DataEvent.Direction.OUT, address, endpoint, payload.slice(1, payload.capacity() - 3));
        
                    address = 0;
                    endpoint = 0;
                    state = State.ACK;
                }
    
                // Handles the ACK packet that follows a DATA packet
                // +-----+
                // | PID |
                // +-----+
                // |  8  |
                // +-----+
                case ACK -> {
                    if(pid != BG_USB_PID_ACK){
                        pendingEvent = null;
                        state = State.IDLE;
                        continue;
                    }
                    
                    assert (pendingEvent != null) : new IllegalStateException("ACK state without a pending event");
            
                    pendingEvents.add(pendingEvent);
            
                    state = State.IDLE;
                    pendingEvent = null;
                }
            }
            break;
        }
        
        return true;
    }
    
    private boolean verifyCrc5(ByteBuffer data, int crc){
        //TODO
        return true;
    }
    
    private boolean verifyCrc16(ByteBuffer data, int crc){
        //TODO
        return true;
    }
    
    @Override
    public @NotNull List<@NotNull Event> getPendingEvents(){
        if(pendingEvents.isEmpty()){
            return List.of();
        }else{
            var list = new ArrayList<>(pendingEvents);
            pendingEvents.clear();
            return list;
        }
    }
    
    /**
     * The state of the internal state machine.
     * @hidden
     */
    private enum State{
        /**
         * Waiting for a transfer to start.
         */
        IDLE,
        /**
         * An IN transfer has started, waiting for the payload.
         */
        IN,
        /**
         * An OUT transfer has started, waiting for the payload.
         */
        OUT,
        /**
         * The payload has been transferred, waiting for the ACK.
         */
        ACK,
    }
}
