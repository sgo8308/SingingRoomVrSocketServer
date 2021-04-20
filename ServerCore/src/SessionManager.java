import java.util.HashMap;
import java.util.List;

/*
* 모든 세션을 관리하는 클래스
* 모두에게 메시지를 보내야할 때 이 매니저를 참고한다.
*/

public class SessionManager {
    static SessionManager _session = new SessionManager();

    public static SessionManager GetInstance(){ return _session; }

    int _sessionId = 0;

    HashMap<Integer, ClientSession> _allSessions = new HashMap<>();
    HashMap<Integer, List<ClientSession>> _sessionsByroom = new HashMap<>();

    public synchronized ClientSession Generate()
    {
        int sessionId = ++_sessionId;

        ClientSession session = new ClientSession();
        session.SessionId = sessionId;
        _allSessions.put(sessionId, session);

        System.out.println("Connected : " + sessionId);

        return session;

    }

    public synchronized ClientSession Find(int id)
    {
        ClientSession session = _allSessions.get(id);
        return session;
    }

    public synchronized void Remove(ClientSession session)
    {
        _allSessions.remove(session.SessionId);
    }

}
