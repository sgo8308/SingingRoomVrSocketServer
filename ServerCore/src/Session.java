import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

abstract class PacketSession extends Session{
    public static int HeaderSize = 2;

    OperatorManager op = new OperatorManager();
    // [size(2)][packetId(2)][ ... ][size(2)][packetId(2)][ ... ]

    @Override
    public final int OnRecv(ByteBuffer buffer) {
        System.out.println("OnRecv Enter");
        int processLen = 0;
        int limitPosition;
        int position ;
        while (true)
        {
            position = buffer.position();
            limitPosition = buffer.limit();

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
            OnRecvPacket(buffer.wrap(buffer.array(), buffer.position(), buffer.position() + dataSize));// 여기서부터 해야 함.

            processLen += dataSize; //처리한 데이터양

            // 한 덩이의 패킷읽었으니까 다음 덩이의 패킷 읽을 수 있게 조정
            buffer = buffer.wrap(buffer.array(),buffer.position() + dataSize,  limitPosition - processLen);
        }

        return processLen;
    }

    public abstract void OnRecvPacket(ByteBuffer buffer);
}

public abstract class Session {
    AsynchronousSocketChannel socketChannel;
    int disconnected = 0;
    boolean _isPending = false;

    ReceiveBuffer receiveBuff = new ReceiveBuffer(1024);

    Queue<ByteBuffer> _sendQueue = new LinkedList<>();
    ByteBuffer[] _pendingList ;

    public abstract void OnConnected(SocketAddress socketAddress);
    public abstract int  OnRecv(ByteBuffer receiveBuffer);
    public abstract void OnSend(long numOfBytes);
    public abstract void OnDisconnected(SocketAddress socketAddress);

    public void Start(AsynchronousSocketChannel socketChannel){
        System.out.println("Session Start Enter");
        this.socketChannel = socketChannel;
        RegisterReceive();
    }

    public synchronized void Send(ByteBuffer sendBuff){// 컨텐츠단에서는 이 send만 사용
        System.out.println("Session Send Enter");
        _sendQueue.add(sendBuff);
        if(!_isPending){
            RegisterSend();
        }
    }



    //region 네트워크 통신 부분 (컨텐츠단에서는 여기 메소드를 쓸 일 없음)
    void RegisterSend(){
        System.out.println("Session RegisterSend Enter");
        _isPending = true;
        _pendingList = new ByteBuffer[_sendQueue.size()];
        int i = 0;
        while (_sendQueue.size() > 0) //sendQueue에 모아놓은 SendBuff를 모두 ByteBuffer배열에 모은다.
        {
            ByteBuffer buff = _sendQueue.poll();
            _pendingList[i] = buff;
            i++;
        }

        socketChannel.write(_pendingList,0, _pendingList.length,1, TimeUnit.HOURS, null, // SendBuff들 모아서 한번에 보내기
                new CompletionHandler<Long, Void>(){ // Long은 Send완료 후 결과물 (전송한 데이터 길이) Void는 첨부할게 없다는 뜻

                    @Override
                    public void completed(Long bytesTransferred, Void attachment) {
                        OnSendCompleted(bytesTransferred);
                    }

                    @Override
                    public void failed(Throwable exc, Void attachment) {
                        System.out.println("Send failed : " + exc);
                    }
                });
        // 이 부분에 혹시 펜딩 익셉션 뜨나 체크
    }

    synchronized void OnSendCompleted(Long bytesTransferred){
        System.out.println("Session OnSendCompleted Enter");
        if(bytesTransferred > 0){
            try
            {
                _pendingList = new ByteBuffer[1024]; // 모아놓은 sendBuff들 비워주기 위해서 새로 만든다.
                _isPending = false;

                OnSend(bytesTransferred); // 컨텐츠단에서 처리 할 수 있게 호출해줌

                if (_sendQueue.size() > 0) // 혹시 Send하는 동안 큐에 쌓였으면 이 쌓인 것들 또 Send 해주기
                    RegisterSend();
            }
            catch (Exception e)
            {
                System.out.println("OnSendCompleted Failed : " + e);
            }
        }else{
            Disconnect();
        }
    }

    void RegisterReceive(){
        System.out.println("Session RegisterReceive Enter");

        receiveBuff.Clean();
        ByteBuffer segment = receiveBuff.GetWriteSegment();
        socketChannel.read(segment, segment,
                new CompletionHandler<Integer, ByteBuffer>(){

                    @Override
                    public void completed(Integer byteTransffered, ByteBuffer receiveBuff) {
                        OnReceiveCompleted(byteTransffered);
                    }

                    @Override
                    public void failed(Throwable exc, ByteBuffer attachment) {
                        System.out.println("receive failed : " + exc);
                    }
                });

    }
    int breakPoint = 0;
    void OnReceiveCompleted(Integer byteTransffered){
        breakPoint ++;
        System.out.println("Session OnReceiveCompleted Enter");
        if (byteTransffered > 0)
        {
            System.out.println("Session OnReceiveCompleted first if state enter");
            try
            {
                //wirte 커서 이동
                receiveBuff.OnWrite(byteTransffered);

                // 컨텐츠 쪽으로 데이터를 넘겨주고 얼마나 처리했는지 받는다
                int processLen = OnRecv(receiveBuff.GetReadSegment());
                if (processLen < 0)
                {
                    Disconnect();
                    return;
                }

                // read 커서 이동
                receiveBuff.OnRead(processLen);
//                if (receiveBuff.OnRead(processLen) == false)
//                {
//                    Disconnect();
//                    return;
//                }

                RegisterReceive();
            }
            catch (Exception e)
            {
                System.out.println("OnRecvCompleted Failed : " + e + " " + Thread.currentThread().getName());
            }
        }
        else
        {
            Disconnect();
        }
    }

    public void Disconnect(){
        System.out.println("Session Disconnect Enter, thread is : "+ Thread.currentThread().getName());

        synchronized(this){ // 혹시나 쓰레드가 소켓을 두 번 닫으면 안되니까 동기화
            if(disconnected == 1){
                return;
            }
            disconnected = 1;
        }
        try{
            socketChannel.shutdownInput();
            socketChannel.shutdownOutput();
            socketChannel.close();
        }catch(Exception e){
            System.out.println("Disconnect exception : " + e);
        }
    }
    //endregion


}
