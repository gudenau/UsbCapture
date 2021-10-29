package net.gudenau.usbcap.driver.beagle;

import com.totalphase.beagle.BeagleExt;
import java.io.IOException;
import java.nio.*;
import java.util.List;
import net.gudenau.usbcap.UsbSpeed;
import net.gudenau.usbcap.capture.AbstractCaptureDevice;
import net.gudenau.usbcap.event.Event;
import net.gudenau.usbcap.event.PacketEvent;
import net.gudenau.usbcap.event.ResetEvent;
import org.jetbrains.annotations.NotNull;

import static com.totalphase.beagle.Beagle.*;

/**
 * A capture device for the Beagle 480, may work with other Beagle devices.
 */
public final class BeagleDevice extends AbstractCaptureDevice{
    private final short device;
    private final int id;
    private final boolean inUse;
    
    // Buffers for later, native code needs direct buffers.
    private final IntBuffer statusBuffer;
    private final IntBuffer eventsBuffer;
    private final LongBuffer time_sopBuffer;
    private final LongBuffer time_durationBuffer;
    private final IntBuffer time_dataoffsetBuffer;
    private final ByteBuffer dataBuffer;
    
    {
        var root = ByteBuffer.allocateDirect(Integer.BYTES * 3 + Long.BYTES * 2 + 1024);
        time_sopBuffer = root.slice(0, 8).order(ByteOrder.nativeOrder()).asLongBuffer();
        time_durationBuffer = root.slice(8, 8).order(ByteOrder.nativeOrder()).asLongBuffer();
        statusBuffer = root.slice(16, 4).order(ByteOrder.nativeOrder()).asIntBuffer();
        eventsBuffer = root.slice(20, 4).order(ByteOrder.nativeOrder()).asIntBuffer();
        time_dataoffsetBuffer = root.slice(24, 4).order(ByteOrder.nativeOrder()).asIntBuffer();
        dataBuffer = root.slice(28, 1024).order(ByteOrder.nativeOrder());
    }
    
    private volatile int handle;
    
    BeagleDevice(short device, int id, boolean inUse){
        this.device = device;
        this.id = id;
        this.inUse = inUse;
    }
    
    @Override
    public void doOpen() throws IOException{
        var features = bg_unique_id_to_features(id);
        if((features & BG_FEATURE_USB) == 0){
            throw new IllegalArgumentException("Beagle device does not support USB capture");
        }
        
        if(inUse){
            throw new IOException("Beagle device is in use by another program");
        }
    
        try(var ext = new BeagleExt()){
            var result = bg_open_ext(device, ext);
            if(result < 0){
                throw new IOException("Failed to open Beagle device: " + bg_status_string(result));
            }
            handle = result;
        }
    }
    
    @Override
    public void doBeginCapture(@NotNull UsbSpeed speed) throws IOException{
        var features = bg_features(handle);
        if((features & (switch(speed){
            case HIGH_SPEED -> BG_FEATURE_USB_HS;
            case SUPER_SPEED -> BG_FEATURE_USB_SS;
            default -> 0;
        })) == 0){
            throw new IOException("Beagle device does not support " + speed.name());
        }
        
        bg_timeout(handle, 500);
        bg_latency(handle, 200);
        
        // TODO Figure out settings for USB 3
        if(speed == UsbSpeed.HIGH_SPEED){
            bg_usb2_capture_config(handle, BG_USB2_CAPTURE_REALTIME);
            bg_usb2_target_config(handle, BG_USB2_AUTO_SPEED_DETECT);
            bg_usb_configure(handle, BG_USB_CAPTURE_USB2, BG_USB_TRIGGER_MODE_IMMEDIATE);
            bg_usb2_hw_filter_config(handle, BG_USB2_HW_FILTER_SELF);
        }else{
            throw new UnsupportedOperationException("USB 3 not currently supported.");
        }
        
        var result = bg_enable(handle, BG_PROTOCOL_USB);
        if(result < 0){
            throw new IOException("Failed to start capture: " + bg_status_string(result));
        }
    }
    
    @Override
    protected void doCaptureEvent(List<Event> eventBuffer) throws IOException{
        while(eventBuffer.isEmpty()){
            dataBuffer.clear();
        
            var result = bg_usb2_read(
                handle,
                statusBuffer,
                eventsBuffer,
                time_sopBuffer,
                time_durationBuffer,
                time_dataoffsetBuffer,
                dataBuffer
            );
            if(result < 0){
                throw new IOException("Failed to capture packets: " + bg_status_string(result));
            }
        
            var events = eventsBuffer.get(0);
        
            // TODO Figure out what events are *useful*
            if((events & BG_EVENT_USB_RESET) != 0){
                eventBuffer.add(new ResetEvent());
            }
        
            if(result > 0){
                eventBuffer.add(new PacketEvent(dataBuffer.slice(0, result)));
            }
        }
    }
    
    @Override
    protected void doClose() throws IOException{
        var result = bg_close(handle);
        if(result < 0){
            throw new IOException("Failed to close Beagle device: " + bg_status_string(result));
        }
        handle = 0;
    }
    
    @Override
    public String toString(){
        return "BeagleDevice[device=" + device +
               ",id=" + id +
               ",inUse=" + inUse +
               ']';
    }
}
