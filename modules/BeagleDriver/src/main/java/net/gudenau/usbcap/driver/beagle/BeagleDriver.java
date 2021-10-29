package net.gudenau.usbcap.driver.beagle;

import com.totalphase.beagle.Beagle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Set;
import net.gudenau.usbcap.capture.CaptureDevice;
import net.gudenau.usbcap.driver.CaptureDriver;
import org.jetbrains.annotations.NotNull;

/**
 * Basic capture driver for the Beagle API.
 */
public final class BeagleDriver implements CaptureDriver{
    @Override
    public @NotNull String getDriverName(){
        return "beagle";
    }
    
    @Override
    public @NotNull Set<@NotNull CaptureDevice> getCaptureDevices(){
        // Figure out how many devices are attached to this machine.
        int deviceCount = Beagle.bg_find_devices_ext(null, null);
        if(deviceCount < 0){
            throw new RuntimeException("Failed to get beagle devices: " + Beagle.bg_status_string(deviceCount));
        }else if(deviceCount == 0){
            return Set.of();
        }
        
        // Allocate a single buffer large enough for all the device info and split it
        var buffer = ByteBuffer.allocateDirect(Short.BYTES + Integer.BYTES);
        var devicesBuffer = buffer.slice(4, 2).order(ByteOrder.nativeOrder()).asShortBuffer();
        var idsBuffer = buffer.slice(0, 4).order(ByteOrder.nativeOrder()).asIntBuffer();
        
        var result = Beagle.bg_find_devices_ext(devicesBuffer, idsBuffer);
        if(result < 0){
            throw new RuntimeException("Failed to get beagle devices: " + Beagle.bg_status_string((int)deviceCount));
        }else if(result == 0){
            return Set.of();
        }
        
        // Create a device instance for every found device.
        Set<CaptureDevice> devices = new HashSet<>();
        for(int i = 0; i < result; i++){
            var device = devicesBuffer.get(i);
            boolean inUse = (device & Beagle.BG_PORT_NOT_FREE) != 0;
            device &= ~Beagle.BG_PORT_NOT_FREE;
            devices.add(new BeagleDevice(device, idsBuffer.get(i), inUse));
        }
        return devices;
    }
}
