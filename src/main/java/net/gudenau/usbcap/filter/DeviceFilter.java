package net.gudenau.usbcap.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.gudenau.usbcap.event.*;
import net.gudenau.usbcap.event.enumeration.Device;
import net.gudenau.usbcap.event.enumeration.DeviceDescriptorEvent;
import net.gudenau.usbcap.event.enumeration.NewDeviceEvent;
import org.jetbrains.annotations.NotNull;

import static net.gudenau.usbcap.internal.BufferHelper.getBcdShort;
import static net.gudenau.usbcap.internal.BufferHelper.getUnsignedByte;
import static net.gudenau.usbcap.internal.BufferHelper.getUnsignedShort;

/**
 * A filter that takes transfers and attempts to convert them into request events and transfer events tagged with an
 * instance of {@link Device}
 */
//TODO Most of the requests
public final class DeviceFilter implements Filter{
    private static final int REQUEST_GET_STATUS = 0;
    private static final int REQUEST_CLEAR_FEATURE = 1;
    private static final int REQUEST_SET_FEATURE = 3;
    private static final int REQUEST_SET_ADDRESS = 5;
    private static final int REQUEST_GET_DESCRIPTOR = 6;
    private static final int REQUEST_SET_DESCRIPTOR = 7;
    private static final int REQUEST_GET_CONFIGURATION = 8;
    private static final int REQUEST_SET_CONFIGURATION = 9;
    private static final int REQUEST_GET_INTERFACE = 10;
    private static final int REQUEST_SET_INTERFACE = 11;
    private static final int REQUEST_SYNCH_FRAME = 12;
    
    private static final int DESCRIPTOR_DEVICE = 1;
    private static final int DESCRIPTOR_CONFIGURATION = 2;
    private static final int DESCRIPTOR_STRING = 3;
    private static final int DESCRIPTOR_INTERFACE = 4;
    private static final int DESCRIPTOR_ENDPOINT = 5;
    private static final int DESCRIPTOR_DEVICE_QUALIFIER = 6;
    private static final int DESCRIPTOR_OTHER_SPEED_CONFIGURATION = 7;
    private static final int DESCRIPTOR_INTERFACE_POWER = 8;
    
    private final Device[] devices = new Device[128];
    private final List<Event> pendingEvents = new ArrayList<>();
    
    public DeviceFilter(){
        reset();
    }
    
    private void reset(){
        if(Arrays.mismatch(devices, new Device[devices.length]) != -1){
            System.out.flush();
        }
        Arrays.fill(devices, null);
    }
    
    @Override
    public boolean handleEvent(@NotNull Event event){
        if(event instanceof ResetEvent){
            reset();
            return false;
        }else if(event instanceof DataEvent data){
            return handleDataEvent(data);
        }else if(event instanceof ControlTransferEvent control){
            return handleControlTransfer(control);
        }
        
        return false;
    }
    
    private boolean handleDataEvent(DataEvent data){
        var device = getDevice(data.address(), false);
        if(device != null){
            pendingEvents.add(new DeviceDataEvent(device, data));
            return true;
        }
        
        return false;
    }
    
