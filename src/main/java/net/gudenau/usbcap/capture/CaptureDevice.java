package net.gudenau.usbcap.capture;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import net.gudenau.usbcap.UsbSpeed;
import net.gudenau.usbcap.event.Event;
import net.gudenau.usbcap.filter.Filter;
import org.jetbrains.annotations.NotNull;

/**
 * A device that is capable of capturing USB packets.
 */
public interface CaptureDevice extends AutoCloseable{
    /**
     * Opens the device to allow further operations.
     *
     * @throws IOException If the device could not be opened
     */
    void open() throws IOException;
    
    /**
     * Adds a packet filter to the filter chain for this device.
     *
     * @param filter The filter to add
     */
    void addFilter(@NotNull Filter filter);
    
    /**
     * Removes a filter from the filter chain for this device.
     *
     * @param filter The filter to remove
     */
    void removeFilter(@NotNull Filter filter);
    
    /**
     * Adds multiple filters to the filter chain for this device.
     *
     * @param filters The collection of filters to add
     */
    default void addFilters(@NotNull List<@NotNull Filter> filters){
        Objects.requireNonNull(filters, "filters was null");
        filters.forEach(this::addFilter);
    }
    
    /**
     * Removes multiple filters from the filter chain for this device.
     *
     * @param filters The filters to remove
     */
    default void removeFilters(@NotNull List<@NotNull Filter> filters){
        Objects.requireNonNull(filters, "filters was null");
        filters.forEach(this::removeFilter);
    }
    
    /**
     * Adds multiple filters to the filter chain for this device.
     *
     * @param filters The collection of filters to add
     */
    default void addFilters(@NotNull Filter... filters){
        Objects.requireNonNull(filters, "filters was null");
        for(Filter filter : filters){
            addFilter(filter);
        }
    }
    
    /**
     * Removes multiple filters from the filter chain for this device.
     *
     * @param filters The filters to remove
     */
    default void removeFilters(@NotNull Filter... filters){
        Objects.requireNonNull(filters, "filters was null");
        for(Filter filter : filters){
            removeFilter(filter);
        }
    }
    
    /**
     * Begin capturing packets from the USB packet capture device.
     *
     * @param speed The USB speed to capture packets
     *
     * @throws IOException If the device could not start capturing packets
     */
    void beginCapture(@NotNull UsbSpeed speed) throws IOException;
    
    /**
     * Capture an event from the device and pass it though the registered filters.
     *
     * Filters can remove generated events as well as generate their own. If more than one event is generated only one
     * is returned, future calls to this method won't block until the buffered events are drained.
     *
     * @return The captured event.
     *
     * @throws IOException If there was an error capturing an event
     */
    Event captureEvent() throws IOException;
    
    @Override void close() throws IOException;
}
