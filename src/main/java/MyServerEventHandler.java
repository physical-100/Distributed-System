import kr.ac.konkuk.ccslab.cm.event.*;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;


public class MyServerEventHandler implements CMAppEventHandler {
    private CMServerStub m_serverStub;
    private  int server_logicalClock ;

    public MyServerEventHandler(CMServerStub serverStub) {
        server_logicalClock=0;
        m_serverStub = serverStub;
    }

    @Override
    public void processEvent(CMEvent cme) {
        server_logicalClock++;
        switch (cme.getType()) {
            case CMInfo.CM_SESSION_EVENT:
                SprocessSessionEvent(cme);
                break;
            case CMInfo.CM_INTEREST_EVENT:
                SprocessInterestEvent(cme);
                break;

            case CMInfo.CM_FILE_EVENT:
                SprocessFileEvent(cme);
                break;
            default:
                return;
        }
    }

    private void SprocessSessionEvent(CMEvent cme) {
        CMSessionEvent se = (CMSessionEvent) cme;
        switch (se.getID()) {
            case CMSessionEvent.LOGIN:
                String userName = se.getUserName();
                System.out.println("[" + userName + "] login.");

                String directoryPath = "./server-file-path/" + userName;
                File directory = new File(directoryPath);

                if (!directory.exists()) {
                    if (directory.mkdirs()) {
                        // 디렉토리 만들기
                    } else {
                        System.out.println("Failed to create directory: " + directoryPath);
                    }
                }
                break;

            case CMSessionEvent.LOGOUT:
                System.out.println("[" + se.getUserName() + "] logout this server.");
                //로그 아웃시 서버에 존재하는 파일 삭제
                String logoutdirectoryPath = "./server-file-path/" + se.getUserName();
                File directory1 = new File(logoutdirectoryPath);

                if (directory1.isDirectory()) {// 로그아웃한다면 서버에  공유된 파일은 삭제
                    //로그인 한 상태에서만 업데이트가 가능하기하다는 가정으로 로그아웃시 삭제됨
                    File[] files = directory1.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile()) {
                                if (file.toString().contains("_shared"))
                                    file.delete(); // 파일 삭제
                            }
                        }
                    }
                }
                break;
            default:
                return;
        }

    }

    private void SprocessInterestEvent(CMEvent cme) {
        // chat메세지가 올때마다 logical clock 값을 받아서 업데이트
        CMInterestEvent ie = (CMInterestEvent) cme;
        switch (ie.getID()) {
            case CMInterestEvent.USER_TALK:
                System.out.println("<" + ie.getUserName() + ">: " + ie.getTalk());
                getLogicalClocks(ie.getTalk());
                if (ie.getTalk().contains("모든")) {   // 공유된 파일 삭제 시  서버의 모든 파일 삭제
                    String[] parts = ie.getTalk().split("\\s+");
                    String filename = parts[1];
                    for (String DirName : getDirectoryPathsWithFile("./server-file-path", filename)) {
                        String extractedString = DirName.substring(DirName.lastIndexOf("/") + 1); // 서버에 파일 이름을 가진 디렉토르 추출
                        File file = new File("./server-file-path/" + extractedString + "/" + filename);
                        boolean isDeleted = file.delete();
                        if (isDeleted) {
                            System.out.println(extractedString+"/"+filename + " File deleted successfully.");
                        } else {
                            System.err.println("Failed to delete the file.");
                        }
                          // 서버에서 삭제 이후 클라이언트에 있는 파일은 동기화에 의해 삭제됨
                    }
                } else if (ie.getTalk().contains("모두 업데이트")) {// 파일 업데이트 후 클라이언트에게 전송
                        String[] parts = ie.getTalk().split("\\s+");
                        String filename = parts[0];
                        if (!getDirectoryPathsWithFile("./server-file-path", filename).isEmpty()) {
                            for (String DirName : getDirectoryPathsWithFile("./server-file-path", filename)) {// 서버에 파일 이름을 가진 디렉토리 추출
                                String extractedString = DirName.substring(DirName.lastIndexOf("/") + 1);
                                System.out.println(extractedString);
                                if (!extractedString.equals(ie.getUserName())) {
                                    String sourcePath = "./server-file-path/" + ie.getUserName() + "/" + filename;
                                    String destinationPath = "./server-file-path/" + extractedString + "/" + filename;

                                    Path source = Paths.get(sourcePath);
                                    Path destination = Paths.get(destinationPath);

                                    try {
                                        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                                        System.out.println("File move successfully.");
                                        send_dummyevent(server_logicalClock + " lock sync",extractedString);
                                        m_serverStub.pushFile("./server-file-path/" + extractedString + "/" + filename, extractedString);
                                        send_dummyevent(server_logicalClock + " logicalclock_change", extractedString);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        System.out.println("Failed to move the file.");
                                    }
                                }
                            }
                        }
                        else {
                            System.out.println("파일이 수정되기전 모두 삭제되었습니다.");
                            send_dummyevent(server_logicalClock+" 파일이_수정되기전_모두_삭제되었습니다.", ie.getUserName());
                        }
                } else if (ie.getTalk().contains("deleted")) {// 삭제 채팅을 받았을 때 처리
                    String[] parts = ie.getTalk().split("\\s+"); // Split the string by whitespace
                    String filename = parts[0];
                    File file = new File("./server-file-path/" + ie.getUserName() + "/" + filename);
                    boolean isDeleted = file.delete();
                    if (isDeleted) {
                        System.out.println(filename + " File deleted successfully.");

                    } else {
                        System.err.println("Failed to delete the file.");
                    }
                } else if (ie.getTalk().contains("share_request_to")) {
                    String[] parts = ie.getTalk().split("\\s+"); // Split the string by whitespace
                    String filename = parts[0];
                    String username = parts[2];

                    if ((filename.contains("_shared"))) { //이미 공유되었던 이름이라면 변경하지 않고 클라이언트로 전송해줌


                        m_serverStub.pushFile("./server-file-path/" + ie.getUserName() + "/" + filename, username);
                        send_dummyevent(server_logicalClock+ " logicalclock_change",username);
                        // push와 동시에 username 디렉토리 생성 후 파일 복사 해옴
                        String destinationPath = "./server-file-path/" + username + "/" + filename;
                        Path source = Paths.get("./server-file-path/" + ie.getUserName() + "/" + filename);
                        Path destination = Paths.get(destinationPath);

                        try {
                            // 대상 파일이 존재하지 않으면 생성
                            if (!Files.exists(destination)) {
                                Files.createFile(destination);
                            }

                            // 파일 복사능
                            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                            System.out.println( username +" 에 파일이 성공적으로 생성되었습니다.");
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.println("파일 생성에 실패하였습니다.");
                        }
                    }
                    else {
                        // 서버에 저장되어있던 클라이언트의 이름 변경
                        // 이름으로 비교를 하면 이름이 같은데 공유되지 않은 파일도 동기화 되는것을 막아주기 위해 _shared를 붙인 파일이름으로 바꾸어준다.
                        String sharedFileName = filename + "_shared";
                        String ServerFilePath = "./server-file-path/" + ie.getUserName() + "/" + filename;
                        String sharedServerFilePath = "./server-file-path/" + ie.getUserName() + "/" + sharedFileName;

                        File clientFile = new File(ServerFilePath);
                        File sharedClientFile = new File(sharedServerFilePath);

                        if (clientFile.renameTo(sharedClientFile)) {
                            System.out.println("File name changed successfully.");
                            m_serverStub.pushFile(sharedServerFilePath, username);
                            send_dummyevent(server_logicalClock+ " logicalclock_change",username);

                        } else {
                            System.out.println("Failed to change file name.");
                        }
                        //서버에도 파일 복사해서 생성
                        String destinationPath = "./server-file-path/" + username + "/" + sharedFileName;
                        Path source = Paths.get("./server-file-path/" + ie.getUserName() + "/" + sharedFileName);
                        Path destination = Paths.get(destinationPath);

                        try {
                            // 대상 파일이 존재하지 않으면 생성
                            if (!Files.exists(destination)) {
                                Files.createFile(destination);
                            }

                            // 파일 복사
                            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("파일이 성공적으로 생성되었습니다.");
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.println("파일 생성에 실패하였습니다.");
                        }

                    }
                } else if (ie.getTalk().contains("file_send")) {
                    send_dummyevent(server_logicalClock+ " logicalclock_change", ie.getUserName());

                }else if (ie.getTalk().contains("파일 동기화 요청")) {
                    String[] parts = ie.getTalk().split("\\s+");
                    String filename = parts[0];
                        send_dummyevent(server_logicalClock + "동기화 가능", ie.getUserName());
                }
                break;
            default:
                return;
        }
    }

    private void SprocessFileEvent(CMEvent cme) {
        CMFileEvent fe = (CMFileEvent) cme;

        switch (fe.getID()) {
            case CMFileEvent.REQUEST_PERMIT_PULL_FILE:
                System.out.println("[" + fe.getFileReceiver() + "] requests file(" + fe.getFileName() + ").\n");
            case CMFileEvent.REPLY_PERMIT_PULL_FILE:
                if (fe.getReturnCode() == -1) {
                    System.err.print("[" + fe.getFileName() + "] does not exist in the owner!\n");
                } else if (fe.getReturnCode() == 0) {
                    System.err.print("[" + fe.getFileSender() + "] rejects to send file(" + fe.getFileName() + ").\n");
                } else
                    System.out.println("[" + fe.getFileSender() + "] send file(" + fe.getFileName() + ").\n");
                break;
            case CMFileEvent.END_FILE_TRANSFER:
                System.out.println(fe.getFileName() + " 파일 수신이 완료되었습니다.\n");
                break;
            case CMFileEvent.END_FILE_TRANSFER_ACK:
                if (fe.getReturnCode() == 1) {
                    System.out.print(" transfer success\n");
                    if (fe.getFileSender().equals("SERVER")) {// 전송이 끝난후 공유된 파일일때 클라이언트로 메세지 보냄
                        if (fe.getFileName().contains("_shared")) {
                            send_dummyevent(fe.getFileName()+" server file send",fe.getFileReceiver());
                        }
                    }
                } else if (fe.getReturnCode() == 0) {
                    System.err.print("[" + fe.getFileReceiver() + "] receive fail\n");
                    break;
                }

                return;
        }
    }
    private  void send_dummyevent(String msg,String reciever){
                        CMDummyEvent due = new CMDummyEvent();
                        due.setDummyInfo(msg);
                        m_serverStub.send(due, reciever);
                        due = null;
    }
    private void getLogicalClocks(String getString) {
        String[] msg = getString.split("\\s+");
        String lastElement = msg[msg.length - 1];
        int clientLogicalClock = Integer.parseInt(lastElement);
        //서버 로지컬 클락 세팅
        server_logicalClock=compareLogicalClocks(server_logicalClock,clientLogicalClock);
    }
    private int compareLogicalClocks(int serverLogicalClock, int clientLogicalClock) {// logical clock 비교 해서 더  큰 값 보다 +1 해서 내놓음
        if (serverLogicalClock >= clientLogicalClock) {
            return serverLogicalClock+1;
        } else {
            return clientLogicalClock+1;
        }
    }
    public static List<String> getDirectoryPathsWithFile(String directoryPath, String filename) {
        // filename을 가진 디렉토리 모두 반환
        List<String> directoryPaths = new ArrayList<>();
        File directory = new File(directoryPath);
        searchDirectory(directory, filename, directoryPaths);
        return directoryPaths;
    }

    private static void searchDirectory(File directory, String filename, List<String> directoryPaths) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                boolean hasFile = false;
                for (File file : files) {
                    if (file.isFile() && file.getName().equals(filename)) {
                        hasFile = true;
                        break;
                    } else if (file.isDirectory()) {
                        searchDirectory(file, filename, directoryPaths);
                    }
                }
                if (hasFile) {
                    directoryPaths.add(directory.getAbsolutePath());
                }
            }
        }
    }
}








