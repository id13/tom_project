package messages.util;

import java.util.zip.CRC32;

public class ByteUtil {

  /** Read a signed 32bit value */
  static public int readInt32(byte bytes[], int offset) {
    int val;
    val = ((bytes[offset] & 0xFF) << 24);
    val |= ((bytes[offset+1] & 0xFF) << 16);
    val |= ((bytes[offset+2] & 0xFF) << 8);
    val |= (bytes[offset+3] & 0xFF);
    return val;
  }


  /** Write a signed 32bit value */
  static public void writeInt32(byte[] bytes, int offset, int value) {
    bytes[offset]= (byte)((value >> 24) & 0xff);
    bytes[offset+1]= (byte)((value >> 16) & 0xff);
    bytes[offset+2]= (byte)((value >> 8) & 0xff);
    bytes[offset+3]= (byte)(value & 0xff);
  }

  static public long computeCRC32(byte[] bytes) {
    CRC32 crc = new CRC32();
    crc.update(bytes);
    return crc.getValue();
  }
  
  /** Write a signed 64bit value */  
  static public void writeLong64(byte[] bytes, int offset, long value) {
    bytes[offset]= (byte)((value >> 56) & 0xffL);
    bytes[offset+1]= (byte)((value >> 48) & 0xffL);
    bytes[offset+2]= (byte)((value >> 40) & 0xffL);
    bytes[offset+3]= (byte)((value >> 32) & 0xffL);
    bytes[offset+4]= (byte)((value >> 24) & 0xffL);
    bytes[offset+5]= (byte)((value >> 16) & 0xffL);
    bytes[offset+6]= (byte)((value >> 8) & 0xffL);
    bytes[offset+7]= (byte)(value & 0xffL);    
  }

  /** Read a signed 64bit value */
  static public long readLong64(byte bytes[], int offset) {
    long val;
    val = (((long)(bytes[offset]) & 0xffL) << 56);
    val |= ((((long)bytes[offset+1]) & 0xffL) << 48);
    val |= ((((long)bytes[offset+2]) & 0xffL) << 40);
    val |= ((((long)bytes[offset+3]) & 0xffL) << 32);
    val |= ((((long)bytes[offset+4]) & 0xffL) << 24);
    val |= ((((long)bytes[offset+5]) & 0xffL) << 16);
    val |= ((((long)bytes[offset+6]) & 0xffL) << 8);
    val |= (((long)bytes[offset+7]) & 0xffL);
    return val;
  }

}
