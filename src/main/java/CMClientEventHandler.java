import kr.ac.konkuk.ccslab.cm.event.*;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;

public class CMClientEventHandler implements CMAppEventHandler {
    private CMClientStub m_clientStub;


    public CMClientEventHandler(CMClientStub clientStub) {
        m_clientStub = clientStub;

    }

    @Override
    public void processEvent( CMEvent cme) {
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
            case CMSessionEvent.SESSION_TALK:// chat
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
        switch (ie.getID()) {
            case CMInterestEvent.USER_TALK:
                System.out.println("(" + ie.getHandlerSession() + ", " + ie.getHandlerGroup() + ")");
                System.out.println("<" + ie.getUserName() + ">: " + ie.getTalk());
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
                    System.err.print("[" + fe.getFileName() + "] does not exist in the owner!\n");
                } else if (fe.getReturnCode() == 0) {
                    System.err.print("[" + fe.getFileSender() + "] rejects to send file(" + fe.getFileName() + ").\n");
                } else
                    System.out.println("[" + fe.getFileSender() + "] send file(" + fe.getFileName() + ").\n");
                break;
//            case CMFileEvent.REPLY_PERMIT_PUSH_FILE:
//                System.out.println(  "["+fe.getFileName() + "] 송신이 완료 되었습니다.\n");
//                break;

//            case CMFileEvent.END_FILE_TRANSFER_CHAN_ACK:
//                System.out.println(fe.getFileName() + "파일 송신이 완료되었습니다.\n");
//                break;
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
}

