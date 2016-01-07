package tom.test;

import static org.junit.Assert.*;

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
  }

}
