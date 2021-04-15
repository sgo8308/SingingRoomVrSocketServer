public class ServerProgram {
    static Listener _listener = new Listener();

    public static void main(String[] args) {
        Listener.SessionFactory sessionFactory = () -> SessionManager.GetInstance().Generate(); //세션의 하위 클래스 전달
        _listener.Init(sessionFactory);
        System.out.println("Listening...");
        while (true)
        {
            ;
        }
    }
}
