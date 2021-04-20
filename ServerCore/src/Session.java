import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

abstract class PacketSession extends Session{
    // 패킷 예시 [size(2)][packetId(2)][ ... ][size(2)][packetId(2)][ ... ]
    public static int HeaderSize = 2;

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

    public abstract void OnRecvPacket(ByteBuffer buffer); //abstract로 정의해서 실제로 받은 패킷을 어떻게 할지 정하도록 컨텐츠단인 clientSession으로 넘겨줌
}


/*
* 전체적인 로직
*
* 소켓을 연결한 후에 비동기로 데이터를 전송하고 받는 부분이 이뤄지는 부분
* 데이터를 받는 receive의 경우는 언제 데이터가 날라올지 모르기 때문에 처음부터 함수를 실행해서 쓰레드풀에 작업큐에 등록해놓고
* send의 경우는 실제로 서버에서 send를 할 때만 사용
* send의 경우 sendBuffer를 모아서 한번에 보내는 방식 사용 -> 효율성을 위해
*
* */

public abstract class Session {
    AsynchronousSocketChannel socketChannel;
    int disconnected = 0;
    boolean _isPending = false;

    ReceiveBuffer receiveBuff = new ReceiveBuffer(1024);

    Queue<ByteBuffer> _sendQueue = new LinkedList<>(); //sendBuffer들을 모아 놓는 큐. 모아 놓았닫가 한번에 보낸다.
    ByteBuffer[] _pendingList ;

    //컨텐츠단인 clientSession에서 처리할 수 있게 abstract로 선언
    public abstract void OnConnected(SocketAddress socketAddress);
    public abstract int  OnRecv(ByteBuffer receiveBuffer);
    public abstract void OnSend(long numOfBytes);
    public abstract void OnDisconnected(SocketAddress socketAddress);

    public void Start(AsynchronousSocketChannel socketChannel){
        System.out.println("Session Start Enter");
        this.socketChannel = socketChannel;
        RegisterReceive(); // 시작하자마자 receive를 받을 수 있는 낚시대를 던져준다.
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
                _pendingList = new ByteBuffer[1024]; // 모아놓은 sendBuff들 비워주기 위해서 새로 만든다. 딱히 clean 이런 메소드를 쓸 수 없어서 일단 그냥 재사용 못하고 새로 만듬
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
            Disconnect(new Object() {}.getClass().getEnclosingMethod().getName());
        }
        System.out.println("Session OnSendCompleted Exit");

    }

    void RegisterReceive(){ // accept와 마찬가지로 receive하는 작업을 쓰레드풀에 작업큐에 넣어주는 작업. 낚시대를 던져준다고 생각하면 편함
        System.out.println("Session RegisterReceive Enter");

        receiveBuff.Clean();
        ByteBuffer segment = receiveBuff.GetWriteSegment(); // 리시브 버퍼 인스턴스로부터 새로 데이터를 받을 버퍼를 생성해준다.
        socketChannel.read(segment, segment,
                new CompletionHandler<Integer, ByteBuffer>(){ // receive완료 되면 결과에 따라 처리하는 핸들러

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
    void OnReceiveCompleted(Integer byteTransffered){
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
                    Disconnect(new Object() {}.getClass().getEnclosingMethod().getName());
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
            Disconnect( new Object() {}.getClass().getEnclosingMethod().getName());
        }
    }

    public void Disconnect(String method){
        System.out.println("Session Disconnect Enter, thread is : "+ Thread.currentThread().getName() + "method :" + method);

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
