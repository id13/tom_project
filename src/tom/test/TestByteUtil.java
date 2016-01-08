package tom.test;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Test;

import messages.util.ByteUtil;

public class TestByteUtil {

  @Test
  public void test() {
    byte[] bytes = new byte[8];
    ByteUtil.writeLong64(bytes, 0, 12345678910123L);
    assertEquals(12345678910123L, ByteUtil.readLong64(bytes, 0));

    ByteUtil.writeLong64(bytes, 0, -456789L);
    assertEquals(-456789L, ByteUtil.readLong64(bytes, 0));
    
    
    byte[] bytes1 = new byte[7];
    bytes1[0] = 0;
    bytes1[1] = 42;
    bytes1[2] = -42;
    bytes1[3] = 127;
    bytes1[4] = -128;
    bytes1[5] = -127;
    bytes1[6] = -100;
    String gloomyMessage = ByteUtil.readString(bytes1);
    byte[] bytes2 = ByteUtil.writeString(gloomyMessage);
    assertEquals(0,bytes2[0]);
    assertEquals(42,bytes2[1]);
    assertEquals(-42,bytes2[2]);
    assertEquals(127,bytes2[3]);
    assertEquals(-128,bytes2[4]);
    assertEquals(-127,bytes2[5]);
    assertEquals(-100,bytes2[6]);

    byte[] bytes3 = new byte[256];
    for (int i=0; i<256; i++) {
      bytes3[i] = (byte) i;
    }
    String string = ByteUtil.readString(bytes3);
    byte[] bytes4 = ByteUtil.writeString(string);
    assertTrue(Arrays.equals(bytes3, bytes4));
  }

}
