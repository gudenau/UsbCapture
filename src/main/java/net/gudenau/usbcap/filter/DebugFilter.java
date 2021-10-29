package net.gudenau.usbcap.filter;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.gudenau.usbcap.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * A filter that turns packets into strings and sends them to a consumer or stdout.
 */
public final class DebugFilter implements Filter{
    private final Predicate<Event> filter;
    private final Consumer<String> eventConsumer;
    
    /**
     * Creates a new debug filter that sends all packets to stdout.
     */
    public DebugFilter(){
        this((ignored)->true);
    }
    
    /**
     * Creates a new debug filter that sends all packets to the provided consumer.
     *
     * @param eventConsumer The consumer
     */
    public DebugFilter(Consumer<String> eventConsumer){
        this((ignored)->true, eventConsumer);
    }
    
    /**
     * Creates a new debug filter that sends some packets to stdout.
     *
     * @param filter The packet filter
     */
    public DebugFilter(Predicate<Event> filter){
        this(filter, System.out::println);
    }
    
    /**
     * Creates a new debug filter that sends some packets to the provided consumer.
     *
     * @param filter The packet filter
     * @param eventConsumer The event consumer
     */
    public DebugFilter(Predicate<Event> filter, Consumer<String> eventConsumer){
        this.filter = Objects.requireNonNull(filter, "filter was null");
        this.eventConsumer = Objects.requireNonNull(eventConsumer, "messageConsumer was null");
    }
    
    @Override
    public boolean handleEvent(@NotNull Event event){
        if(filter.test(event)){
            eventConsumer.accept(event.getId() + "\t" + event);
        }
        return false;
    }
    
    @Override
    public @NotNull List<@NotNull Event> getPendingEvents(){
        return List.of();
    }
}
