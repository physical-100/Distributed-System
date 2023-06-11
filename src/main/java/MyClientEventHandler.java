
import kr.ac.konkuk.ccslab.cm.event.*;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

import java.nio.file.StandardCopyOption;


public class MyClientEventHandler implements CMAppEventHandler {
    private CMClientStub m_clientStub;
    public int logicalClock;
    public MyClientEventHandler(CMClientStub clientStub) {
        m_clientStub = clientStub;
        logicalClock = 0;
    }

    @Override
    public void processEvent( CMEvent cme) {
        logicalClock++;
        switch (cme.getType()) {
            case CMInfo.CM_SESSION_EVENT:
                CprocessSessionEvent(cme);
                break;
            case CMInfo.CM_INTEREST_EVENT:   // chat
                CprocessInterestEvent(cme);
                break;
            case CMInfo.CM_DATA_EVENT: // data가 들어왔을때
                CprocessDataEvent(cme);
                break;
            case CMInfo.CM_FILE_EVENT:
                CprocessFileEvent(cme);
                break;
            case CMInfo.CM_DUMMY_EVENT:
                CprocessDummyEvent(cme);
                break;
            default:
                return;
        }
    }
    private void CprocessSessionEvent(CMEvent cme) {

        CMSessionEvent se = (CMSessionEvent) cme;
        switch (se.getID()) {
            case CMSessionEvent.LOGIN_ACK:
                if (se.isValidUser() == 0) {
                    System.err.println("This client fails authentication by the default server!");
                } else if (se.isValidUser() == -1) {
                    System.err.println("This client is already in the login-user list!");
                } else {
                    System.out.println("This client successfully logs in to the default server.");
                }
                break;
            case CMSessionEvent.SESSION_TALK:
                System.out.println("(" + se.getHandlerSession() + ")");
                System.out.println("<" + se.getUserName() + ">: " + se.getTalk());
                break;
            case CMSessionEvent.SESSION_ADD_USER: // 현재 서버에 접속중인 클라이언트들을 표시
                System.out.println("[" + se.getUserName() + "] 현재 로그인 중입니다.");
                break;

            default:
                return;
        }
    }
    private void CprocessDataEvent(CMEvent cme) {
        CMDataEvent de = (CMDataEvent) cme;
        switch (de.getID()) {
            case CMDataEvent.REMOVE_USER:// 클라이언트의 로그아웃을 표시
                System.out.println("[" + de.getUserName() + "] logout server\n");
                break;
            default:
                return;
        }
    }

    private void CprocessInterestEvent(CMEvent cme) {//chat
        CMInterestEvent ie = (CMInterestEvent) cme;
        System.out.println(ie.getID());
        switch (ie.getID()) {
            case CMInterestEvent.USER_TALK:
                System.out.println("(" + ie.getHandlerSession() + ", " + ie.getHandlerGroup() + ")");
                System.out.println("<" + ie.getUserName() + ">: " + ie.getTalk());
                break;

            default:
                return;
        }
    }
    private void CprocessDummyEvent(CMEvent cme) {
        CMDummyEvent due = (CMDummyEvent) cme;
        if(due.getDummyInfo().contains("server")) {
//            System.out.println("msg: " + due.getDummyInfo());
            String[] parts = due.getDummyInfo().split("\\s+");
            String filename = parts[0];
            Path clientFilePath = Paths.get("./client-file-path/" + filename);
            Path newDirectoryPath = Paths.get("./client-file-path/" + due.getReceiver());
            Path newFilePath = Paths.get("./client-file-path/" + due.getReceiver() + "/" + filename);

            // Create the new directory if it doesn't exist
            try {
                if (!Files.exists(newDirectoryPath)) {
                    Files.createDirectory(newDirectoryPath);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Failed to create the directory.");
            }

            // Move files from clientFilePath to newFilePath
            try {
                // Check if the file exists
                if (Files.exists(clientFilePath)) {
                    Files.move(clientFilePath, newFilePath, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    // Create the file
                    Files.createFile(clientFilePath);
                    Files.move(clientFilePath, newFilePath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Failed to share files.");
            }
            // 이건 현재 클라이언트 경로 내의 파일 리스트 출력
            Path directoryPath = Paths.get("./client-file-path/" + due.getReceiver());

//            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directoryPath)) {
//                for (Path filePath : directoryStream) {
//                    if (Files.isRegularFile(filePath)) {
//                        System.out.println(filePath.getFileName());
//                    }
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        } else if (due.getDummyInfo().contains("logicalclock_change")){
            String[] parts = due.getDummyInfo().split("\\s+");
            String getInt = parts[0];
            int severLogicalClock = Integer.parseInt(getInt);
            logicalClock=compareLogicalClocks(logicalClock,severLogicalClock);

        }
        else if (due.getDummyInfo().contains("삭제되었습니다.")) { //수정요청을 보냈을때 이미 삭제되고 없는 경우
            String[] parts = due.getDummyInfo().split("\\s+");
            String getInt = parts[0];
            int severLogicalClock = Integer.parseInt(getInt);
            logicalClock=compareLogicalClocks(logicalClock,severLogicalClock);
        }
        return;
    }

    private void CprocessFileEvent(CMEvent cme) {
        CMFileEvent fe = (CMFileEvent) cme;
        switch (fe.getID()) {
            case CMFileEvent.REPLY_PERMIT_PULL_FILE:
                if (fe.getReturnCode() == -1) {
                    System.err.print("[" + fe.getFileName() + "] does not exist in the owner!\n");
                } else if (fe.getReturnCode() == 0) {
                    System.err.print("[" + fe.getFileSender() + "] rejects to send file(" + fe.getFileName() + ").\n");
                } else
                    System.out.println("[" + fe.getFileSender() + "] send file(" + fe.getFileName() + ").\n");
                break;
            case CMFileEvent.END_FILE_TRANSFER_CHAN_ACK:
                System.out.println(fe.getFileName() + "파일 송신이 완료되었습니다.\n");
                break;
            case CMFileEvent.END_FILE_TRANSFER:
                System.out.println("["+fe.getFileName() + "] 수신이 완료 되었습니다.\n");
            case CMFileEvent.END_FILE_TRANSFER_ACK:
                if (fe.getReturnCode() == 1) {
                    System.out.print("["+fe.getFileName() + "] 송신이 완료 되었습니다.\n");
                } else if (fe.getReturnCode() == 0) {
                    System.err.print("[" + fe.getFileReceiver() + "] receive fail\n");
                    break;
                }
                return;
        }
    }
    private int compareLogicalClocks(int clientLogicalClock, int serverLogicalClock) {// logical clock 비교 해서 더  큰 값 보다 +1 해서 내놓음
        if (serverLogicalClock >= clientLogicalClock) {
            return serverLogicalClock+1;
        } else {
            return clientLogicalClock+1;
        }
    }
}


