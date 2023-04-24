import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.CMFileEvent;
import kr.ac.konkuk.ccslab.cm.event.CMInterestEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.CMFileTransferInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;

public class CMServerEventHandler implements CMAppEventHandler {
    private CMServerStub m_serverStub;

    public CMServerEventHandler(CMServerStub serverStub) {
        m_serverStub = serverStub;

    }

    @Override
    public void processEvent(CMEvent cme) {
        switch(cme.getType()) {
            case CMInfo.CM_SESSION_EVENT:
                processSessionEvent(cme);
                break;
            case CMInfo.CM_FILE_EVENT:
                processFileEvent(cme);
                break;
            case CMInfo.CM_INTEREST_EVENT:
                processInterestEvent(cme);
                break;
            default:
                return;
        }
    }

    private void processSessionEvent(CMEvent cme) {
        CMSessionEvent se = (CMSessionEvent) cme;
        switch(se.getID()) {
            case CMSessionEvent.LOGIN:
                System.out.println("[" + se.getUserName() + "] requests login.");
                break;
            case CMSessionEvent.LOGOUT:
                System.out.println("[" + se.getUserName() + "] logs out.");
                break;
            case CMSessionEvent.INTENTIONALLY_DISCONNECT:
                System.out.println("[" + se.getUserName() + "] just intentionally disconnected from the server.");
                break;
            default:
                return;
        }
    }

    private void processFileEvent(CMEvent cme) {
        CMFileTransferInfo fInfo = m_serverStub.getCMInfo().getFileTransferInfo();
        long lTotalDelay = 0;
        long lTransferDelay = 0;
        boolean ret = false;

        CMFileEvent fe = (CMFileEvent) cme;
        switch(fe.getID()) {
            case CMFileEvent.REPLY_PERMIT_PULL_FILE:
                if(fe.getReturnCode() == -1) {
                    System.out.println("[" + fe.getFileName() + "] does not exist in the owner");
                } else if(fe.getReturnCode() == 0) {
                    System.out.println("[" + fe.getFileSender() + "] rejects to send the file: " + fe.getFileName());
                }
                break;
            case CMFileEvent.REPLY_PERMIT_PUSH_FILE:
                if(fe.getReturnCode() == 0) {
                    System.out.println("[" + fe.getFileReceiver() + "] rejected the push-file request.");
                    System.out.println("file path: " + fe.getFilePath() + ", size: " + fe.getFileSize());
                }
                break;
            case CMFileEvent.START_FILE_TRANSFER:
                if(fInfo.getStartRequestTime() != 0) {
                    System.out.println("[" + fe.getFileSender() + "] starts to send file: " + fe.getFileName());
                }
                break;
            case CMFileEvent.START_FILE_TRANSFER_ACK:
                if(fInfo.getStartRequestTime() != 0) {
                    System.out.println("[" + fe.getFileReceiver() + "] starts to receive file: " + fe.getFileName());
                }
                break;
            case CMFileEvent.END_FILE_TRANSFER:
                if(fInfo.getStartRequestTime() != 0) {
                    System.out.println("[" + fe.getFileSender() + "] completes to send file: " + fe.getFileName());
                    System.out.println("total bytes: " + fe.getFileSize() + " Bytes.");
                    lTotalDelay = fInfo.getEndRecvTime() - fInfo.getStartRequestTime();
                    lTransferDelay = fInfo.getEndRecvTime() - fInfo.getStartRecvTime();
                    System.out.println("total delay: " + lTotalDelay + "ms, file-receiving delay: " + lTransferDelay + "ms.");
                }
                break;
            case CMFileEvent.END_FILE_TRANSFER_ACK:
                if(fInfo.getStartRequestTime() != 0) {
                    System.out.println("[" + fe.getFileReceiver() + "] completes to receive file: " + fe.getFileName());
                    System.out.println("total bytes: " + fe.getFileSize() + " Bytes.");
                    lTotalDelay = fInfo.getEndSendTime() - fInfo.getStartRequestTime();
                    lTransferDelay = fInfo.getEndSendTime() - fInfo.getStartSendTime();
                    System.out.println("total delay: " + lTotalDelay + "ms, file-sending delay: " + lTransferDelay + "ms.");
                }
                break;
            default:
                return;
        }
    }

    private void processInterestEvent(CMEvent cme) {
        CMInterestEvent ie = (CMInterestEvent) cme;
        switch(ie.getID()) {
            case CMInterestEvent.USER_ENTER:
                System.out.println("[" + ie.getUserName() + "] successfully logged in, and enters the group.");
                break;
            case CMInterestEvent.USER_LEAVE:
                System.out.println("[" + ie.getUserName() + "] leaves the group.");
                break;
            default:
                return;
        }
    }
}
