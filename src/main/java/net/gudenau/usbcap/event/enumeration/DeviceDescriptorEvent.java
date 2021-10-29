package net.gudenau.usbcap.event.enumeration;

import net.gudenau.usbcap.event.Event;

/**
 * An event for a USB device descriptor.
 *
 * @param device The USB device handle
 * @param usbVersion The reported USB version
 * @param deviceClass The USB class of the device
 * @param deviceSubClass The USB subclass of the device
 * @param deviceProtocol The USB protocol of the device
 * @param maxPacketSize The max packet size for the device
 * @param vendor The vendor ID of the device
 * @param product The product ID of the device
 * @param deviceVersion The reported version of the device
 * @param manufacturerIndex The index of the manufacture string record
 * @param serialNumberIndex The index of the serial number string record
 * @param configurationCount The number of configurations supported by the device
 */
public record DeviceDescriptorEvent(
    Device device,
    int usbVersion,
    int deviceClass, int deviceSubClass, int deviceProtocol,
    int maxPacketSize,
    int vendor, int product, int deviceVersion,
    int manufacturerIndex, int serialNumberIndex,
    int configurationCount
) implements Event{
    public static final String DEVICE_DESCRIPTOR = "device_descriptor";
    
    @Override
    public String getId(){
        return DEVICE_DESCRIPTOR;
    }
}
