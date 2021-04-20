import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class Session {
    AsynchronousSocketChannel socketChannel;
    int disconnected = 0;
    boolean _isPending = false;

    ByteBuffer receiveBuff = ByteBuffer.allocate(1024);

    Queue<ByteBuffer> _sendQueue = new LinkedList<>();
    ByteBuffer[] _pendingList ; // 크기를 얼마나 크게 해야할까?

    public void Start(AsynchronousSocketChannel socketChannel){
        this.socketChannel = socketChannel;
        RegisterReceive();
        ByteBuffer sendBuff = ByteBuffer.allocate(30);
        sendBuff.put((byte)1);
        sendBuff.put((byte)2);
        sendBuff.put((byte)3);
        sendBuff.put((byte)4);
        sendBuff.put((byte)5);
        Send(sendBuff);
    }

    public synchronized void Send(ByteBuffer sendBuff){
        _sendQueue.add(sendBuff);

        if(!_isPending){
            RegisterSend();
        }
    }

    void RegisterSend(){
        _isPending = true;
        int i = 0;
        _pendingList = new ByteBuffer[_sendQueue.size()];
        while (_sendQueue.size() > 0) //sendQueue에 모아놓은 SendBuff들 모두 ByteBuffer배열에 모은다.
        {
            ByteBuffer buff = _sendQueue.poll();
            buff.flip();
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
        System.out.println(_pendingList[0].array()[0]);
        System.out.println(_pendingList[0].array()[1]);
        System.out.println(_pendingList[0].array()[2]);

        // 이 부분에 혹시 펜딩 익셉션 뜨나 체크
    }

    synchronized void OnSendCompleted(Long bytesTransferred){
        if(bytesTransferred > 0){
            try
            {
                _pendingList = new ByteBuffer[1024]; // 모아놓은 sendBuff들 비워주기 위해서 새로 만든다.
                _isPending = false;

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
        //			_recvBuffer.Clean();
        //			ArraySegment<byte> segment = _recvBuffer.WriteSegment;
        //			_recvArgs.SetBuffer(segment.Array, segment.Offset, segment.Count);
        socketChannel.read(receiveBuff, receiveBuff,
                new CompletionHandler<Integer, ByteBuffer>(){

                    @Override
                    public void completed(Integer byteTransffered, ByteBuffer receiveBuff) {
                        //int processLen = OnRead(readBuff); 보류
                        OnReceiveCompleted(byteTransffered, receiveBuff);
                    }

                    @Override
                    public void failed(Throwable exc, ByteBuffer attachment) {
                        System.out.println("receive failed : " + exc);
                    }
                });

    }

    void OnReceiveCompleted(Integer byteTransffered, ByteBuffer receiveBuff){
        if (byteTransffered > 0)
        {
            System.out.println(Arrays.toString(receiveBuff.array()));
            try
            {
//                // Write 커서 이동
//                if (_recvBuffer.OnWrite(args.BytesTransferred) == false)
//                {
//                    Disconnect();
//                    return;
//                }
//
//                // 컨텐츠 쪽으로 데이터를 넘겨주고 얼마나 처리했는지 받는다
//                int processLen = OnRecv(_recvBuffer.ReadSegment);
//                if (processLen < 0 || _recvBuffer.DataSize < processLen)
//                {
//                    Disconnect();
//                    return;
//                }
//
//                // Read 커서 이동
//                if (_recvBuffer.OnRead(processLen) == false)
//                {
//                    Disconnect();
//                    return;
//                }

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
