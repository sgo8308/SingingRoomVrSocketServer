import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;


public class ClientSession extends PacketSession {
    public int SessionId ;

    OperatorManager op = new OperatorManager();

    public class PacketID{
        public static final short PlayerEnter = 1;
        public static final short PlayerExit = 2;
        public static final short PlayerMove = 3;
    }

    public class PlayerEnterPacket{
        public int playerId;
    }

    public void OnConnected(SocketAddress socketAddress)
    {
        System.out.println("OnConnected : " + socketAddress);

        try{
            Thread.sleep(5000);
        }catch(Exception e){
          e.printStackTrace();
        }
//        Disconnect();
    }

    @Override
    public void OnSend(long numOfBytes) {
        System.out.println("Transferred bytes: " + numOfBytes);
    }

    @Override
    public void OnDisconnected(SocketAddress socketAddress) {
        System.out.println("OnDisconnected : " + socketAddress);

    }
    int o = 0;
    public void OnRecvPacket(ByteBuffer buffer) // 실제로 패킷 정보를 저장하는 부분
    {
        o++;
        System.out.println("-----" + o);
        System.out.println("OnRecvPacket Enter");
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        short size = buffer.getShort();
        short packetId = buffer.getShort();
        System.out.println("Packet Size = "+ size + " , PacketId = " + packetId);

        switch (packetId){
            case PacketID.PlayerEnter :
                System.out.println("packetId ");
                int playerId = buffer.getInt();
                System.out.println("PlayerId = "+ playerId );

                //모두에게 뿌려주기
                ByteBuffer s = SendBufferHelper.Open(4096);
                s.order(ByteOrder.LITTLE_ENDIAN);
                short size2 = 0;
                short packetId2 = PacketID.PlayerEnter;
                size2 += 2;
                s.position(s.position() + 2); // size 넣어줄 공간 2바이트 비워주기
                s.putShort(packetId2);
                size2 += 2;
                s.putInt(30);
                size2 += 4;
                s.position(s.position() - size2); // 비워준 공간으로 포지션 떙겨서 넣기
                s.putShort(size2);
                ByteBuffer sendBuff = SendBufferHelper.Close(size2);

                List<ClientSession> sessions = new ArrayList<>(SessionManager.GetInstance()._sessions.values());

                for (ClientSession session : sessions ) {
                    if(session.socketChannel != super.socketChannel){// 자기 자신 빼고 나머지에게 보내기
                        session.Send(sendBuff.duplicate()); // 그대로 sendbuff를 보내면 각각의 소켓이 같은 버퍼를 공유해서 포지션이나 리미트에 영향을 미침. -> 새벽에 개고생
                    }
                }

                break;

            case PacketID.PlayerExit:

                break;

            case PacketID.PlayerMove:
                break;
            default:
                break;
        }

    }

    // TEMP
    public void Handle_PlayerInfoOk(ByteBuffer buffer)
    {

    }
}
