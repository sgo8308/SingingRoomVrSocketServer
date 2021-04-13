package ChatServerAndClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class NioServer {

    public static void main(String[] args) {
        // 방이 없는 버전
        // 연결된 클라이언트를 관리할 컬렉션
        Set<SocketChannel> allClient = new HashSet<>();

        try (ServerSocketChannel serverSocket = ServerSocketChannel.open()) {

            // 서비스 포트 설정 및 논블로킹 모드로 설정
            serverSocket.bind(new InetSocketAddress(15000));
            serverSocket.configureBlocking(false);

            // 채널 관리자(Selector) 생성 및 채널 등록
            Selector selector = Selector.open();
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("----------서버 접속 준비 완료----------");
            // 버퍼의 모니터 출력을 위한 출력 채널 생성

            // 입출력 시 사용할 바이트버퍼 생성
            ByteBuffer inputBuf = ByteBuffer.allocate(1024);
            ByteBuffer outputBuf = ByteBuffer.allocate(1024);

            // 클라이언트 접속 시작
            while (true) {

                selector.select(); // 이벤트 발생할 때까지 스레드 블로킹

                // 발생한 이벤트를 모두 Iterator에 담아줌
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

                // 발생한 이벤트들을 담은 Iterator의 이벤트를 하나씩 순서대로 처리함
                while (iterator.hasNext()) {

                    // 현재 순서의 처리할 이벤트를 임시 저장하고 Iterator에서 지워줌
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    // 연결 요청중인 클라이언트를 처리할 조건문 작성
                    if (key.isAcceptable()) {

                        // 연결 요청중인 이벤트이므로 해당 요청에 대한 소켓 채널을 생성해줌
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel clientSocket = server.accept();

                        // Selector의 관리를 받기 위해서 논블로킹 채널로 바꿔줌
                        clientSocket.configureBlocking(false);

                        // 연결된 클라이언트를 컬렉션에 추가
                        allClient.add(clientSocket);

                        // 아이디를 입력받기 위한 출력을 해당 채널에 해줌
                        clientSocket.write(ByteBuffer.wrap("아이디를 입력해주세요 : ".getBytes()));

                        // 아이디를 입력받을 차례이므로 읽기모드로 셀렉터에 등록해줌
                        clientSocket.register(selector, SelectionKey.OP_READ, new User());


                        // 읽기 이벤트(클라이언트 -> 서버)가 발생한 경우
                    } else if (key.isReadable()) {

                        // 현재 채널 정보를 가져옴 (attach된 사용자 정보도 가져옴)
                        SocketChannel readSocket = (SocketChannel) key.channel();
                        User user = (User) key.attachment();

                        // 채널에서 데이터를 읽어옴
                        try {
                            readSocket.read(inputBuf);

                            // 만약 클라이언트가 연결을 끊었다면 예외가 발생하므로 처리
                        } catch (Exception e) {
                            key.cancel(); // 현재 SelectionKey를 셀렉터 관리대상에서 삭제
                            allClient.remove(readSocket); // Set에서도 삭제

                            // 서버에 종료 메세지 출력
                            String end = user.getID() + "님의 연결이 종료되었습니다.\n";
                            System.out.print(end);

                            // 자신을 제외한 클라이언트에게 종료 메세지 출력
                            outputBuf.put(end.getBytes());
                            for(SocketChannel s : allClient) {
                                if(!readSocket.equals(s)) {
                                    outputBuf.flip();
                                    s.write(outputBuf);
                                }
                            }
                            outputBuf.clear();
                            continue;
                        }


                        // 현재 아이디가 없을 경우 아이디 등록
                        if (user.isID()) {
                            // 현재 inputBuf의 내용 중 개행문자를 제외하고 가져와서 ID로 넣어줌
                            inputBuf.limit(inputBuf.position());
                            inputBuf.position(0);
                            byte[] b = new byte[inputBuf.limit()];
                            inputBuf.get(b);
                            user.setID(new String(b));

                            // 서버에 출력
                            String enter = user.getID() + "님이 입장하셨습니다.\n";
                            System.out.print(enter);

                            outputBuf.put(enter.getBytes());

                            // 모든 클라이언트에게 메세지 출력
                            for(SocketChannel s : allClient) {

                                outputBuf.flip();
                                s.write(outputBuf);
                            }

                            inputBuf.clear();
                            outputBuf.clear();
                            continue;
                        }

                        // 읽어온 데이터와 아이디 정보를 결합해 출력한 버퍼 생성
                        inputBuf.flip();
                        outputBuf.put((user.getID() + " : ").getBytes());
                        outputBuf.put(inputBuf);
                        outputBuf.flip();

                        for(SocketChannel s : allClient) {
                            if (!readSocket.equals(s)) {

                                s.write(outputBuf);
                                outputBuf.flip();
                            }
                        }

                        inputBuf.clear();
                        outputBuf.clear();
                    }
                }
            }

        } catch (

                IOException e) {

            e.printStackTrace();
        }


        //방 있는 버전
        // 연결된 클라이언트를 관리할 컬렉션

//        Set<SocketChannel> allClient = new HashSet<>();
//        RoomManager roomManager = new RoomManager();
//        try (ServerSocketChannel serverSocket = ServerSocketChannel.open()) {
//
//            // 서비스 포트 설정 및 논블로킹 모드로 설정
//            serverSocket.bind(new InetSocketAddress(15000));
//            serverSocket.configureBlocking(false);
//
//            // 채널 관리자(Selector) 생성 및 채널 등록
//            Selector selector = Selector.open();
//            serverSocket.register(selector, SelectionKey.OP_ACCEPT);
//
//            System.out.println("----------서버 접속 준비 완료----------");
//            // 버퍼의 모니터 출력을 위한 출력 채널 생성
//
//            // 입출력 시 사용할 바이트버퍼 생성
//            ByteBuffer inputBuf = ByteBuffer.allocate(1024);
//            ByteBuffer outputBuf = ByteBuffer.allocate(1024);
//
//            // 클라이언트 접속 시작
//            while (true) {
//
//                selector.select(); // 이벤트 발생할 때까지 스레드 블로킹
//
//                // 발생한 이벤트를 모두 Iterator에 담아줌
//                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
//
//                // 발생한 이벤트들을 담은 Iterator의 이벤트를 하나씩 순서대로 처리함
//                while (iterator.hasNext()) {
//
//                    // 현재 순서의 처리할 이벤트를 임시 저장하고 Iterator에서 지워줌
//                    SelectionKey key = iterator.next();
//                    iterator.remove();
//
//                    // 연결 요청중인 클라이언트를 처리할 조건문 작성
//                    if (key.isAcceptable()) {
//
//                        // 연결 요청중인 이벤트이므로 해당 요청에 대한 소켓 채널을 생성해줌
//                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
//                        SocketChannel clientSocket = server.accept();
//
//                        // Selector의 관리를 받기 위해서 논블로킹 채널로 바꿔줌
//                        clientSocket.configureBlocking(false);
//
//                        // 연결된 클라이언트를 컬렉션에 추가
//                        allClient.add(clientSocket);
//
//                        // 아이디를 입력받기 위한 출력을 해당 채널에 해줌
//                        clientSocket.write(ByteBuffer.wrap("아이디를 입력해주세요 : ".getBytes()));
//
//                        // 아이디를 입력받을 차례이므로 읽기모드로 셀렉터에 등록해줌
//                        clientSocket.register(selector, SelectionKey.OP_READ, new User());
//
//
//                        // 읽기 이벤트(클라이언트 -> 서버)가 발생한 경우
//                    } else if (key.isReadable()) {
//
//                        // 현재 채널 정보를 가져옴 (attach된 사용자 정보도 가져옴)
//                        SocketChannel readSocket = (SocketChannel) key.channel();
//                        User user = (User) key.attachment();
//
//                        // 채널에서 데이터를 읽어옴
//                        try {
//                            readSocket.read(inputBuf);
//
//                            // 만약 클라이언트가 연결을 끊었다면 예외가 발생하므로 처리
//                        } catch (Exception e) {
//                            key.cancel(); // 현재 SelectionKey를 셀렉터 관리대상에서 삭제
//                            allClient.remove(readSocket); // Set에서도 삭제
//
//                            // 서버에 종료 메세지 출력
//                            String end = user.getID() + "님의 연결이 종료되었습니다.\n";
//                            System.out.print(end);
//
//                            // 자신을 제외한 클라이언트에게 종료 메세지 출력
//                            outputBuf.put(end.getBytes());
//                            for(SocketChannel s : allClient) {
//                                if(!readSocket.equals(s)) {
//                                    outputBuf.flip();
//                                    s.write(outputBuf);
//                                }
//                            }
//                            outputBuf.clear();
//                            continue;
//                        }
//
//
//                        // 현재 아이디가 없을 경우 아이디 등록
//                        if (user.isID()) {
//                            // 현재 inputBuf의 내용 중 개행문자를 제외하고 가져와서 ID로 넣어줌
//                            inputBuf.limit(inputBuf.position());
//                            inputBuf.position(0);
//                            byte[] b = new byte[inputBuf.limit()];
//                            inputBuf.get(b);
//                            user.setID(new String(b));
//
//                            outputBuf.clear();
//                            readSocket.register(selector, SelectionKey.OP_WRITE, user);
//                            continue;
//                        }
//
//                        Room room = null;
//
//                        //어느 방 갈지 응답왔으면 방 등록하고 방에 있는 사람들에게 공지
//                        if((user.getRoomId() == null && user.isNoticed)){
//                            inputBuf.limit(inputBuf.position());
//                            inputBuf.position(0);
//                            byte[] b = new byte[inputBuf.limit()];
//                            inputBuf.get(b);
//
//                            //방이 없었으면 roomManager Set에 넣어줌
//                            if(roomManager.getRoom(new String(b)) == null){
//                                room = new Room();
//                                room.setRoomId(new String(b));
//                                room.addUser(readSocket);
//                                roomManager.createRoom(room);
//                            }else{
//                                //방이 있으면 기존 방 업데이트
//                                room = roomManager.getRoom(new String(b));
//                                room.addUser(readSocket);
//                                roomManager.upDateRoom(room);
//                            }
//                            user.setRoom(new String(b));
//
//                            // 서버에 출력
//                            String enter = user.getID() + "님이" + user.getRoomId() +" 방에 입장하셨습니다.\n";
//                            System.out.print(enter);
//
//                            outputBuf.put(enter.getBytes());
//
//                            // 모든 클라이언트에게 메세지 출력
//                            for(SocketChannel s : room.getSockets()) {
//
//                                outputBuf.flip();
//                                s.write(outputBuf);
//                            }
//
//                            inputBuf.clear();
//                            outputBuf.clear();
//                            readSocket.register(selector, SelectionKey.OP_READ, user);
//                            continue;
//                        }
//
//                        // 읽어온 데이터와 아이디 정보를 결합해 출력한 버퍼 생성
//                        inputBuf.flip();
//                        outputBuf.put((user.getID() + " : ").getBytes());
//                        outputBuf.put(inputBuf);
//                        outputBuf.flip();
//
//                        room = roomManager.getRoom(user.getRoomId());
//
//                        for(SocketChannel s : room.getSockets()){
//                            if (!readSocket.equals(s)) {
//
//                                s.write(outputBuf);
//                                outputBuf.flip();
//                            }
//                        }
//
//                        inputBuf.clear();
//                        outputBuf.clear();
//                    }else if (key.isWritable()){
//                        SocketChannel readSocket = (SocketChannel) key.channel();
//                        User user = (User) key.attachment();
//                        //방이 없을 경우 방 등록하라고 메세지 보내기
//                        Room room = null;
//
//                        if((user.getRoomId() == null && !user.isNoticed)){
//                            readSocket.write(ByteBuffer.wrap("방 번호를 입력해주세요 : ".getBytes()));
//                            user.isNoticed = true;
//                            readSocket.register(selector, SelectionKey.OP_READ, user);
//                        }
//
//                    }
//                }
//            }
//
//        } catch (
//
//                IOException e) {
//
//            e.printStackTrace();
//        }
    }
}

