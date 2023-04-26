import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.CMFileEvent;
import kr.ac.konkuk.ccslab.cm.event.CMInterestEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;

public class CMServerEventHandler implements CMAppEventHandler {
    private CMServerStub m_serverStub;

    public CMServerEventHandler(CMServerStub serverStub) {

        m_serverStub = serverStub;
    }

    @Override
    public void processEvent(CMEvent cme) {
        switch (cme.getType()) {
            case CMInfo.CM_SESSION_EVENT -> {
                SprocessSessionEvent(cme);
                break;
            }
            case CMInfo.CM_INTEREST_EVENT ->  {
                SprocessInterestEvent(cme);
                break;
            }
            case CMInfo.CM_FILE_EVENT -> {
                SprocessFileEvent(cme);
                break;
            }
            default -> {
                return;
            }
        }
    }

    private void SprocessSessionEvent(CMEvent cme) {
        CMSessionEvent se = (CMSessionEvent) cme;
        switch (se.getID())
        {
            case CMSessionEvent.SESSION_TALK->{
            System.out.println("("+se.getHandlerSession()+")");
            System.out.println("<"+se.getUserName()+">: "+se.getTalk());
            break;}
            case CMSessionEvent.LOGIN ->{
                System.out.println("["+se.getUserName()+"] requests login.");
                break;
            }
            case CMSessionEvent.LOGOUT ->{
                System.out.println("["+se.getUserName()+"] logout this server.");
                break;
            }
            default -> {
            return;
            }
        }

    }
    private void SprocessInterestEvent(CMEvent cme) {
        CMInterestEvent ie = (CMInterestEvent) cme;
        switch (ie.getID()) {
            case CMInterestEvent.USER_TALK -> {
                System.out.println("(" + ie.getHandlerSession() + ", " + ie.getHandlerGroup() + ")");
                System.out.println("<" + ie.getUserName() + ">: " + ie.getTalk());
                break;
            }
            default -> {
                return;
            }
        }
    }
    private void SprocessFileEvent(CMEvent cme) {
        CMFileEvent fe = (CMFileEvent) cme;

        switch (fe.getID()) {
            case CMFileEvent.REQUEST_PERMIT_PULL_FILE:
                System.out.println("["+fe.getFileReceiver()+"] requests file("+fe.getFileName()+ ").\n");
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
                } else if (fe.getReturnCode() == 0) {
                    System.err.print("[" + fe.getFileReceiver() + "] receive fail\n");
                    break;
                }
                return;
        }
    }

}
