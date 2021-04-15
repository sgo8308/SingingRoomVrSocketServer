import java.util.HashMap;

public class SessionManager {
    static SessionManager _session = new SessionManager();

    public static SessionManager GetInstance(){ return _session; }

    int _sessionId = 0;

    HashMap<Integer, ClientSession> _sessions = new HashMap<>();

    public synchronized ClientSession Generate()
    {
        int sessionId = ++_sessionId;

        ClientSession session = new ClientSession();
        session.SessionId = sessionId;
        _sessions.put(sessionId, session);

        System.out.println("Connected : " + sessionId);

        return session;

    }

    public synchronized ClientSession Find(int id)
    {
        ClientSession session = _sessions.get(id);
        return session;
    }

    public synchronized void Remove(ClientSession session)
    {
        _sessions.remove(session.SessionId);
    }

}
