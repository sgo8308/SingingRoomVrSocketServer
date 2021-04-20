import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/*
 *  각 쓰레드들이 자신의 전역변수로 버퍼를 하나씩 생성해서 그 버퍼를 계속 사용하도록 해주는 클래스
 *  버퍼가 꽉 차서 더 이상 못 쓰게 될 경우에는 새로 만들어준다.
 *  Open함수로 충분히 큰 공간을 가진 버퍼를 일단 만들어준 후에
 *  close함수로 실제로 사용한 공간만큼을 가진 버퍼로 줄여준다.
 * */
class SendBufferHelper
{
    public static ThreadLocal<SendBuffer> CurrentBuffer = ThreadLocal.withInitial(() -> null);

    public static int ChunkSize = 4096 * 100;

    public static ByteBuffer Open(int reserveSize) {

        if (CurrentBuffer.get() == null)
        CurrentBuffer.set(new SendBuffer(ChunkSize));

        if (CurrentBuffer.get().GetFreeSize() < reserveSize) // 여유공간보다 send하려는 크기가 크면 버퍼를 새로 만든다.
        CurrentBuffer.set(new SendBuffer(ChunkSize));

        return CurrentBuffer.get().Open(reserveSize);
    }

    public static ByteBuffer Close(int usedSize)
    {
        return CurrentBuffer.get().Close(usedSize);
    }
}

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
        return _buffer.capacity() - _buffer.position();
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
