import kr.ac.konkuk.ccslab.cm.event.*;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.*;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class MyWinClientEventHandler implements CMAppEventHandler{
    //private JTextArea m_outTextArea;
    private MyWinClient m_client;
    private CMClientStub m_clientStub;
    public int logicalClock;


    public MyWinClientEventHandler(CMClientStub clientStub, MyWinClient client)
    {
        m_client = client;
        m_clientStub = clientStub;
        logicalClock = 0;
        
    }@Override
    public void processEvent(CMEvent cme) {
        logicalClock++;
        switch(cme.getType())
        {
            case CMInfo.CM_SESSION_EVENT:
                CprocessSessionEvent(cme);
                break;
            case CMInfo.CM_INTEREST_EVENT:
                CprocessInterestEvent(cme);
                break;
            case CMInfo.CM_DATA_EVENT:
                CprocessDataEvent(cme);
                break;
            case CMInfo.CM_DUMMY_EVENT:
                try {
                    CprocessDummyEvent(cme);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case CMInfo.CM_FILE_EVENT:
                CprocessFileEvent(cme);
                break;
            default:
                return;
        }
    }

    private void CprocessSessionEvent(CMEvent cme) {

        CMSessionEvent se = (CMSessionEvent) cme;
        switch (se.getID()) {
            case CMSessionEvent.LOGIN:
                printMessage(se.getUserName()+"로그인 했음");
            case CMSessionEvent.LOGIN_ACK:
                if (se.isValidUser() == 0) {
                    printledMessage("This client fails authentication by the default server!","bold");
                } else if (se.isValidUser() == -1) {
                    printledMessage("This client is already in the login-user list!","bold");
                } else {
                    printMessage("This client successfully logs in to the default server.\n");
                }
                break;
            case CMSessionEvent.SESSION_TALK:
                printMessage("(" + se.getHandlerSession() + ")");
                printMessage("<" + se.getUserName() + ">: " + se.getTalk());
                break;
            case CMSessionEvent.SESSION_ADD_USER: // 현재 서버에 접속중인 클라이언트들을 표시
                printMessage("[" + se.getUserName() + "] 현재 로그인 중입니다.\n");
                break;

            default:
                return;
        }
    }
    private void CprocessInterestEvent(CMEvent cme) {//chat
        CMInterestEvent ie = (CMInterestEvent) cme;
        switch (ie.getID()) {
            case CMInterestEvent.USER_TALK:
                printMessage("(" + ie.getHandlerSession() + ", " + ie.getHandlerGroup() + ")");
                printMessage("<" + ie.getUserName() + ">: " + ie.getTalk());
                break;

            default:
                return;
        }
    }


    private void CprocessDummyEvent(CMEvent cme) throws IOException {
        CMDummyEvent due = (CMDummyEvent) cme;
        if(due.getDummyInfo().contains("server")) {
//            printMessage("msg: " + due.getDummyInfo());
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
                printledMessage("Failed to create the directory.","bold");
            }
            // Move files from clientFilePath to newFilePath
            try {
                // Check if the file exists
                if (Files.exists(clientFilePath)) {
                    // 공유된 클라이언트가 3개 이상일때 서버에서 클라이언트마다 보내도 하나의 경로로 도달하게 된다.
                    // 그래서 파일 전송완료 이후 클라이언트 내에서 파일을 해당 디렉토리로 옮긴다.(move 말고, copy 사용 ) move 사용시 하나의 클라이언트 수정시 파일이 사라져 빈값을 가진 파일이 만들어짐
                    Files.copy(clientFilePath, newFilePath, StandardCopyOption.REPLACE_EXISTING);
//                    printMessage("파일이 수정되었습니다.");
                } else {
                    // Create the file
                    Files.createFile(clientFilePath);
                    System.out.println("파일을 만들었습니다.");
                    Files.copy(clientFilePath, newFilePath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                e.printStackTrace();
                printledMessage("Failed to share files.\n","bold");
            }
//            System.out.println(ack);
            m_client.ack = 0;
        } else if (due.getDummyInfo().contains("logicalclock_change")){
            String[] parts = due.getDummyInfo().split("\\s+");
            String getInt = parts[0];
            int severLogicalClock = Integer.parseInt(getInt);
            logicalClock=compareLogicalClocks(logicalClock,severLogicalClock);
        }
        else if (due.getDummyInfo().contains("삭제되었습니다.")) {  //수정요청을 보냈을때 이미 삭제되고 없는 경우
            String[] parts = due.getDummyInfo().split("\\s+");
            String getInt = parts[0];
            int severLogicalClock = Integer.parseInt(getInt);
            logicalClock=compareLogicalClocks(logicalClock,severLogicalClock);
        }
        else if (due.getDummyInfo().contains("파일 수정 불가능")) {  //수정 요청에 대한 ack를 받지 못한 경우
            String[] parts = due.getDummyInfo().split("\\s+");
            String getInt = parts[0];
            int severLogicalClock = Integer.parseInt(getInt);
            logicalClock=compareLogicalClocks(logicalClock,severLogicalClock);
            m_client.strFileContent=null;
        }else if (due.getDummyInfo().contains("lock sync")) { // 파일 전송완료후 ack가  변경될때까지 lock 한다.
            System.out.println(due.getDummyInfo());
                m_client.ack =1;
        }
        else if (due.getDummyInfo().contains("파일 수정 가능")) {  //수정요청을 보냈을때 이미 삭제되고 없는 경우
            String[] parts = due.getDummyInfo().split("\\s+");
            String getInt = parts[0];
            String filename = parts[1];
            int severLogicalClock = Integer.parseInt(getInt);
            logicalClock=compareLogicalClocks(logicalClock,severLogicalClock);

            FileWriter fileWriter = new FileWriter("./client-file-path/"+due.getReceiver()+"/"+filename);
            fileWriter.write(m_client.strFileContent);
            fileWriter.close();
            printMessage(filename+"File modified successfully.\n");
            m_client.strFileContent=null;
        }
        return;
    }
    private void CprocessDataEvent(CMEvent cme) {
        CMDataEvent de = (CMDataEvent) cme;
        switch (de.getID()) {
            case CMDataEvent.REMOVE_USER:// 클라이언트의 로그아웃을 표시
                printMessage("[" + de.getUserName() + "] logout server\n");
                break;
            default:
                return;
        }
    }
    private void CprocessFileEvent(CMEvent cme) {
        CMFileEvent fe = (CMFileEvent) cme;
        switch (fe.getID()) {
            case CMFileEvent.REPLY_PERMIT_PULL_FILE:
                if (fe.getReturnCode() == -1) {
                    printledMessage("[" + fe.getFileName() + "] does not exist in the owner!\n","bold");
                } else if (fe.getReturnCode() == 0) {
                    printledMessage("[" + fe.getFileSender() + "] rejects to send file(" + fe.getFileName() + ").\n","bold");
                } else
                    printMessage("[" + fe.getFileSender() + "] send file(" + fe.getFileName() + ").\n");
                break;
            case CMFileEvent.END_FILE_TRANSFER_CHAN_ACK:
                printMessage(fe.getFileName() + "파일 송신이 완료되었습니다.\n");
                break;
            case CMFileEvent.END_FILE_TRANSFER:
                printMessage("["+fe.getFileName() + "] 수신이 완료 되었습니다.\n");
            case CMFileEvent.END_FILE_TRANSFER_ACK:
                if (fe.getReturnCode() == 1) {
                    printMessage("["+fe.getFileName() + "] 송신이 완료 되었습니다.\n");
                } else if (fe.getReturnCode() == 0) {
                    printledMessage("[" + fe.getFileReceiver() + "] receive fail\n","bold");
                    break;
                }
                return;
        }
    }

    private void printledMessage(String strText,String style){
        m_client.printStyledMessage(strText,style);
    }
    private void printMessage(String strText)
    {
        m_client.printMessage(strText);

        return;
    }


   private int compareLogicalClocks(int clientLogicalClock, int serverLogicalClock) {// logical clock 비교 해서 더  큰 값 보다 +1 해서 내놓음
       if (serverLogicalClock >= clientLogicalClock) {
           return serverLogicalClock+1;
       } else {
           return clientLogicalClock+1;
       }
   }
}