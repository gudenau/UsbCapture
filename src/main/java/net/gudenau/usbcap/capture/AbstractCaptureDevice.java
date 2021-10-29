package net.gudenau.usbcap.capture;

import java.io.IOException;
import java.util.*;
import net.gudenau.usbcap.UsbSpeed;
import net.gudenau.usbcap.event.Event;
import net.gudenau.usbcap.filter.Filter;
import org.jetbrains.annotations.NotNull;

/**
 * A simple abstract capture device to ease driver development.
 *
 * Warning, if you need thread safety this is not for you.
 */
public abstract class AbstractCaptureDevice implements CaptureDevice{
    private final List<Filter> filters = new ArrayList<>();
    private volatile boolean opened = false;
    
    @Override
    public final void open() throws IOException{
        doOpen();
        opened = true;
    }
    
    /**
     * Open the underlying device.
     *
     * @throws IOException If the device could not be opened
     */
    protected abstract void doOpen() throws IOException;
    
    @Override
    public final void addFilter(@NotNull Filter filter){
        filters.add(filter);
    }
    
    @Override
    public final void removeFilter(@NotNull Filter filter){
        filters.remove(filter);
    }
    
    /**
     * A central place to check if the device is opened.
     *
     * @throws IOException If the device is not open
     */
    private void checkOpen() throws IOException{
        if(!opened){
            throw new IOException("USB capture device was not open");
        }
    }
    
    @Override
    public final void beginCapture(@NotNull UsbSpeed speed) throws IOException{
        Objects.requireNonNull(speed, "speed was null");
        checkOpen();
        doBeginCapture(speed);
    }
    
    /**
     * Tells the device to start capturing packets.
     *
     * @param speed The USB speed to capture
     *
     * @throws IOException If the capture could not be started
     */
    protected abstract void doBeginCapture(UsbSpeed speed) throws IOException;
    
    /**
     * All of the pending events we have.
     */
    private final Queue<Event> eventBuffer = new LinkedList<>();
    
    @Override
    public final Event captureEvent() throws IOException{
        checkOpen();
    
        // If we already have events, just grab the oldest one.
        if(!eventBuffer.isEmpty()){
            return eventBuffer.remove();
        }
    
        // We want to return an event, if one isn't generated we need to keep checking...
        // TODO Make some sort of timeout or cancel method
        while(eventBuffer.isEmpty()){
            List<Event> pendingEvents = new LinkedList<>();
            while(pendingEvents.isEmpty()){
                // Get events from the device implementation
                doCaptureEvent(pendingEvents);
            }
        
            // The jank filter code, there is likely a much better way to handle this
            
            int index = 0;
            outer:
            while(index < pendingEvents.size()){
                // Get the oldest pending event
                var event = pendingEvents.get(index);
                // Just in case
                assert (event != null) : new IllegalStateException("null event");
                
                // Pass the event though the filters.
                for(var filter : filters){
                    // Tell the event to reset, cleans up reads and writes to Buffers.
                    event.reset();
                    // Pass the event to a filter
                    var shouldRemove = filter.handleEvent(event);
                    // Insert all of the events the filter generated after the current event.
                    pendingEvents.addAll(index + 1, filter.getPendingEvents());
                    
                    // If the filter wants the event to be removed we don't have to keep passing it to other filters.
                    if(shouldRemove){
                        index++;
                        continue outer;
                    }
                }
                
                // The event made it all the way through the filter chain, pass it along to the consumer.
                index++;
                eventBuffer.add(event);
            }
        }
    
        return eventBuffer.remove();
    }
    
    /**
     * Capture events from the capture device.
     *
     * @param eventBuffer The list to add the events to
     *
     * @throws IOException If there was an error capturing an event
     */
    protected abstract void doCaptureEvent(List<Event> eventBuffer) throws IOException;
    
    @Override
    public final void close() throws IOException{
        // Do nothing if not open
        if(!opened){
            return;
        }
    
        doClose();
    }
    
    /**
     * Close the underlying device.
     *
     * Never called when the device is already closed.
     *
     * @throws IOException If the device could not be closed cleanly
     */
    protected abstract void doClose() throws IOException;
}
