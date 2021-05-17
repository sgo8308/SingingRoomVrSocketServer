import java.nio.ByteBuffer;
import java.nio.ByteOrder;

abstract class PacketSession extends Session {
    // 패킷 예시 [size(2)][packetId(2)][ ... ][size(2)][packetId(2)][ ... ]
    public static int HeaderSize = 2;

    @Override
    public final int OnRecv(ByteBuffer buffer) {
        System.out.println("OnRecv Enter");
        int processLen = 0;
        int limitPosition;
        int position;
        int count = 0;
        while (true) {
            count++;
            position = buffer.position();
            limitPosition = buffer.limit();

            System.out.println("OnRecv에서 현재 회수 : " + count + "buffer의 size는 : " + buffer.remaining());

            // 최소한 헤더는 파싱할 수 있는지 확인
            if (buffer.remaining() < HeaderSize)
                break;

            // 패킷이 완전체로 도착했는지 확인
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            short dataSize = buffer.getShort();

            buffer.position(position);

            if (buffer.remaining() < dataSize)
                break;

            // 여기까지 왔으면 패킷 조립 가능
            OnRecvPacket(buffer.wrap(buffer.array(), buffer.position(), dataSize));// 여기서부터 해야 함.
            processLen += dataSize; //처리한 데이터양

            // 한 덩이의 패킷읽었으니까 다음 덩이의 패킷 읽을 수 있게 조정
            buffer = buffer.wrap(buffer.array(), buffer.position() + dataSize, limitPosition - processLen);
        }
        System.out.println("OnRecv Exit");
        return processLen;
    }

    public abstract void OnRecvPacket(ByteBuffer buffer); //abstract로 정의해서 실제로 받은 패킷을 어떻게 할지 정하도록 컨텐츠단인 clientSession으로 넘겨줌
}