class RoomManager{
    Set<Room> roomSet;

    RoomManager(){
         roomSet = new HashSet<Room>();
    }

    void createRoom(Room room){
        roomSet.add(room);
    }

    void upDateRoom(Room room){
        for(Room rm : roomSet){
            if(room.getId() == rm.getId()){
                roomSet.remove(rm);
            }
        }
        createRoom(room);
    }

    Room getRoom(String roomId){
        Room _room = null;
        if(roomSet != null){
            for(Room room : roomSet){
                if(room.getId() == roomId){
                    _room = room;
                }
            }
        }

        return _room;
    }
}

class Room{
    Set<SocketChannel> userSet;
    String roomId;

    Room(){
        userSet = new HashSet<SocketChannel>();
    }

    Set<SocketChannel> getSockets(){
        return userSet;
    }

    void addUser(SocketChannel socket){
        userSet.add(socket);
    }

    void setRoomId(String roomId){
        this.roomId = roomId;
    }

    String getId(){
        return this.roomId;
    }
}

// 접속한 사용자의 ID를 가진 클래스
class User {

    // 아직 아이디 입력이 안된 경우 true
    private boolean idCheck = true;
    private String id;
    private String roomId = null;
    public boolean isNoticed = false;

    // ID가 들어있는지 확인
    boolean isID() {

        return idCheck;
    }

    // ID를 입력받으면 false로 변경
    private void setCheck() {

        idCheck = false;
    }

    // ID 정보 반환
    String getID() {

        return id;
    }

    // ID 입력
    void setID(String id) {
        this.id = id;
        setCheck();
    }

    void setRoom(String roomId){
        this.roomId = roomId;
    }

    String getRoomId() {
        return this.roomId;
    }
}