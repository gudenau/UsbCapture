package net.gudenau.usbcap.filter;

import java.nio.ByteBuffer;
import java.util.List;
import net.gudenau.usbcap.event.*;
import net.gudenau.usbcap.internal.BufferHelper;
import org.jetbrains.annotations.NotNull;

import static net.gudenau.usbcap.internal.BufferHelper.*;

/**
 * A filter that handles USB control transfer packets, groups them together and creates a single event.
 */
public final class ControlTransferFilter implements Filter{
    private State state;
    private ByteBuffer buffer = ByteBuffer.allocate(4096);
    private Request request;
    private Event pendingEvent;
    
    public ControlTransferFilter(){
        reset();
    }
    
    private void reset(){
        state = State.IDLE;
        request = null;
        pendingEvent = null;
    }
    
    @Override
    public boolean handleEvent(@NotNull Event event){
        if(event instanceof ResetEvent){
            reset();
            return false;
        }else if(event instanceof SetupDataEvent setup){
            handleSetupEvent(setup);
            return true;
        }else if(state != State.IDLE && event instanceof DataEvent data){
            handleDataEvent(data);
            return true;
        }
        
        return false;
    }
    
    private void handleSetupEvent(SetupDataEvent event){
        assert(state == State.IDLE) : new IllegalStateException("Got unexpected SETUP event");
        assert(pendingEvent == null) : new IllegalStateException("Pending event is still pending");
        
        var address = event.address();
        var endpoint = event.endpoint();
        var data = event.data();
        var bmRequestType = getUnsignedByte(data);
        var bRequest = getUnsignedByte(data);
        var wValue = getUnsignedShort(data);
        var wIndex = getUnsignedShort(data);
        var wLength = getUnsignedShort(data);
        
        request = new Request(
            address, endpoint,
            bmRequestType, bRequest, wValue, wIndex, wLength
        );
        state = (bmRequestType & 0b10000000) != 0 ? State.SETUP_IN : State.SETUP_OUT;
    }
    
    private void handleDataEvent(DataEvent event){
        assert(state != State.IDLE) : new IllegalStateException("Got unexpected DATA event");
        assert(pendingEvent == null) : new IllegalStateException("Pending event is still pending");
        
        if(
            (event.direction() == DataEvent.Direction.IN && state == State.SETUP_IN) ||
            (event.direction() == DataEvent.Direction.OUT && state == State.SETUP_OUT)
        ){
            var data = event.data();
            if(buffer.remaining() < data.remaining()){
                var newBuffer = ByteBuffer.allocate(buffer.capacity() << 1);
                buffer.flip();
                newBuffer.put(buffer);
                buffer = newBuffer;
            }
            buffer.put(data);
        }else{
            state = State.IDLE;
            buffer.flip();
            pendingEvent = new ControlTransferEvent(
                request.address(), request.endpoint(),
                request.bmRequestType(), request.bRequest(), request.wValue(), request.wIndex(), request.wLength(),
                BufferHelper.clone(buffer)
            );
            buffer.clear();
            request = null;
        }
    }
    
    @Override
    public @NotNull List<@NotNull Event> getPendingEvents(){
        if(pendingEvent == null){
            return List.of();
        }else{
            var event = pendingEvent;
            pendingEvent = null;
            return List.of(event);
        }
    }
    
    /**
     * The state of the internal state machine.
     *
     * @hidden
     */
    private enum State{
        /**
         * The state machine is waiting for a control transfer to start.
         */
        IDLE,
        /**
         * The state machine is reading the contents of an IN control transfer
         */
        SETUP_IN,
        /**
         * The state machine is reading the contents of an OUT control transfer
         */
        SETUP_OUT,
    }
    
    private record Request(
        int address, int endpoint,
        int bmRequestType, int bRequest, int wValue, int wIndex, int wLength
    ){}
}
