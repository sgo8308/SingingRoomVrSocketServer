import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ClientSession extends PacketSession { // 클라이언트의 세션을 의미한다. 이 세션을 통해 서버는 클라이언트와 통신 가능. 연결된 클라이언트의 대리인이라고 이해.
                                                   // 실제로 가장 앞단에서 이루어지는 것들을 이 클래스의 콜백 메소드들을 통해서 정의한다.
    public int SessionId ; //세션의 개별 ID

    public class PacketID{ // 패킷의 개별 ID
        public static final short PlayerEnter = 1;
        public static final short PlayerMove = 2;
        public static final short Players = 3;
        public static final short PlayerExit = 4;
    }

    public class PlayerEnterPacket{
        public int playerId;
    }

    public void OnConnected(SocketAddress socketAddress) // 연결이 되었을 때 수행할 작업
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

    public void OnRecvPacket(ByteBuffer buffer) // 패킷을 받았을 때 행할 작업
    {
        System.out.println("OnRecvPacket Enter");
        buffer.order(ByteOrder.LITTLE_ENDIAN); // C#으로 부터 패킷을 받았으므로 LITTLE_ENDIAN 형식으로 버퍼를 바꾸어서 패킷 안에 데이터를 받아준다.
        short size = buffer.getShort();
        short packetId = buffer.getShort();
        int playerId = buffer.getInt();
        int roomId = buffer.getInt();

        SessionId = playerId;

        System.out.println("Packet Size = "+ size + " , PacketId = " + packetId);
        System.out.println("PlayerId = "+ playerId + ", roomId = "+ roomId );

        //방이 없으면 방 만들고 있으면 원래 방 쓰기
        HashMap<Integer, List<ClientSession>> sessionsByRoom = SessionManager.GetInstance()._sessionsByRoom;

        switch (packetId){
            case PacketID.PlayerEnter :
                System.out.println("OnRecvPacket case PlayerEnter Enter");
                if(sessionsByRoom.get(roomId) == null){
                    List<ClientSession> clientSessions = new ArrayList<>();
                    clientSessions.add(this);
                    sessionsByRoom.put(roomId, clientSessions);
                    return;
                }
                sessionsByRoom.get(roomId).add(this);
                Handle_PlayerEnter(packetId, playerId, roomId, sessionsByRoom);
                break;

            case PacketID.PlayerMove:
                System.out.println("OnRecvPacket case PlayerMove Enter");
                Handle_PlayerMove(buffer, packetId, playerId, roomId, sessionsByRoom);
                break;

            case PacketID.PlayerExit:
                System.out.println("OnRecvPacket case PlayerExit Enter");

                break;
            default:
                break;
        }

    }

    /*
    * 로직
    * 플레이어 id를 받은 후에
    * sendBufferHelper를 열어서
    * size를 넣어줄 버퍼의 앞 공간은 비워두고
    * 패킷id와 플레이어id를 밀어넣고
    * 마지막으로 총 size를 버퍼의 맨 앞으로 넣은 후에
    * sendvufferHelper를 닫아서 패킷을 완성해준다.
    * */
    public void Handle_PlayerEnter(short packetId, int playerId, int roomId, HashMap<Integer, List<ClientSession>> sessionsByRoom){
        //플레이어가 들어왔다는 데이터를 현재 접속 중인 모든 유저에게 뿌려주기
        ByteBuffer s = SendBufferHelper.Open(4096);
        s.order(ByteOrder.LITTLE_ENDIAN); // C#에 맞게 보내기 위해 Buffer Order를 변경해줌
        short size = 0;
        size += 2;
        s.position(s.position() + 2); // size 넣어줄 공간 2바이트 비워주기
        s.putShort(packetId);
        size += 2;
        s.putInt(playerId);
        size += 4;
        s.position(s.position() - size); // 비워준 공간으로 포지션 떙겨서 넣기
        s.putShort(size);
        ByteBuffer sendBuff = SendBufferHelper.Close(size);

        List<ClientSession> sessions = sessionsByRoom.get(roomId);

        for (ClientSession session : sessions ) {
            if(session.socketChannel != super.socketChannel){// 자기 자신 빼고 나머지에게 보내기
                session.Send(sendBuff.duplicate()); // 그대로 sendbuff를 보내면 각각의 소켓이 같은 버퍼를 공유해서 포지션이나 리미트에 영향을 미침. -> 새벽에 개고생
            }
        }

        //지금 들어온 유저에게 접속중이었던 사람들 리스트 뿌려주기
        ByteBuffer s2 = SendBufferHelper.Open(4096);
        s2.order(ByteOrder.LITTLE_ENDIAN); // C#에 맞게 보내기 위해 Buffer Order를 변경해줌
        short size2 = 0;
        size2 += 2;
        s2.position(s2.position() + 2); // size 넣어줄 공간 2바이트 비워주기
        s2.putShort(PacketID.Players);
        size2 += 2;
        s2.putShort((short)(sessions.size() - 1)); // 자기 자신빼고 총 인원수
        size2 += 2;
        for (ClientSession session : sessions) {
            if(session.socketChannel != super.socketChannel){// 자기 자신 빼고 나머지 버퍼에 넣기
                s2.putInt(session.SessionId);
                size2 += 4;
            }
        }
        s2.position(s2.position() - size2); // 비워준 공간으로 포지션 떙겨서 넣기
        s2.putShort(size2);
        ByteBuffer sendBuff2 = SendBufferHelper.Close(size2);

        Send(sendBuff2.duplicate());
    }

    public void Handle_PlayerMove(ByteBuffer buffer, short packetId, int playerId, int roomId,
                                  HashMap<Integer, List<ClientSession>> sessionsByRoom){
        float headPosX = buffer.getFloat();
        float headPosY = buffer.getFloat();
        float headPosZ = buffer.getFloat();
        float headRotX = buffer.getFloat();
        float headRotY = buffer.getFloat();
        float headRotZ = buffer.getFloat();
        float headRotW = buffer.getFloat();
        System.out.println("headPos = (" +headPosX+", "+headPosY+ ", " + headPosZ+")");
        float LHandPosX = buffer.getFloat();
        float LHandPosY = buffer.getFloat();
        float LHandPosZ = buffer.getFloat();
        float LHandRotX = buffer.getFloat();
        float LHandRotY = buffer.getFloat();
        float LHandRotZ = buffer.getFloat();
        float LHandRotW = buffer.getFloat();
        System.out.println("LHandPos = (" +LHandPosX+", "+LHandPosY+ ", " + LHandPosZ+")");

        float RHandPosX = buffer.getFloat();
        float RHandPosY = buffer.getFloat();
        float RHandPosZ = buffer.getFloat();
        float RHandRotX = buffer.getFloat();
        float RHandRotY = buffer.getFloat();
        float RHandRotZ = buffer.getFloat();
        float RHandRotW = buffer.getFloat();
        System.out.println("RHandPos = (" +RHandPosX+", "+RHandPosY+ ", " + RHandPosZ+")");

        float parentPosX = buffer.getFloat();
        float parentPosY = buffer.getFloat();
        float parentPosZ = buffer.getFloat();
        float parentRotX = buffer.getFloat();
        float parentRotY = buffer.getFloat();
        float parentRotZ = buffer.getFloat();
        float parentRotW = buffer.getFloat();
        System.out.println("parentPos = (" +parentPosX+", "+parentPosY+ ", " + parentPosZ+")");

        //보낸 사람 빼고 뿌려주기
        ByteBuffer bf = SendBufferHelper.Open(4096);
        System.out.println("하이 1 ");
        bf.order(ByteOrder.LITTLE_ENDIAN);
        short size = 0;
        size += 2;
        bf.position(bf.position() + 2); // size 넣어줄 공간 2바이트 비워주기
        bf.putShort(PacketID.PlayerMove);
        size += 2;
        bf.putInt(playerId);
        size += 4;
        System.out.println("하이 2 ");

        bf.putFloat(headPosX);
        size += 4;
        bf.putFloat(headPosY);
        size += 4;
        bf.putFloat(headPosZ);
        size += 4;
        bf.putFloat(headRotX);
        size += 4;
        bf.putFloat(headRotY);
        size += 4;
        bf.putFloat(headRotZ);
        size += 4;
        bf.putFloat(headRotW);
        size += 4;
        System.out.println("하이 3 ");

        bf.putFloat(LHandPosX);
        size += 4;
        bf.putFloat(LHandPosY);
        size += 4;
        bf.putFloat(LHandPosZ);
        size += 4;
        bf.putFloat(LHandRotX);
        size += 4;
        bf.putFloat(LHandRotY);
        size += 4;
        bf.putFloat(LHandRotZ);
        size += 4;
        bf.putFloat(LHandRotW);
        size += 4;
        System.out.println("하이 4 ");

        bf.putFloat(RHandPosX);
        size += 4;
        bf.putFloat(RHandPosY);
        size += 4;
        bf.putFloat(RHandPosZ);
        size += 4;
        bf.putFloat(RHandRotX);
        size += 4;
        bf.putFloat(RHandRotY);
        size += 4;
        bf.putFloat(RHandRotZ);
        size += 4;
        bf.putFloat(RHandRotW);
        size += 4;
        System.out.println("하이 5 ");

        bf.putFloat(parentPosX);
        size += 4;
        bf.putFloat(parentPosY);
        size += 4;
        bf.putFloat(parentPosZ);
        size += 4;
        bf.putFloat(parentRotX);
        size += 4;
        bf.putFloat(parentRotY);
        size += 4;
        bf.putFloat(parentRotZ);
        size += 4;
        bf.putFloat(parentRotW);
        size += 4;
        System.out.println("하이 6 ");

        bf.position(bf.position() - size); // 비워준 공간으로 포지션 떙겨서 넣기
        bf.putShort(size);
        ByteBuffer sendBuff = SendBufferHelper.Close(size);
        System.out.println("하이 7 ");

        List<ClientSession> sessions = sessionsByRoom.get(roomId);
        System.out.println("하이 8 ");

        for (ClientSession session : sessions ) {
            if(session.socketChannel != super.socketChannel) {
                session.Send(sendBuff.duplicate()); // 그대로 sendbuff를 보내면 각각의 소켓이 같은 버퍼를 공유해서 포지션이나 리미트에 영향을 미침. -> 새벽에 개고생
            }
        }
        System.out.println("하이 9 ");

    }

    public void Handle_PlayerMoveTest(ByteBuffer buffer, short packetId){
        int playerId = buffer.getInt();
        int direction = buffer.getInt(); //이동하는 방향
        System.out.println("PlayerId = "+ playerId );
        System.out.println("direction = "+ direction );

        //모두에게 뿌려주기
        ByteBuffer bf = SendBufferHelper.Open(4096);
        bf.order(ByteOrder.LITTLE_ENDIAN);
        short size = 0;
        size += 2;
        bf.position(bf.position() + 2); // size 넣어줄 공간 2바이트 비워주기
        bf.putShort(packetId);
        size += 2;
        bf.putInt(playerId);
        size += 4;
        bf.putInt(direction);
        size += 4;
        bf.position(bf.position() - size); // 비워준 공간으로 포지션 떙겨서 넣기
        bf.putShort(size);
        ByteBuffer sendBuff = SendBufferHelper.Close(size);

        List<ClientSession> sessions = new ArrayList<>(SessionManager.GetInstance()._allSessions.values());

        for (ClientSession session : sessions ) {
            session.Send(sendBuff.duplicate()); // 그대로 sendbuff를 보내면 각각의 소켓이 같은 버퍼를 공유해서 포지션이나 리미트에 영향을 미침. -> 새벽에 개고생
        }
    }
}
