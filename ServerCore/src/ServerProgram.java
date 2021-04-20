public class ServerProgram {
    static Listener _listener = new Listener();

    public static void main(String[] args) {
        Listener.SessionFactory sessionFactory = () -> SessionManager.GetInstance().Generate(); //sessionFactory의 Bulid메소드를 이런식으로 익명함수로 정의해줌.
        _listener.Init(sessionFactory);
        System.out.println("Listening...");
        while (true)
        {
            ;
        }
    }
}
