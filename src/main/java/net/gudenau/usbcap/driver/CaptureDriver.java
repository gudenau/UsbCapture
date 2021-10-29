package net.gudenau.usbcap.driver;

import java.util.Set;
import net.gudenau.usbcap.capture.CaptureDevice;
import org.jetbrains.annotations.NotNull;

/**
 * The interface that provides hardware devices to use to capture USB packets.
 */
public interface CaptureDriver{
    /**
     * Returns the name of this driver.
     *
     * @return The name of the driver
     */
    @NotNull String getDriverName();
    
    /**
     * Returns a set of all connected capture devices that this driver can use.
     *
     * @return All connected capture devices
     */
    @NotNull Set<@NotNull CaptureDevice> getCaptureDevices();
}
