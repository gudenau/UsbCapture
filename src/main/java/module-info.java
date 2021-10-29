module net.gudenau.UsbCapture {
    exports net.gudenau.usbcap;
    exports net.gudenau.usbcap.capture;
    exports net.gudenau.usbcap.driver;
    exports net.gudenau.usbcap.event;
    exports net.gudenau.usbcap.event.enumeration;
    exports net.gudenau.usbcap.filter;
    
    uses net.gudenau.usbcap.driver.CaptureDriver;
    
    requires org.jetbrains.annotations;
}