import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Executors;

public class Listener {
    AsynchronousChannelGroup channelGroup; // 같은 채널 그룹에 속한 소켓채널끼리 쓰레드의 자원을 공유 가능
    AsynchronousServerSocketChannel serverSocketChannel; // 소켓채널을 소켓이라고 생각하면 편함 . 소켓과 달리 inputStream outputStream이 채널 하나로 통합되어 있음.
    SessionFactory _sessionFactory; // 세션을 생성해주는 함수를 포함하는 functionalInterface

    public void Init(SessionFactory sessionFactory){
        System.out.println("Listener Init Enter");
        try{
            channelGroup = AsynchronousChannelGroup.withFixedThreadPool(
                    Runtime.getRuntime().availableProcessors(),
                    Executors.defaultThreadFactory()
            ); // 비동기 채널 그룹을 만들고 쓰레드풀의 쓰레드 갯수를 CPU가 지원가능한 코어의 수로 제한

            _sessionFactory = sessionFactory;
            serverSocketChannel = AsynchronousServerSocketChannel.open(channelGroup); // 소캣 채널 생성
            serverSocketChannel.bind(new InetSocketAddress(15001)); // 소켓 채널에 포트 할당. IP 같은 경우는 자동으로 할당되는 듯
        }catch(Exception e){
          e.printStackTrace();
          //stopserver()
        }
        RegisterAccept();
    }

    void RegisterAccept() // accept라는 낚시대를 던져놓는다고 생각하면 편함. 쓰레드풀에 있는 작업큐에 집어 넣으면 쓰레드풀의 쓰레드 중 하나가 큐에서 작업을 가져와서 실행시키고 accept가 완료되면 콜백함수를 통해 알려준다.
    {
        System.out.println("Listener RegisterAccept Enter");
        serverSocketChannel.accept(null ,
                new   CompletionHandler<AsynchronousSocketChannel, Void>() { // accept가 완료되면 실행되는 콜백함수.
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

    void OnAcceptCompleted(AsynchronousSocketChannel socketChannel){
        System.out.println("Listener OnAcceptCompleted Enter");
        try{
            System.out.println("연결 수락 : " + socketChannel.getRemoteAddress());
            Session session = _sessionFactory.Build(); // 세션팩토리를 통해 세션을 만들어준다.
            session.Start(socketChannel);
            session.OnConnected(socketChannel.getRemoteAddress());
        }catch(Exception e){
            e.printStackTrace();
        }

        RegisterAccept();
    }

    @FunctionalInterface
    public interface SessionFactory { // FunctionalInterface라고 해서 안에 메소드를 하나만 놓을 수 있고 이 인터페이스를 써서 함수 만들 때 람다함수로 간편하게 만들 수 있어서 씀
        public Session Build();
    }
}
