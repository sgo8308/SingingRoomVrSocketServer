import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
        this.socketChannel = socketChannel;
        RegisterReceive();
    }

    public synchronized void Send(ByteBuffer sendBuff){
        _sendQueue.add(sendBuff);
        if(!_isPending){
            RegisterSend();
        }
    }

    void RegisterSend(){
        _isPending = true;
        _pendingList = new ByteBuffer[_sendQueue.size()];
        int i = 0;
        while (_sendQueue.size() > 0) //sendQueue에 모아놓은 SendBuff들 모두 ByteBuffer배열에 모은다.
        {
            ByteBuffer buff = _sendQueue.poll();
            buff.flip(); //이거 해야할지 고민
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

        receiveBuff.Clean();
        ByteBuffer segment = receiveBuff.GetWriteSegment();
        socketChannel.read(segment, segment,
                new CompletionHandler<Integer, ByteBuffer>(){

                    @Override
                    public void completed(Integer byteTransffered, ByteBuffer receiveBuff) {
                        //int processLen = OnRead(readBuff); 보류
                        OnReceiveCompleted(byteTransffered);
                    }

                    @Override
                    public void failed(Throwable exc, ByteBuffer attachment) {
                        System.out.println("receive failed : " + exc);
                    }
                });

    }

    void OnReceiveCompleted(Integer byteTransffered){
        if (byteTransffered > 0)
        {
            try
            {
                // 컨텐츠 쪽으로 데이터를 넘겨주고 얼마나 처리했는지 받는다
                int processLen = OnRecv(receiveBuff.GetReadSegment());
                if (processLen < 0 || receiveBuff.GetDataSize() < processLen)
                {
                    Disconnect();
                    return;
                }

                // buffer 포지션 이동
                if (receiveBuff.OnRead(processLen) == false)
                {
                    Disconnect();
                    return;
                }

                RegisterReceive();
            }
            catch (Exception e)
            {
                System.out.println("OnRecvCompleted Failed : " + e);
            }
        }
        else
        {
            Disconnect();
        }
    }

    public void Disconnect(){
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


}
