import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.*;
import java.nio.file.Files;
import java.util.Timer;
import java.util.TimerTask;

public class CMClientApp {
    private static String strUserName;
    private static Timer timer;
    private CMClientStub m_clientStub;
//    private  static  boolean first;
    private static CMServerStub m_serverStub;
    private  static MyClientEventHandler m_eventHandler;

    public CMClientApp() {
        m_clientStub = new CMClientStub();
        m_serverStub = new CMServerStub();
        m_eventHandler = new MyClientEventHandler(m_clientStub);
        strUserName=null;
    }

    public CMClientStub getClientStub() {
        return m_clientStub;
    }

    public MyClientEventHandler getClientEventHandler() {
        return m_eventHandler;
    }

    private static void login(CMClientStub clientStub) { // 로그인 함수
        String strPassword = null;
        boolean bRequestResult = false;
        Console console = System.console();
        System.out.print("user name: ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            strUserName = br.readLine();
            if (console == null) {
                System.out.print("password: ");
                strPassword = br.readLine();
            } else
                strPassword = new String(console.readPassword("password: "));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //로그인 수행
        bRequestResult = clientStub.loginCM(strUserName, strPassword);
        if (bRequestResult)
            System.out.println("successfully sent the login request.");
        else {
            System.err.println("failed the login request!");
            return;
        }
    }

    public static void request(CMClientStub clientStub) {// 파일 요청 함수

        String strFileName = null;
        String strFileOwner = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("====== request a file");
        try {
            System.out.print("File name: ");
            strFileName = br.readLine();
            System.out.print("File owner(server name): ");
            strFileOwner = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        clientStub.requestFile(strFileName, strFileOwner);
    }

    public static void uploadMultipleFiles(CMClientStub clientStub) {
        String[] strFiles = null;
        String strFileList = null;
        int nFileNum = -1;
        String strTarget = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("====== pull  one or multiple files");
        try {
            System.out.print("Input receiver name: ");
            strTarget = br.readLine();
            System.out.print("Number of files: ");
            nFileNum = Integer.parseInt(br.readLine());
            System.out.print("Input file names separated with space: ");
            strFileList = br.readLine();

        } catch (NumberFormatException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        strFileList.trim();
        strFiles = strFileList.split("\\s+");
        if (strFiles.length != nFileNum) {
            System.out.println("The number of files incorrect!");
            return;
        }
        for (int i = 0; i < nFileNum; i++) {
//            CMFileTransferManager.pushFile(strFiles[i], strTarget, clientStub.getCMInfo());
            clientStub.pushFile("./client-file-path/" + strFiles[i], strTarget);
            clientStub.chat("/SERVER","file_send "+m_eventHandler.logicalClock);
        }
        return;
    }// 여러개의 파일을 보냅니다.

    public static void NextStep(CMClientStub clientStub) {
        String nextstep = null;
        System.out.println("이벤트가 응답된 후 다음 실행 명령어를 입력하세요");
        BufferedReader next = new BufferedReader(new InputStreamReader(System.in));
        try {
            nextstep = next.readLine();
            switch (nextstep) {
                case "logout":
                    clientStub.logoutCM();
                    System.out.println("로그아웃 되었습니다.");
                    NextStep(clientStub);

                    break;

                case "disconnect":
                    clientStub.disconnectFromServer();
                    break;
                case "terminate":
                    clientStub.terminateCM();
                    return;

                case "login":
                    login(clientStub);
                    NextStep(clientStub);
                    break;
                case "request":
                    request(clientStub);
                    NextStep(clientStub);
                    break;
                case "create file":
                    Createfile(clientStub);
                    NextStep(clientStub);
                    break;
                case "get list":
                    System.out.print(getFilesInClientDirectory(clientStub));
                    NextStep(clientStub);
                    break;
                case "modify file":
                    ModifyFile(clientStub);
                    NextStep(clientStub);
                    break;
                    case "delete file":
                        DeleteFile(clientStub);
                        NextStep(clientStub);
                        break;
                    case "share":
                        ShareFile(clientStub);
                        NextStep(clientStub);
                        break;
                case "upload":
                    uploadMultipleFiles(clientStub);
                    NextStep(clientStub);
                    break;
                case "chat":// chat
                    String strTarget = null;
                    String strMessage = null;
                    System.out.println("====== chat");
                    System.out.print("target(/b, /s, /g, or /username): ");
                    BufferedReader msg = new BufferedReader(new InputStreamReader(System.in));
                    try {
                        strTarget = msg.readLine();
                        strTarget = strTarget.trim();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.print("message: ");
                    try {
                        strMessage = msg.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    clientStub.chat(strTarget, strMessage+" "+m_eventHandler.logicalClock);
                    NextStep(clientStub);
                    break;
                default:
                    NextStep(clientStub);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public static void ShareFile(CMClientStub clientStub) {
        String fileName = null;
        String userName = null;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("File name: ");
            fileName = br.readLine();

            System.out.print("User name: ");
            userName = br.readLine();
            if(!clientStub.getGroupMembers().isMember(userName)){
                System.out.print("User name: ");
                userName = br.readLine();
            }
            String message = fileName + " share_request_to " + userName+" "+ m_eventHandler.logicalClock;
                clientStub.chat("/SERVER", message);
            System.out.println("공유 요청을 보냈습니다.");
                //파일 이름에 _shared가 없다면 추가 있다면 바로 메세지
                // 파일 이름에 "_shared" 추가
                if(!(fileName.contains("_shared"))) {
                    String sharedFileName = fileName + "_shared";

                    // 클라이언트의 파일 이름 변경
                    String clientFilePath = "./client-file-path/" + strUserName + "/" + fileName;
                    String sharedClientFilePath = "./client-file-path/" + strUserName + "/" + sharedFileName;

                    File clientFile = new File(clientFilePath);
                    File sharedClientFile = new File(sharedClientFilePath);

                    if (clientFile.renameTo(sharedClientFile)) {
                        System.out.println("File name changed successfully.");
                    } else {
                        System.out.println("Failed to change file name.");
                    }
                }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void DeleteFile(CMClientStub clientStub) {
        List<String> clientFileList = getFilesInClientDirectory(clientStub);
        String fileName = null;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            boolean isValidFileName = false;

            while (!isValidFileName) { //그런 파일이 없다면 다시 파일 입력
                System.out.print("File name: ");
                fileName = br.readLine();

                if (clientFileList.contains(fileName)) {
                    isValidFileName = true;

                    if (fileName.contains("_shared")) {
                        String message = "모든 "+ fileName + " 파일 삭제바랍니다. "+m_eventHandler.logicalClock;
                        clientStub.chat("/SERVER", message);
                        // 서버에 공유 파일 모두 삭제 메세지 요청 요청후 서버의 모든 공유 파일이 사라지면 파일 동기화에 의해 클라이언트의 파일도 삭제된다.
                        try {
                            Thread.sleep(1500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else { // 비공유 파일의 경우 바로 삭제 후에 동기화에 의해 서버에서 삭제됨
                        File file = new File("./client-file-path/" + strUserName + "/" + fileName);
                        boolean isDeleted = file.delete();

                        if (isDeleted) {
                            System.out.println("File deleted successfully.");
                        } else {
                            System.err.println("Failed to delete the file.");
                        }
                    }
                } else {
                    System.err.println("Invalid file name. Please enter a valid file name.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void Createfile(CMClientStub clientStub) {
        String strFileName = null;
        String strFileContent = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("====== create a file");
        boolean isValidFileName = false;

        while (!isValidFileName) {
            try {
                System.out.print("File name: ");
                strFileName = br.readLine();

                // Check if file name has "_shared" appended
                if (strFileName.contains("_shared")) {
                    System.err.println("File name cannot contain '_shared'. Please enter a different file name.");
                } else {
                    isValidFileName = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            System.out.print("File content: ");
            strFileContent = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create the directory if it doesn't exist
        String directoryPath = "./client-file-path/"+strUserName;;
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            if (directory.mkdirs()) {

            } else {
                System.err.println("Failed to create directory: " + directoryPath);
                return;
            }
        }

        // Create the file locally
        try {
            FileWriter fileWriter = new FileWriter(directoryPath + "/" + strFileName);
            fileWriter.write(strFileContent);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Send the file to the server
        clientStub.pushFile(directoryPath + "/" + strFileName, "SERVER");
        clientStub.chat("/SERVER","file_send "+m_eventHandler.logicalClock);
    }
    private static void ModifyFile(CMClientStub clientStub) {
        String FileName= null;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("File name: ");
            FileName = br.readLine();
            System.out.print("Enter new content: ");
            String newContent = br.readLine();

            FileWriter fileWriter = new FileWriter("./client-file-path/"+strUserName+"/"+FileName);
            fileWriter.write(newContent);
            fileWriter.close();

            System.out.println("File modified successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static List<String> getFilesInClientDirectory(CMClientStub clientStub) {
        List<String> fileList = new ArrayList<>();
        File directory = new File("./client-file-path/"+strUserName);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        fileList.add(file.getName());
                    }
                }
            }
        }
        return fileList;
    }
    private static List<String> getFilesInServerDirectory(CMClientStub clientStub) {
        List<String> fileList = new ArrayList<>();
        File directory = new File("./server-file-path/"+strUserName);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        fileList.add(file.getName());
                    }
                }
            }
        }
        return fileList;
    }
    private  static void updateSync(CMClientStub clientStub) {
        // 목록 가져오기
        List<String> clientFileList = getFilesInClientDirectory(clientStub);
        List<String> serverFileList = getFilesInServerDirectory(clientStub);

        // 클라이언트에만 존재하는 파일을 두가지로 처리 1.서버로 보내기 2. 클라이언트에서 삭제
        for (String clientFile : clientFileList) {
            if (!serverFileList.contains(clientFile)) {
                if(clientFile.contains("_shared")){// _shared 태그를 달고 있는 파일이 서버에는 없고 클라이언트에만 있다면
                    // 다른 클라이언트에서 삭제 되어 서버의 모든 파일이 삭제 되었으므로 클라이언트에서 삭제. 이유(클라이언트가 로그아웃해 있을때 삭제 되었을 경우 세션에 들어왔을때 동기화 되게하기 위해
                    File file = new File("./client-file-path/"+strUserName+"/"+clientFile);
                    boolean isDeleted = file.delete();
                    if (isDeleted) {
                        System.out.println(clientFile + " File deleted successfully.");
                    }
                }

                else {
                    String filePath = "./client-file-path/" + strUserName + "/" + clientFile;
                    clientStub.pushFile(filePath, "SERVER");
                    clientStub.chat("/SERVER","file_send "+m_eventHandler.logicalClock);
                    try {
                        // Wait for the specified duration
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }
    private static void deleteSync(CMClientStub clientStub) {
        List<String> clientFileList = getFilesInClientDirectory(clientStub);
        List<String> serverFileList = getFilesInServerDirectory(clientStub);
        // 서버에만 존재하는 파일을 서버에 삭제 요청하기
        for (String serverFile : serverFileList) {
            if (!clientFileList.contains(serverFile)) {
                if(serverFile.contains("_shared")){
                    String message = "모든 "+serverFile + " 파일 삭제바랍니다. "+m_eventHandler.logicalClock;
                    clientStub.chat("/SERVER", message);// 서버에 공유 파일 모두 삭제 메세지 요청
                    try {// 일단 슬립  logical lock 으로 구현해야함
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    String message = serverFile + " deleted. "+m_eventHandler.logicalClock;
                    clientStub.chat("/SERVER", message);// 서버에 삭제 메세지 요청

                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    private static void compareSync(CMClientStub clientStub) {
        List<String> clientFileList = getFilesInClientDirectory(clientStub);
        // 파일 내용 비교 및 업데이트
        for (String fileName : clientFileList) {
            String clientFilePath = "./client-file-path/" + strUserName + "/" + fileName;
            String serverFilePath = "./server-file-path/" + strUserName + "/" + fileName;

            String clientFileContent = getFileContent(clientFilePath);
            String serverFileContent = getFileContent(serverFilePath);

            if (!clientFileContent.equals(serverFileContent)) {// 내용이 다른 경우
                if (fileName.contains("_shared")) {  // 공유된 파일이라면 서버 내의 모든 파일 업데이트
                    String message1 = fileName + " deleted. " + m_eventHandler.logicalClock;
                    clientStub.chat("/SERVER", message1);
                    clientStub.pushFile(clientFilePath, "SERVER");// 재전송
                    clientStub.chat("/SERVER", "file_send " + m_eventHandler.logicalClock);
                    try {// 일단 슬립  logical lock 으로 구현해야함
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    String message2 = fileName + " 모두 업데이트 " + m_eventHandler.logicalClock;
                    clientStub.chat("/SERVER", message2);
                } else {
                    // 내용이 다르다면 파일 삭제 후 재전송
                    String message = fileName + " deleted. "+m_eventHandler.logicalClock;
                    clientStub.chat("/SERVER", message);

                    clientStub.pushFile(clientFilePath, "SERVER");
                    clientStub.chat("/SERVER","file_send "+m_eventHandler.logicalClock);
                }
            }
        }
    }
    private static void synchronizeFilesWithServer(CMClientStub clientStub) { // 동기화 함수
        updateSync(clientStub);
        // 서버에만 존재하는 파일을 서버에 삭제 요청하기
        deleteSync(clientStub);

        compareSync(clientStub);
    }

//        List<String> clientFileList = getFilesInClientDirectory(clientStub);
//        List<String> serverFileList = getFilesInServerDirectory(clientStub);
//        // 파일 내용 비교 및 업데이트
//        for (String fileName : clientFileList) {
//            String clientFilePath = "./client-file-path/" + strUserName + "/" + fileName;
//            String serverFilePath = "./server-file-path/" + strUserName + "/" + fileName;
//
//            String clientFileContent = getFileContent(clientFilePath);
//            String serverFileContent = getFileContent(serverFilePath);
//
//            if (!clientFileContent.equals(serverFileContent)) {// 내용이 다른 경우
//                if (fileName.contains("_shared")) {  // 공유된 파일이라면 서버 내의 모든 파일 업데이트
//                    // 여기서부터 ack 메세지 보내서 받아서 하면 됨 서버 큐에 파일이름이 없다면 실행 가수
////                    if(first==true) {  // 처음에 동기화 가능 요청
////                        clientStub.chat("/SERVER", fileName + " 파일 동기화 요청");
////                        first = false; // 서버에 요청을 보내고 처음이 아님으로 바꿈
////                    }
////                    if(m_eventHandler.modify_possible= true) { // 공유된 파일이라면 ack를 받은 이후 수정 요청청
//                        String message1 = fileName + " deleted. " + m_eventHandler.logicalClock;
//                        clientStub.chat("/SERVER", message1);
//                        clientStub.pushFile(clientFilePath, "SERVER");// 재전송
//                        clientStub.chat("/SERVER", "file_send " + m_eventHandler.logicalClock);
//                        try {// 일단 슬립  logical lock 으로 구현해야함
//                            Thread.sleep(1500);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//
//                        String message2 = fileName + " 모두 업데이트 " + m_eventHandler.logicalClock;
//                        clientStub.chat("/SERVER", message2);
//                        // 모두 업데이트 메세지가 보내지면 서버의 파일을 모두 업데이트하고 클라이언트로 전송
////                        m_eventHandler.modify_possible= false;// 수정 이후 수정불가능하게 만들어 놓음 // 이게 바뀌는지 확인 해보자
////                        first=true;
////                    }
//                } else {
//                    // 내용이 다르다면 파일 삭제 후 재전송
//                    String message = fileName + " deleted. "+m_eventHandler.logicalClock;
//                    clientStub.chat("/SERVER", message);
//
//                    clientStub.pushFile(clientFilePath, "SERVER");
//                    clientStub.chat("/SERVER","file_send "+m_eventHandler.logicalClock);
//                }
//            }
//        }
//    }
    private static String getFileContent(String filePath) {  // 파일 수정을 위해서 파일을 읽어오는 함수
        try {
            byte[] encodedBytes = Files.readAllBytes(Paths.get(filePath));
            return new String(encodedBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
        public static void main (String[] args){
            CMClientApp client = new CMClientApp();
            CMServerStub serverStub = new CMServerApp().getServerStub();
            CMClientStub clientStub = client.getClientStub();
            MyClientEventHandler eventHandler = client.getClientEventHandler();
            boolean ret = false;
            // initialize
            clientStub.setAppEventHandler(eventHandler);
            ret = clientStub.startCM();
            if (ret)
                System.out.println("init success");
            else {
                System.err.println("init error!");
                return;
            }
            login(clientStub);
            // 5초마다     동기화 실행
            timer = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    synchronizeFilesWithServer(clientStub);
                }
            };
            //15초마다 실행
            timer.schedule(task, 5000, 15000);
            NextStep(clientStub);
        }
    }

