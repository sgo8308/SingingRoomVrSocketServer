import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class OperatorManager {
    //https://202psj.tistory.com/531 <- 출처
    //Endian은 바이트 순서. 자바는 Big ENDIAN을 쓰고 다른 언어는 주롤 Little Endian을 쓰기 때문에 나누어서 처리한다.

    public final static int BIG_ENDIAN = 1;
    public final static int Little_ENDIAN = 2;

    public byte[] intToBytes(int Value, int endian){
        byte[] temp = new byte[4];
        ByteBuffer buff = ByteBuffer.allocate(Integer.SIZE/8);
        buff.putInt(Value);
        temp = ChangeByteOrder(buff.array(), endian);
        return temp;
    }

    public int BytesToInt(byte[] Value, int offset, int endian){
        ByteBuffer buff = ByteBuffer.allocate(4);
        buff = ByteBuffer.wrap(Value);

        if(endian == BIG_ENDIAN){
            buff.order(ByteOrder.BIG_ENDIAN);
        }else if(endian == Little_ENDIAN){
            buff.order(ByteOrder.LITTLE_ENDIAN);
        }
        return buff.getInt();
    }

    public int ByteBuffToInt(ByteBuffer byteBuff, int endian){
        if(endian == BIG_ENDIAN){
            byteBuff.order(ByteOrder.BIG_ENDIAN);
        }else if(endian == Little_ENDIAN){
            byteBuff.order(ByteOrder.LITTLE_ENDIAN);
        }
        return byteBuff.getInt();
    }

    public byte[] ShortToBytes(short Value, int endian){
        byte[] temp;
        temp = new byte[]{(byte)((Value & 0xFF00) >> 8),(byte)(Value & 0x00FF)};
        temp = ChangeByteOrder(temp, endian);
        return temp;
    };

    public short BytesToShort(byte[] Value, int offset, int endian){
        short newValue = 0;
        byte[] temp = Value;

        temp = ChangeByteOrder(temp, endian);

        newValue |= (((short)temp[offset]) << 8) & 0xFF00;
        newValue |= ((short)temp[offset + 1]) & 0xFF;

        return newValue;
    }

//    public short ByteBuffToShort(ByteBuffer byteBuff, int offset, int endian){
//        short newValue = 0;
//        byte[] temp = byteBuff.array();
//
//        temp = ChangeByteOrder(temp, endian);
//
//        newValue |= (((short)temp[offset]) << 8) & 0xFF00;
//        newValue |= ((short)temp[offset + 1]) & 0xFF;
//
//        byteBuff.position(byteBuff.position() + 2);
//        return newValue;
//    }

    public byte[] ChangeByteOrder(byte[] value ,int endian){
        int idx = value.length;
        byte[] Temp = new byte[idx];

        if(endian == BIG_ENDIAN){
            Temp = value;
        }else if(endian == Little_ENDIAN){
            for(int i=0; i < idx; i++) {
                Temp[i] = value[idx-(i+1)];
            }
        }
        return Temp;
    }
}
