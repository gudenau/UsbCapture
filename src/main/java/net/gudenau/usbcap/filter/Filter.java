package net.gudenau.usbcap.filter;

import java.util.List;
import net.gudenau.usbcap.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * The base filter class. Responsible for processing events for the end consumer.
 */
public interface Filter{
    /**
     * Attempts to handle an event generated by a capture device or another filter.
     *
     * @param event The event to filter
     *
     * @return True to remove the event, false to allow further processing
     */
    boolean handleEvent(@NotNull Event event);
    
    /**
     * Return events generated by this filter.
     *
     * @return A list of generated events
     */
    @NotNull List<@NotNull Event> getPendingEvents();
}
