import kr.ac.konkuk.ccslab.cm.event.*;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.*;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;

import java.io.IOException;
import java.io.PrintWriter;
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
                CprocessDummyEvent(cme);
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
                    printMessage("This client fails authentication by the default server!");
                } else if (se.isValidUser() == -1) {
                    printMessage("This client is already in the login-user list!");
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


    private void CprocessDummyEvent(CMEvent cme) {
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
                printMessage("Failed to create the directory.");
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
                printMessage("Failed to share files.");
            }
            // 이건 현재 클라이언트 경로 내의 파일 리스트 출력
            Path directoryPath = Paths.get("./client-file-path/" + due.getReceiver());

//            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directoryPath)) {
//                for (Path filePath : directoryStream) {
//                    if (Files.isRegularFile(filePath)) {
//                        printMessage(filePath.getFileName());
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
                    printMessage("[" + fe.getFileName() + "] does not exist in the owner!\n");
                } else if (fe.getReturnCode() == 0) {
                    printMessage("[" + fe.getFileSender() + "] rejects to send file(" + fe.getFileName() + ").\n");
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
                    printMessage("[" + fe.getFileReceiver() + "] receive fail\n");
                    break;
                }
                return;
        }
    }
    

//    private void processCONTENT_DOWNLOAD_END(CMSNSEvent se)
//    {
//        CMConfigurationInfo confInfo = m_clientStub.getCMInfo().getConfigurationInfo();
//        int nAttachScheme = confInfo.getAttachDownloadScheme();
//        CMSNSInfo snsInfo = m_clientStub.getCMInfo().getSNSInfo();
//        CMSNSContentList contentList = snsInfo.getSNSContentList();
//        Iterator<CMSNSContent> iter = null;
//        boolean bShowLink = true;
//
//        //printMessage("# downloaded contents: "+se.getNumContents());
//        printMessage("# downloaded contents: "+se.getNumContents()+"\n");
//        //printMessage("# contents to be printed: "+contentList.getSNSContentNum());
//        printMessage("# contents to be printed: "+contentList.getSNSContentNum()+"\n");
//
//        // writes info to the file
////        int nRealDelay = (int)(System.currentTimeMillis()-m_lStartTime);
////        int nAccessDelay = nRealDelay + m_nEstDelaySum;
////        printMessage("Real download delay: "+nRealDelay+" ms\n");
////        if(m_pw != null)   // if multiple downloading is requested,
////        {
////            m_pw.format("%10d %10d%n", nAccessDelay, se.getNumContents());
////            m_pw.flush();
////        }
//
//        // print out SNS content that is downloaded
//        iter = contentList.getContentList().iterator();
//        while(iter.hasNext())
//        {
//            CMSNSContent cont = iter.next();
//            //printMessage("--------------------------------------");
//            printMessage("-----------------------------------------------------------\n");
//            //printMessage("ID("+cont.getContentID()+"), Date("+cont.getDate()+"), Writer("
//            //      +cont.getWriterName()+"), File("+cont.getAttachedFileName()+")");
//            printMessage("ID("+cont.getContentID()+"), Date("+cont.getDate()+"), Writer("
//                    +cont.getWriterName()+"), #attachment("+cont.getNumAttachedFiles()
//                    +"), replyID("+cont.getReplyOf()+"), lod("+cont.getLevelOfDisclosure()+")\n");
//            //printMessage("Message: "+cont.getMessage());
//            printMessage("Message: "+cont.getMessage()+"\n");
//            if(cont.getNumAttachedFiles() > 0)
//            {
//                ArrayList<String> fNameList = cont.getFileNameList();
//                for(int i = 0; i < fNameList.size(); i++)
//                {
//                    String fPath = confInfo.getTransferedFileHome().toString() + File.separator + fNameList.get(i);
//                    File file = new File(fPath);
//
//                    // display images (possibly thumbnails)
//                    if(nAttachScheme == CMInfo.SNS_ATTACH_FULL || nAttachScheme == CMInfo.SNS_ATTACH_PARTIAL)
//                    {
//                        if(CMUtil.isImageFile(fPath))
//                        {
//                            printImage(fPath);
//                        }
//                    }
//                    else if(nAttachScheme == CMInfo.SNS_ATTACH_PREFETCH)
//                    {
//                        // skip visualization of the prefetched original image
//                        if(CMUtil.isImageFile(fPath) && fNameList.get(i).contains("-thumbnail."))
//                        {
//                            printImage(fPath);
//                        }
//                    }
//
//                    // determine whether a link will be showed or not
//                    bShowLink = true;
//
//                    if(nAttachScheme == CMInfo.SNS_ATTACH_PARTIAL)
//                    {
//                        // if the file is a thumbnail file
//                        String fName = fNameList.get(i);
//                        if(fName.contains("-thumbnail."))
//                        {
//                            int index = fName.lastIndexOf("-thumbnail.");
//                            String ext = fName.substring(index+"-thumbnail".length(), fName.length());
//                            fName = fName.substring(0, index)+ext;
//                            fPath = confInfo.getTransferedFileHome().toString()+File.separator+fName;
//                            file = new File(fPath);
//                        }
//                    }
//                    else if(nAttachScheme == CMInfo.SNS_ATTACH_PREFETCH)
//                    {
//                        if(CMUtil.isImageFile(fPath))
//                        {
//                            String fName = fNameList.get(i);
//                            if(fName.contains("-thumbnail."))
//                            {
//                                int index = fName.lastIndexOf("-thumbnail.");
//                                String ext = fName.substring(index+"-thumbnail".length(), fName.length());
//                                fName = fName.substring(0, index)+ext;
//                                fPath = confInfo.getTransferedFileHome().toString()+File.separator+fName;
//                                file = new File(fPath);
//                            }
//                            else
//                                bShowLink = false;
//                        }
//                    }
//
//                    // print the link to a original attachment file
//                    if(bShowLink)
//                    {
//                        if(file.exists())
//                        {
//                            printMessage("attachment: ");
//                        }
//                        else
//                        {
//                            printMessage("attachment not downloaded: ");
//                        }
//                        printFilePath(fPath);
//                    }
//                }   // for
//            }   // if
//        }   // while
//        //printMessage("sum of estimated download delay: "+m_nEstDelaySum +" ms\n");
//
//        // continue simulation until m_nSimNum = 0
////        if( --m_nSimNum > 0 )
////        {
////            // repeat the request of SNS content downloading
////            m_lStartTime = System.currentTimeMillis();
////            int nContentOffset = 0;
////            String strUserName = m_clientStub.getMyself().getName();
////            String strWriterName = "";
////
////            m_clientStub.requestSNSContent(strWriterName, nContentOffset);
////            if(CMInfo._CM_DEBUG)
////            {
////                //printMessage("["+strUserName+"] requests content with offset("+nContentOffset+").");
////                printMessage("["+strUserName+"] requests content of writer["+strWriterName+"] with offset("
////                        +nContentOffset+").\n");
////            }
////        }
//        else
//        {
//            if(m_fos != null){
//                try {
//                    m_fos.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            if(m_pw != null){
//                m_pw.close();
//            }
//        }
//
//        return;
//    }
    
    
    private void printMessage(String strText)
    {
      /*
      m_outTextArea.append(strText);
      m_outTextArea.setCaretPosition(m_outTextArea.getDocument().getLength());
      */
      /*
      StyledDocument doc = m_outTextPane.getStyledDocument();
      try {
         doc.insertString(doc.getLength(), strText, null);
         m_outTextPane.setCaretPosition(m_outTextPane.getDocument().getLength());
      } catch (BadLocationException e) {
         e.printStackTrace();
      }
      */
        m_client.printMessage(strText);

        return;
    }

   /*
   private void printStyledMessage(String strText, String strStyleName)
   {
      m_client.printStyledMessage(strText, strStyleName);
   }
   */

    private void printImage(String strPath)
    {
        m_client.printImage(strPath);
    }

    private void printFilePath(String strPath)
    {
        m_client.printFilePath(strPath);
    }

   /*
   private void setMessage(String strText)
   {
      m_outTextArea.setText(strText);
      m_outTextArea.setCaretPosition(m_outTextArea.getDocument().getLength());
   }
   */
   private int compareLogicalClocks(int clientLogicalClock, int serverLogicalClock) {// logical clock 비교 해서 더  큰 값 보다 +1 해서 내놓음
       if (serverLogicalClock >= clientLogicalClock) {
           return serverLogicalClock+1;
       } else {
           return clientLogicalClock+1;
       }
   }
}