    private boolean handleControlTransfer(ControlTransferEvent control){
        //Util.log("Control transfer: %d (%s)\n", control.address());
        var device = getDevice(control.address());
        
        return switch(control.bRequest()){
            case REQUEST_GET_STATUS -> {
                //Util.log("REQUEST_GET_STATUS\n");
                yield false;
            }
            case REQUEST_CLEAR_FEATURE -> {
                //Util.log("REQUEST_CLEAR_FEATURE\n");
                yield false;
            }
            case REQUEST_SET_FEATURE -> {
                //Util.log("REQUEST_SET_FEATURE\n");
                yield false;
            }
            case REQUEST_SET_ADDRESS -> {
                var newAddress = control.wValue();
                assert(devices[newAddress] == null) : new RuntimeException("Device already existed at address " + newAddress);
                devices[newAddress] = device;
                devices[control.address()] = null;
                
                //Util.log("REQUEST_SET_ADDRESS\n");
                yield true;
            }
            case REQUEST_GET_DESCRIPTOR -> {
                if(control.bmRequestType() != 0b10000000){
                    yield false;
                }
                
                var type = control.wValueHigh();
                var index = control.wValueLow();
                var language = control.wIndex();
                var length = control.wLength();
                
                var data = control.data();
                data.limit(Math.min(data.capacity(), length));
    
                //Util.log("REQUEST_GET_DESCRIPTOR\n");
                
                yield switch(type){
                    case DESCRIPTOR_DEVICE ->{
                        //Util.log("\tDESCRIPTOR_DEVICE\n");
                        
                        data.limit(Math.min(data.limit(), getUnsignedByte(data)));
                        var descriptorType = getUnsignedByte(data);
                        assert(descriptorType == DESCRIPTOR_DEVICE) : new IllegalStateException("Got non-device descriptor");
                        var usbVersion = getBcdShort(data);
                        var deviceClass = getUnsignedByte(data);
                        var deviceSubClass = getUnsignedByte(data);
                        var deviceProtocol = getUnsignedByte(data);
                        var maxPacketSize = getUnsignedByte(data);
                        var vendor = getUnsignedShort(data);
                        var product = getUnsignedShort(data);
                        var deviceVersion = getBcdShort(data);
                        var manufacturerIndex = getUnsignedByte(data);
                        var serialNumberIndex = getUnsignedByte(data);
                        var configurationCount = getUnsignedByte(data);
                        
                        pendingEvents.add(new DeviceDescriptorEvent(
                            device,
                            usbVersion,
                            deviceClass, deviceSubClass, deviceProtocol,
                            maxPacketSize,
                            vendor, product, deviceVersion,
                            manufacturerIndex, serialNumberIndex, configurationCount
                        ));
                        
                        yield true;
                    }
                    case DESCRIPTOR_CONFIGURATION ->{
                        //Util.log("\tDESCRIPTOR_CONFIGURATION\n");
                        yield true;
                    }
                    case DESCRIPTOR_STRING ->{
                        //Util.log("\tDESCRIPTOR_STRING\n");
                        yield true;
                    }
                    case DESCRIPTOR_INTERFACE ->{
                        //Util.log("\tDESCRIPTOR_INTERFACE\n");
                        yield true;
                    }
                    case DESCRIPTOR_ENDPOINT ->{
                        //Util.log("\tDESCRIPTOR_ENDPOINT\n");
                        yield true;
                    }
                    case DESCRIPTOR_DEVICE_QUALIFIER ->{
                        //Util.log("\tDESCRIPTOR_DEVICE_QUALIFIER\n");
                        yield true;
                    }
                    case DESCRIPTOR_OTHER_SPEED_CONFIGURATION ->{
                        //Util.log("\tDESCRIPTOR_OTHER_SPEED_CONFIGURATION\n");
                        yield true;
                    }
                    case DESCRIPTOR_INTERFACE_POWER ->{
                        //Util.log("\tDESCRIPTOR_INTERFACE_POWER\n");
                        yield true;
                    }
                    default -> false;
                };
            }
            case REQUEST_SET_DESCRIPTOR -> {
                //Util.log("REQUEST_SET_DESCRIPTOR\n");
                yield false;
            }
            case REQUEST_GET_CONFIGURATION -> {
                //Util.log("REQUEST_GET_CONFIGURATION\n");
                yield false;
            }
            case REQUEST_SET_CONFIGURATION -> {
                //Util.log("REQUEST_SET_CONFIGURATION\n");
                yield false;
            }
            case REQUEST_GET_INTERFACE -> {
                //Util.log("REQUEST_GET_INTERFACE\n");
                yield false;
            }
            case REQUEST_SET_INTERFACE -> {
                //Util.log("REQUEST_SET_INTERFACE\n");
                yield false;
            }
            case REQUEST_SYNCH_FRAME -> {
                //Util.log("REQUEST_SYNCH_FRAME\n");
                yield false;
            }
            default -> false;
        };
    }
    
    private Device getDevice(int address){
        return getDevice(address, true);
    }
    
    private Device getDevice(int address, boolean create){
        var device = devices[address];
        if(device == null && create){
            device = new Device();
            devices[address] = device;
            pendingEvents.add(new NewDeviceEvent(device));
        }
        return device;
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
}
