import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/*
* 데이터를 하나만  보내는게 아니라 여러개를 한번에 보내면 sendBuffer에 저장해서 보내야 함
* sendBuffer는 receiveBuffer와 다르게 남은 공간이 소진될 때까지만 쓰고 새로 만듬
* clean이 없는 이유는 동일 패킷을 100명한테 보낼 때 다른 한 명이 send가 끝났다고 clean을 해버리면 다른 애들이 영향 받기 때문
* */

public class SendBuffer
{
    ByteBuffer _buffer;
    int _usedSize = 0;
    public int GetFreeSize() {
        return _buffer.capacity() - _usedSize;
    }

    public SendBuffer(int chunkSize)
    {
        _buffer = ByteBuffer.allocate(chunkSize);
        _buffer.order(ByteOrder.LITTLE_ENDIAN);// c#과 통신하기 위해서 LITTLE_ENDIAN으로 바꿔줌
    }

    public ByteBuffer Open(int reserveSize){ // open과 close 안에 send할 데이터를 담아줌
        if (reserveSize > GetFreeSize())
            return null;

        return _buffer.wrap(_buffer.array(), _usedSize, reserveSize); // _buffer로부터 _usedSize position부터 reserveSize뒤에 limit를 만들어줌
    }

    public ByteBuffer Close(int usedSize){
        ByteBuffer segment = _buffer.wrap(_buffer.array(),_usedSize, usedSize); // _buffer로부터 _usedSize position부터 usedSize뒤에 limit를 만들어서 이 사이에 있는 데이터만 보내게 함.
        _usedSize += usedSize;
        return segment;
    }
}
