import kr.ac.konkuk.ccslab.cm.manager.CMFileTransferManager;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;

public class CMClientApp {
    private CMClientStub m_clientStub;
    private CMServerStub m_serverStub;
    private CMClientEventHandler m_eventHandler;

    public CMClientApp() {
        m_clientStub = new CMClientStub();
        m_serverStub = new CMServerStub();
        m_eventHandler = new CMClientEventHandler(m_clientStub);
    }

    public CMClientStub getClientStub() {
        return m_clientStub;
    }

    public CMClientEventHandler getClientEventHandler() {
        return m_eventHandler;
    }
    private static void login(CMClientStub clientStub){ // 로그인 함수
        String strUserName = null;
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
    public static void request(CMClientStub clientStub){// 파일 요청 함수

        String strFileName = null;
        String strFileOwner = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); System.out.println("====== request a file");
        try {
            System.out.print("File name: ");
            strFileName = br.readLine(); System.out.print("File owner(server name): "); strFileOwner = br.readLine();
        } catch (IOException e) { e.printStackTrace();
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
            clientStub.pushFile(strFiles[i],strTarget);
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
                    clientStub.chat(strTarget, strMessage);
                    NextStep(clientStub);
                    break;
                default:
                    NextStep(clientStub);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public static void main(String[] args) {
        CMClientApp client = new CMClientApp();
        CMServerStub serverStub = new CMServerApp().getServerStub();
        CMClientStub clientStub = client.getClientStub();
        CMClientEventHandler eventHandler = client.getClientEventHandler();
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

        NextStep(clientStub);
    }
}
