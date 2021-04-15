import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Executors;

public class Listener {
    AsynchronousChannelGroup channelGroup;
    AsynchronousServerSocketChannel serverSocketChannel;
    SessionFactory _sessionFactory;
    public void Init(SessionFactory sessionFactory){
        System.out.println("Listener Init Enter");
        try{
            channelGroup = AsynchronousChannelGroup.withFixedThreadPool(
                    Runtime.getRuntime().availableProcessors(),
                    Executors.defaultThreadFactory()
            ); // 비동기 채널 그룹을 만들고 쓰레드풀의 쓰레드 갯수를 CPU가 지원가능한 코어의 수로 제한
            _sessionFactory = sessionFactory;
            serverSocketChannel = AsynchronousServerSocketChannel.open(channelGroup);
            serverSocketChannel.bind(new InetSocketAddress(15000));
        }catch(Exception e){
          e.printStackTrace();
          //stopserver()
        }
        RegisterAccept();
    }

    void RegisterAccept() // 낚시대를 던짐
    {
        System.out.println("Listener RegisterAccept Enter");
        serverSocketChannel.accept(null ,
                new   CompletionHandler<AsynchronousSocketChannel, Void>() {
                    @Override
                    public void completed(AsynchronousSocketChannel socketChannel, Void attachment) {
                        OnAcceptCompleted(socketChannel);
                    }

                    @Override
                    public void failed(Throwable exc, Void attachment) {
                        //실패했을 때 코드
                        System.out.println("accept failed : " + exc);
                    }
                }  );
    }

    void OnAcceptCompleted(AsynchronousSocketChannel socketChannel){ // accept이 성공적으로 완료됐을 때 콜백함수
        System.out.println("Listener OnAcceptCompleted Enter");
        try{
            System.out.println("연결 수락 : " + socketChannel.getRemoteAddress());
            Session session = _sessionFactory.Build();
            session.Start(socketChannel);
            session.OnConnected(socketChannel.getRemoteAddress());
        }catch(Exception e){
            e.printStackTrace();
        }

        RegisterAccept();
    }

    @FunctionalInterface
    public interface SessionFactory {
        public Session Build();
    }
}
