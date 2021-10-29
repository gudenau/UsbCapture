module net.gudenau.UsbCapture.drivers.Beagle {
    requires net.gudenau.UsbCapture;
    requires com.totalphase.BeagleAPI;
    requires org.jetbrains.annotations;
    
    provides net.gudenau.usbcap.driver.CaptureDriver with net.gudenau.usbcap.driver.beagle.BeagleDriver;
}