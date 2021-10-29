package net.gudenau.usbcap.internal;

//TODO
public final class CRC{
    static{
        generateTables();
    }
    
    private static final byte[] CRC5 = new byte[256];
    private static final byte[] CRC16 = new byte[256];
    
    private static void generateTables(){
        generateTable(CRC5, 0x05);
        generateTable(CRC16, 0x8005);
    }
    
    private static void generateTable(byte[] crc, int dwPolynomial){
        for(int i = 0; i < 256; i++){
            int dwCrc = i;
            for(int j = 5; j > 0; j--){
                if((dwCrc & 1) != 0){
                    dwCrc = (dwCrc >> 1) ^ dwPolynomial;
                }else{
                    dwCrc >>= 1;
                }
            }
        
            crc[i] = (byte)dwCrc;
        }
    }
    
    public static int crc5(int crc, byte[] buffer){
        int remainder = ~crc;
        for(var datum : buffer){
            remainder = (remainder >> 8) ^ CRC5[(remainder ^ datum) & 0xFF];
        }
        return ~remainder;
    }
    
    public static int crc16(int crc, byte[] buffer){
        int remainder = ~crc;
        for(var datum : buffer){
            remainder = (remainder >> 8) ^ CRC16[(remainder ^ datum) & 0xFF];
        }
        return ~remainder;
    }
}
