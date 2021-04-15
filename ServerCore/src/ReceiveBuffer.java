import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ReceiveBuffer {
    // [r][][w][][][][][][][] -> 하나의 패킷이 3번에 걸쳐서 분할되서 온다고 할 때 1개가 도착할 때마다 writePos를 옮겨주면서 recvBuffer에 기록하고
    //                           마지막 3개까지 다 도착하면 readPos를 writePos쪽으로 옮겨주면서 그 사이에 있는 값을 실제로 읽어준다. 그 다음에는 clear를 한다.
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

    public int getDataSize(){
        return _writePos - _readPos;
    }

    public int getFreeSize(){
        return _buffer.capacity() - _buffer.position();
    }

//    public ByteBuffer GetReadSegment(){// 현재까지 받은 데이터의 유효 범위
//
//        return _buffer.flip();
//    }

    public ByteBuffer GetReadSegment(){// 현재까지 받은 데이터의 유효 범위

        return _buffer.wrap(_buffer.array(), _buffer.arrayOffset() + _readPos, getDataSize());
    }

//    public ByteBuffer GetWriteSegment(){ //데이터를 더 써도되는 여유 공간 버퍼
//        return _buffer;
//    }

    public ByteBuffer GetWriteSegment(){ //데이터를 더 써도되는 여유 공간 버퍼

        return _buffer.wrap(_buffer.array(), _buffer.arrayOffset() + _writePos, getFreeSize());
    }

//    public void Clean()
//    {
//        if (!_buffer.hasRemaining()) // 클라에서 보낸 데이터를 다 리시브 해준 상태
//        {
//            // 남은 데이터가 없으면 복사하지 않고 포지션만 리셋
//            _buffer.clear();
//        }
//        else
//        {
//            // 남은 찌끄레기가 있으면 시작 위치로 복사
//            if(_buffer.position() == 0){
//                return;
//            }
//
//            _buffer.compact();
//        }
//    }

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

//    public int GetDataSize(){
//        return _buffer.remaining();
//    }
}
