import kr.ac.konkuk.ccslab.cm.event.*;
import kr.ac.konkuk.ccslab.cm.event.filesync.CMFileSyncEvent;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMFileTransferInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInteractionInfo;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;

import java.io.File;
import java.io.FileOutputStream;

public class CMClientEventHandler implements CMAppEventHandler {
    private CMClientStub m_clientStub;
    private CMClientApp m_client;

    private FileOutputStream m_fos;

    private long m_lDelaySum, m_lStartTime;
    private boolean m_bReqAttachedFile;
    private String m_strFileSender;
    private String m_strFileReceiver;
    private File[] m_arraySendFiles;

    public CMClientEventHandler(CMClientStub stub, CMClientApp client) {
        m_clientStub = stub;
        m_client = client;
        m_lDelaySum = m_lStartTime = 0;
        m_fos = null;
        m_strFileReceiver = null;
        m_strFileSender = null;
        m_arraySendFiles = null;
    }

    @Override
    public void processEvent(CMEvent cme) {
        switch(cme.getType()) {
            case CMInfo.CM_SESSION_EVENT:
                processSessionEvent(cme);
                break;
            case CMInfo.CM_DATA_EVENT:
                processDataEvent(cme);
                break;
            case CMInfo.CM_FILE_EVENT:
                processFileEvent(cme);
                break;
            default:
                return;
        }
    }

    private void processSessionEvent(CMEvent cme) {
        CMSessionEvent se = (CMSessionEvent) cme;
        switch(se.getID()) {
            case CMSessionEvent.LOGIN_ACK:
                if (se.isValidUser() == 0)
                    printMessage("This client fails authentication by the default server!\n");
                else if (se.isValidUser() == -1)
                    printMessage("This client is already in the login-user list!\n");
                else {
                    printMessage("This client successfully logs in to the default server.\n");
                    CMInteractionInfo interInfo = m_clientStub.getCMInfo().getInteractionInfo();
                    m_client.setTitle("CM Client [" + interInfo.getMyself().getName() + "]");
                    m_client.setButtonsAccordingToClientState();
                }
                break;
            case CMSessionEvent.JOIN_SESSION:
                m_client.setButtonsAccordingToClientState();
                break;
            case CMSessionEvent.UNEXPECTED_SERVER_DISCONNECTION:
                m_client.printStyledMessage("Unexpected disconnection from [" + se.getChannelName() + "].\n", "bold");
                m_client.setButtonsAccordingToClientState();
                m_client.setTitle("CM Client");
                break;
            case CMSessionEvent.INTENTIONALLY_DISCONNECT:
                printMessage("Intentionally disconnected from the channel.\n");
                m_client.setButtonsAccordingToClientState();
                m_client.setTitle("CM Client");
                break;
            default:
                return;
        }
    }

    private void processDataEvent(CMEvent cme) {
        CMDataEvent de = (CMDataEvent) cme;

        switch(de.getID()) {
            case CMDataEvent.NEW_USER:
                printMessage("["+de.getUserName()+"] just logged in.\n");
                break;
            case CMDataEvent.REMOVE_USER:
                printMessage("["+de.getUserName()+"] just logged off.");
                break;
            default:
                return;
        }
    }

    private void processFileEvent(CMEvent cme) {
        CMFileEvent fe = (CMFileEvent) cme;
        CMConfigurationInfo conInfo = null;
        CMFileTransferInfo fInfo = m_clientStub.getCMInfo().getFileTransferInfo();

        int nOption = -1;
        long lTotalDelay = 0;
        long lTransferDelay = 0;

        switch(fe.getID()) {
            case CMFileEvent.REQUEST_PERMIT_PULL_FILE:
                break;
            case CMFileEvent.REPLY_PERMIT_PULL_FILE:
                break;
            case CMFileEvent.REQUEST_PERMIT_PUSH_FILE:
                break;
            case CMFileEvent.REPLY_PERMIT_PUSH_FILE:
                break;
            case CMFileEvent.START_FILE_TRANSFER:
                if(fInfo.getStartRequestTime() != 0) {
                    printMessage("[" + fe.getFileReceiver() + "] starts to receive file:  " + fe.getFileName() + ".\n");
                }
                break;
            case CMFileEvent.END_FILE_TRANSFER:
                if(fInfo.getStartRequestTime() != 0) {
                    printMessage("[" + fe.getFileSender() + "] completes to send file: " + fe.getFileName() + ".\n");
                    printMessage(" file size: " + fe.getFileSize() + "bytes.\n");
                    lTotalDelay = fInfo.getEndRecvTime() - fInfo.getStartRequestTime();
                    printMessage("total delay: " + lTotalDelay + "ms, ");
                    lTransferDelay = fInfo.getEndRecvTime() - fInfo.getStartRecvTime();
                    printMessage(" file receiving delay: " + lTransferDelay + " ms.\n");
                }
                break;
            case CMFileEvent.END_FILE_TRANSFER_ACK:
                if(fInfo.getStartRequestTime() != 0) {
                    printMessage("[" + fe.getFileReceiver() + "] completes to receive file: " + fe.getFileName() + ".\n");
                    printMessage(" file size: " + fe.getFileSize() + "bytes.\n");
                    lTotalDelay = fInfo.getEndSendTime() - fInfo.getStartRequestTime();
                    printMessage("total delay: " + lTotalDelay + "ms, ");
                    lTransferDelay = fInfo.getEndSendTime() - fInfo.getStartSendTime();
                    printMessage(" file sending delay: " + lTransferDelay + " ms.\n");
                }
        }
    }
    public void setReqAttachedFile(boolean bReq) {
        m_bReqAttachedFile = bReq;
    }

    private void printMessage(String strText) { m_client.printMessage(strText); }
    private void printFilePath(String strPath)
    {
        m_client.printFilePath(strPath);
    }
}
