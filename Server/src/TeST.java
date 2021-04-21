import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public class TeST {
    public static void main(String[] args) {
        OperatorManager op = new OperatorManager();
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.put((byte)1);
        buffer.put((byte)2);
        buffer.put((byte)3);
        buffer.put((byte)4);
        buffer.put((byte)5);

        buffer.rewind();

        buffer.wrap(buffer.array(),3, 5);
    }
    public static void log(ByteBuffer buf)
    {
        System.out.println(buf.position() + " ~ " + buf.limit() + " [" + new String(buf.array()) + "]");
    }

    public enum PacketID{
        PlayerEnter,
        PlayerExit,
        PlayerMove
    }

    public interface TestInterface{
        public int test2();
    }
}
