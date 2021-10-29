# UsbCapture

A library to help capture and decode USB packets.

The library provides a powerful and simple to use filter system that allows for the removal and injection of events into
the capture. There are a few filters that come by default, they include filters for converting the raw capture into
raw transfers and converting raw transfers into transfers with a specific device attached.

---

### Examples

Raw packet capture:
```Java
// Get a set of all known capture devices
var devices = UsbCapture.getCaptureDevices();
// Find a capture device, or throw if there are none found.
// This is done in a try-with-resources block to ensure it is always closed
try(
    var device = devices.stream().findAny()
        .orElseThrow(()->new RuntimeException("Failed to find USB capture device"));
){
    // Open the device
    device.open();
    // Start capturing high speed USB packets
    device.beginCapture(UsbSpeed.HIGH_SPEED);
    
    // Capture 64 packets and print them to stdout.
    for(int i = 0; i < 64; i++){
        System.out.printf("%d %s\n",device.captureEvent());
    }
    
    // The try-with-resources block automaticly calls close.
}
```

Transfer-level packet capture:
```Java
var devices = UsbCapture.getCaptureDevices();
try(
    var device = devices.stream().findAny()
        .orElseThrow(()->new RuntimeException("Failed to find USB capture device"));
){
    device.open();

    // This filter takes the raw PacketEvents and condenses them into DataEvents and
    // SetupDataEvents. 
    device.addFilter(new PacketFilter());
    
    device.beginCapture(UsbSpeed.HIGH_SPEED);
    
    for(int i = 0; i < 64; i++){
        // We expect ResetEvents, SetupDataEvents and DataEvents
        var event = device.captureEvent();
        switch(event.getId()){
            case ResetEvent.RESET_EVENT -> System.out.printf("%d USB reset\n", i);
            case SetupDataEvent.SETUP_DATA_EVENT -> System.out.printf("%d Setup transfer: %s\n", i, event);
            case DataEvent.DATA_EVENT -> System.out.printf("%d Data transfer:  %s\n", i, event);
            default -> System.out.printf("%d Unknown event: %s\n");
        }
    }
}
```


Device-level transfer capture:
```Java
var devices = UsbCapture.getCaptureDevices();
try(
    var device = devices.stream().findAny()
        .orElseThrow(()->new RuntimeException("Failed to find USB capture device"));
){
    device.open();
    
    // This chain of filters allow for a device-level view of captured USB packets.
    device.addFilter(new PacketFilter());
    device.addFilter(new ControlTransferFilter());
    device.addFilter(new DeviceFilter());
    
    device.beginCapture(UsbSpeed.HIGH_SPEED);
    
    for(int i = 0; i < 64; i++){
        // We expect ResetEvents, SetupDataEvents and DataEvents
        var event = device.captureEvent();
        switch(event.getId()){
            case ResetEvent.RESET_EVENT -> System.out.printf("%d USB reset\n", i);
            case DeviceDataEvent.DEVICE_DATA_EVENT -> System.out.printf("%d Device data: %s\n", i, event);
            case NewDeviceEvent.NEW_DEVICE_EVENT -> System.out.printf("%d New device:  %s\n", i, event);
            case DeviceDescriptorEvent.DEVICE_DESCRIPTOR_EVENT -> System.out.printf("%d Device descriptor:  %s\n", i, event);
            default -> System.out.printf("%d Unknown event: %s\n");
        }
    }
}
```
---

### Saving captures for later

This library has a very basic method of saving captured packets to disk to process later.

Example packet writer:
```Java
// This is mostly the same as the normal way to capture packets.
var captureDevices = UsbCapture.getCaptureDevices();
try(
    var device = captureDevices.stream().findAny()
        .orElseThrow(()->new RuntimeException("Failed to find USB capture device"));
    var filter = new CaptureFilter(Path.of(".", ""capture.bin"))
){
    device.open();
    device.addFilter(filter);
    device.beginCapture(UsbSpeed.HIGH_SPEED);
    
    for(int i = 0; i < 64; i++){
        device.captureEvent();
    }
}
```

Example packet reader:
```Java
try(var device = new FileCaptureDevice(Path.of(".", "capture.bin"))){
    // Everything else works like normal!
    device.open();
    device.beginCapture(UsbSpeed.HIGH_SPEED);
    
    for(int i = 0; i < 64; i++){
        System.out.printf("%d: %s\n", i, device.captureEvent());
    }
}
```

---

### Networking

This library has a very basic form of networking to allow for the capture of packets on one machine and processing of
them on a different machine. 

Example server:
```Java
// Create a new server on port 8765, it will only accept a single connection.
// Once the connection is established the port will be freed.
try(var device = new NetworkCaptureDevice()){
    // Everything else works like normal!
    device.open();
    device.beginCapture(UsbSpeed.HIGH_SPEED);
    
    for(int i = 0; i < 64; i++){
        System.out.printf("%d: %s\n", i, device.captureEvent());
    }
}
```

Example client:
```Java
// This is mostly the same as the normal way to capture packets.
var captureDevices = UsbCapture.getCaptureDevices();
try(
    var device = captureDevices.stream().findAny()
        .orElseThrow(()->new RuntimeException("Failed to find USB capture device"))
){
    device.open();
    
    // Connect to a server at 127.0.0.1:8765 and submit packets there.
    // You should not have any filters that create events that are not PacketEvent or ResetEvent before this one.
    device.addFilter(new NetworkFilter("127.0.0.1"));
    
    device.beginCapture(UsbSpeed.HIGH_SPEED);
    
    while(true){
        device.captureEvent();
    }
}
```

---

### Implementing a driver

Implementing a driver is fairly straightforward. You create a class that implements `CaptureDriver` and declared it in
your `module-info`.

```Java
module com.example.ExampleDriver{
    requires net.gudenau.UsbCapture;
    
    provides net.gudenau.usbcap.driver.CaptureDriver with com.example.driver.ExampleDriver;
}
```

```Java
public final class ExampleDriver implements CaptureDriver{
    @Override
    public @NotNull String getDriverName(){
        return "example";
    }
    
    @Override
    public @NotNull Set<@NotNull CaptureDevice> getCaptureDevices(){
        return Set.of();
    }
}
```
