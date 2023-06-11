
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;


public class CMServerApp {
    private CMServerStub m_serverStub;
    private MyServerEventHandler m_eventHandler;


    public CMServerApp() {
        m_serverStub = new CMServerStub();
        m_eventHandler = new MyServerEventHandler(m_serverStub);

    }
    public CMServerStub getServerStub() {
        return m_serverStub;
    }
    public MyServerEventHandler getServerEventHandler()
    {
        return m_eventHandler;
    }

    private static void printFilesInServerDirectory(CMServerStub serverStub) {
        File directory = new File("./server-file-path");
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        System.out.println("File: " + file.getName());
                    } else if (file.isDirectory()) {
                        System.out.println("Directory: " + file.getName());
                    }
                }
            }
        }
    }

    public static void NextStep(CMServerStub serverStub) {
        String nextstep = null;
        System.out.println("다음 실행 명령어를 입력하세요");
        BufferedReader next = new BufferedReader(new InputStreamReader(System.in));
        try {
            nextstep = next.readLine();
            switch (nextstep) {
                case "get file":
                    printFilesInServerDirectory(serverStub);
                    NextStep(serverStub);
                    break;
                default:
                    NextStep(serverStub);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        CMServerApp server = new CMServerApp();
        CMServerStub serverStub = server.getServerStub();
        serverStub.setAppEventHandler(server.getServerEventHandler());
        serverStub.startCM();
        NextStep(serverStub);
        }

    }


