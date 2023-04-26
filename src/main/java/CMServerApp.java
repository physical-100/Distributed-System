import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

public class CMServerApp {

    private CMServerStub m_serverStub;
    private CMServerEventHandler m_eventHandler;


    public CMServerApp() {
        m_serverStub = new CMServerStub();
        m_eventHandler = new CMServerEventHandler(m_serverStub);

    }
    public CMServerStub getServerStub() {
        return m_serverStub;
    }
    public CMServerEventHandler getServerEventHandler()
    {
        return m_eventHandler;
    }
//    public void setFilePath()
//    {
//        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//        System.out.println("====== set file path");
//        String strPath = null;
//        System.out.print("file path: ");
//        try {
//            strPath = br.readLine();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        m_serverStub.setTransferedFileHome(Paths.get(strPath));
//
//        System.out.println("======");
//    } //기본 디렉토리 다시 설정

//    public void requestFile()
//    {
//        boolean bReturn = false;
//        String strFileName = null;
//        String strFileOwner = null;
//        String strFileAppend = null;
//        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//        System.out.println("====== request a file");
//        try {
//            System.out.print("File name: ");
//            strFileName = br.readLine();
//            System.out.print("File owner(user name): ");
//            strFileOwner = br.readLine();
//            System.out.print("File append mode('y'(append);'n'(overwrite);''(empty for the default configuration): ");
//            strFileAppend = br.readLine();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        if(strFileAppend.isEmpty())
//            bReturn = m_serverStub.requestFile(strFileName, strFileOwner);
//        else if(strFileAppend.equals("y"))
//            bReturn = m_serverStub.requestFile(strFileName,  strFileOwner, CMInfo.FILE_APPEND);
//        else if(strFileAppend.equals("n"))
//            bReturn = m_serverStub.requestFile(strFileName,  strFileOwner, CMInfo.FILE_OVERWRITE);
//        else
//            System.err.println("wrong input for the file append mode!");
//
//        if(!bReturn)
//            System.err.println("Request file error! file("+strFileName+"), owner("+strFileOwner+").");
//
//        System.out.println("======");
//    } server의 requestfile

//    public void pushFile()
//    {
//        boolean bReturn = false;
//        String strFilePath = null;
//        String strReceiver = null;
//        String strFileAppend = null;
//        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//        System.out.println("====== push a file");
//
//        try {
//            System.out.print("File path name: ");
//            strFilePath = br.readLine();
//            System.out.print("File receiver (user name): ");
//            strReceiver = br.readLine();
//            System.out.print("File append mode('y'(append);'n'(overwrite);''(empty for the default configuration): ");
//            strFileAppend = br.readLine();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        if(strFileAppend.isEmpty())
//            bReturn = m_serverStub.pushFile(strFilePath, strReceiver);
//        else if(strFileAppend.equals("y"))
//            bReturn = m_serverStub.pushFile(strFilePath,  strReceiver, CMInfo.FILE_APPEND);
//        else if(strFileAppend.equals("n"))
//            bReturn = m_serverStub.pushFile(strFilePath,  strReceiver, CMInfo.FILE_OVERWRITE);
//        else
//            System.err.println("wrong input for the file append mode!");
//
//        if(!bReturn)
//            System.err.println("Push file error! file("+strFilePath+"), receiver("+strReceiver+")");
//
//        System.out.println("======");
//    }    서버의 pushfile

    public static void main(String[] args) {
        CMServerApp server = new CMServerApp();
        CMServerStub serverStub = server.getServerStub();
        serverStub.setAppEventHandler(server.getServerEventHandler());
        serverStub.startCM();

    }

}
