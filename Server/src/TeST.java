import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TeST {
    public static void main(String[] args) {
        int result = 28320000%28320000;
        System.out.println(result);


    }

    static synchronized void test1(){
        while(true){
            try{Thread.sleep(1000);
            }catch(Exception e){}
            System.out.println("test1 is going");
        }
    }

    static synchronized void test2(){
        System.out.println("test2 is going");
    }

    public byte[] changeByteOrder(byte[] value){
        int idx = value.length;
        byte[] Temp = new byte[idx-2];

        for(int i=2; i < idx-2; i++) {
            Temp[i] = value[idx-(i+1)];
        }

        return Temp;
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
