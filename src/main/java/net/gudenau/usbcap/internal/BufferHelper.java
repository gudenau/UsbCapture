package net.gudenau.usbcap.internal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A bunch of {@link ByteBuffer} helpers.
 */
public final class BufferHelper{
    private BufferHelper(){}
    
    /**
     * Dumps the contents of a {@link ByteBuffer} into a {@link StringBuilder}
     *
     * @param builder The builder
     * @param buffer The buffer
     */
    public static void toString(StringBuilder builder, ByteBuffer buffer){
        int capacity = buffer.capacity();
        if(capacity > 0){
            for(int i = 0; i < capacity; i++){
                builder.append("%02X,".formatted(buffer.get(i)));
            }
            builder.setLength(builder.length() - 1);
        }
    }
    
    /**
     * Gets a byte from the buffer, or 0.
     *
     * @param buffer The buffer to read from
     *
     * @return The read byte or 0
     */
    public static byte getByte(ByteBuffer buffer){
        return buffer.hasRemaining() ? buffer.get() : 0;
    }
    
    /**
     * Check if a buffer has enough remaining bytes for a read, if not makes sure there is no remaining capacity.
     *
     * @param buffer The buffer to check
     * @param size The amount of bytes required
     *
     * @return True if the buffer has enough bytes remaining
     */
    private static boolean check(ByteBuffer buffer, int size){
        if(buffer.remaining() < size){
            buffer.position(buffer.limit());
            return false;
        }else{
            return true;
        }
    }
    
    /**
     * Gets a short from the buffer, or 0.
     *
     * @param buffer The buffer to read from
     *
     * @return The read short or 0
     */
    public static short getShort(ByteBuffer buffer){
        return check(buffer, Short.BYTES) ? buffer.getShort() : 0;
    }
    
    /**
     * Gets an unsigned byte from the buffer, or 0.
     *
     * @param buffer The buffer to read from
     *
     * @return The read unsigned byte or 0
     */
    public static int getUnsignedByte(ByteBuffer buffer){
        return Byte.toUnsignedInt(getByte(buffer));
    }
    
    /**
     * Gets an unsigned byte from the buffer, or 0.
     *
     * @param buffer The buffer to read from
     *
     * @return The read unsigned byte or 0
     */
    public static int getUnsignedShort(ByteBuffer buffer){
        return Short.toUnsignedInt(getShort(buffer));
    }
    
    /**
     * Creates a deep copy of a buffer.
     *
     * @param buffer The buffer to copy
     *
     * @return The new little endian buffer
     */
    public static ByteBuffer clone(ByteBuffer buffer){
        var newBuffer = ByteBuffer.allocate(buffer.remaining());
        newBuffer.put(buffer);
        return newBuffer.order(ByteOrder.LITTLE_ENDIAN).clear();
    }
    
    /**
     * Reads a short BCD value and converts it to an int.
     *
     * @param buffer The buffer to read
     *
     * @return The converted number
     */
    public static int getBcdShort(ByteBuffer buffer){
        var readValue = getUnsignedShort(buffer);
        
        int value = 0;
        for(int i = 0; readValue != 0; i++){
            int digit = readValue & 0xF;
            value += digit * i * 10;
            readValue >>= 4;
        }
        return value;
    }
}
