import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class ClientSession extends PacketSession {
    public void OnConnected(SocketAddress socketAddress)
    {
        System.out.println("OnConnected : " + socketAddress);
        try{
            Thread.sleep(5000);
        }catch(Exception e){
          e.printStackTrace();
        }
        Disconnect();
    }

    @Override
    public void OnSend(long numOfBytes) {
        System.out.println("Transferred bytes: " + numOfBytes);
    }

    @Override
    public void OnDisconnected(SocketAddress socketAddress) {
        System.out.println("OnDisconnected : " + socketAddress);

    }

    public void OnRecvPacket(ByteBuffer buffer)
    {
        int pos = 0;

//        ushort size = BitConverter.ToUInt16(buffer.Array, buffer.Offset);
//        pos += 2;
//        ushort id = BitConverter.ToUInt16(buffer.Array, buffer.Offset + pos);
//        pos += 2;
//
//        // TODO
//        switch ((PacketID)id)
//        {
//            case PacketID.PlayerInfoReq:
//            {
//                long playerId = BitConverter.ToInt64(buffer.Array, buffer.Offset + pos);
//                pos += 8;
//            }
//            break;
//            case PacketID.PlayerInfoOk:
//            {
//                int hp = BitConverter.ToInt32(buffer.Array, buffer.Offset + pos);
//                pos += 4;
//                int attack = BitConverter.ToInt32(buffer.Array, buffer.Offset + pos);
//                pos += 4;
//            }
//            //Handle_PlayerInfoOk();
//            break;
//            default:
//                break;
//        }
        System.out.println("RecvPacketId: {id}, Size {size}");
    }

    // TEMP
    public void Handle_PlayerInfoOk(ByteBuffer buffer)
    {

    }
}
