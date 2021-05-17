import java.nio.ByteBuffer;

/*
 *  각 쓰레드들이 자신의 전역변수로 버퍼를 하나씩 생성해서 그 버퍼를 계속 사용하도록 해주는 클래스
 *  버퍼가 꽉 차서 더 이상 못 쓰게 될 경우에는 새로 만들어준다.
 *  Open함수로 충분히 큰 공간을 가진 버퍼를 일단 만들어준 후에
 *  close함수로 실제로 사용한 공간만큼을 가진 버퍼로 줄여준다.
 * */
class SendBufferHelper {
    public static ThreadLocal<SendBuffer> CurrentBuffer = ThreadLocal.withInitial(() -> null);

    public static int ChunkSize = 4096;

    public static ByteBuffer Open(int reserveSize) {

        if (CurrentBuffer.get() == null)
            CurrentBuffer.set(new SendBuffer(ChunkSize));

        if (CurrentBuffer.get().GetFreeSize() < reserveSize) // 여유공간보다 send하려는 크기가 크면 버퍼를 새로 만든다.
            CurrentBuffer.set(new SendBuffer(ChunkSize));

        return CurrentBuffer.get().Open(reserveSize);
    }

    public static ByteBuffer Close(int usedSize) {
        return CurrentBuffer.get().Close(usedSize);
    }
}
