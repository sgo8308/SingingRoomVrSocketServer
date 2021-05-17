import java.util.HashMap;
import java.util.List;

public class RoomManager {
    static RoomManager roomManager = new RoomManager();

    public static RoomManager GetInstance(){ return roomManager; }

    HashMap<Integer, RoomInfo> roomInfos = new HashMap<>();

}

class RoomInfo{
    public boolean isVideoPlaying;
    public List<String> reservedVideoIdList; // 일단 이 url을 raw로 할지 그냥 videoId로 할지 아직 모르겠음 일단 클라이언트부터 손보고
}