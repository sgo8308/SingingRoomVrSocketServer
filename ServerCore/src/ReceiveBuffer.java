import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ReceiveBuffer {
    /*
    * 전체적인 로직
    *
    * [r][][w][][][][][][][] 이런식으로 버퍼가 있고 readPosition과 writePosition을 정의한다.
    * writePosition부터 데이터를 채워나가고 데이터가 채워진만큼 writePosition은 옆으로 이동을 한다.
    * 패킷에 정보가 한 번에 다오는게 아니기 때문에 모든 정보가 다 올 때까지 readPositon은 움직이지 않고 기다리다가
    * 모든 정보가 다오 면 readPosition부터 writePosition 전까지 데이터를 읽어준다.
    * 데이터를 읽은 후에는 readPosition을 읽은만큼 옮겨주고 버퍼를 다시 원상 복구 시켜준다.
    *
    * 이 때 2가지 경우가 잇는데
    * 데이터를 읽고 나서 readPosition과 writePosition이 곂치는 경우, 곂치지 않는 경우가 있다.
    * 첫번째 경우에는 r과w를 맨 앞으로 옮겨주면 된다.
    * 두번째 경우에는 r과 w와 그 사이의 데이터를 함께 맨 앞으로 옮겨주면 된다
    */

    /*
    * ByteBuffer 설명
    * 데이터 통신시 position부터 limit 사이에 있는 값을 보낸다.
    * 같은 array를 공유하는 버퍼들은 그 array의 값이 바뀌면 모두 영향을 받는다.
    * wrap 함수의 경우 인자로 넣어준 array를 공유하는 새로운 버퍼를 만드는데 달라지는 점은 position과 limit이다.
    * 아래 함수들에서 wrap이나 duplicate로 계속 새로 만들어주는 이유는 position과 limit를 독립적으로 갖고있는 버퍼들을 사용해야 코드가 덜 꼬이기 때문
    * */

    ByteBuffer _buffer;
    int _readPos;
    int _writePos;

    public int DataSize ;
    public int FreeSize ;

    public ReceiveBuffer(int bufferSize)
    {
        _buffer = ByteBuffer.allocate(bufferSize);
        _buffer.order(ByteOrder.LITTLE_ENDIAN);// c#과 통신하기 위해서 LITTLE_ENDIAN으로 바꿔줌
    }

    public int getDataSize(){ // 현재까지 받은 데이터의 총 사이즈
        return _writePos - _readPos;
    }

    public int getFreeSize(){ // 버퍼의 남은 여유공간
        return _buffer.capacity() - _buffer.position();
    }

    public ByteBuffer GetReadSegment(){// 현재까지 받은 데이터의 유효 범위의 position과 limit를 갖고 있는 버퍼를 리턴

        return _buffer.wrap(_buffer.array(), _buffer.arrayOffset() + _readPos, getDataSize());
    }


    public ByteBuffer GetWriteSegment(){ //데이터를 더 써도되는 여유 공간 버퍼를 리턴

        return _buffer.wrap(_buffer.array(), _buffer.arrayOffset() + _writePos, getFreeSize());
    }

    public void Clean(){
        int dataSize = getDataSize();
        if(dataSize == 0){
            _readPos = _writePos = 0;
        }else{
            _buffer.position(_readPos);
            _buffer.limit(_writePos + 1);
            _buffer.compact();
            _buffer.clear();
        }
    }

    public boolean OnRead(int numOfBytes)
    {
//        if (numOfBytes > _buffer.remaining())
//            return false;

        _readPos += numOfBytes;
        return true;
    }

    public boolean OnWrite(int numOfBytes)
    {
//        if (numOfBytes > FreeSize)
//            return false;

        _writePos += numOfBytes;
        return true;
    }
}
