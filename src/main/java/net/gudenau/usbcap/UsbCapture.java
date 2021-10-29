package net.gudenau.usbcap;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.gudenau.usbcap.capture.CaptureDevice;
import net.gudenau.usbcap.driver.CaptureDriver;
import org.jetbrains.annotations.NotNull;

/**
 * The primary starting point for this API, allows you to get a list of known USB capture devices.
 */
public final class UsbCapture{
    private static final Map<String, CaptureDriver> DRIVERS;
    
    static{
        // Do this once
        DRIVERS = ServiceLoader.load(CaptureDriver.class).stream()
            .map(ServiceLoader.Provider::get)
            .collect(Collectors.toUnmodifiableMap(CaptureDriver::getDriverName, Function.identity()));
    }
    
    /**
     * Gets all of the known USB capture devices connected to host.
     *
     * @return A set of capture devices
     */
    public static @NotNull Set<@NotNull CaptureDevice> getCaptureDevices(){
        return DRIVERS.values().parallelStream()
            .map(CaptureDriver::getCaptureDevices)
            .flatMap(Set::stream)
            .collect(Collectors.toUnmodifiableSet());
    }
}
