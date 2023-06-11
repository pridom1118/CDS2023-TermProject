import kr.ac.konkuk.ccslab.cm.entity.CMUser;
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
import java.io.IOException;
import java.nio.file.*;

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
            case CMInfo.CM_DUMMY_EVENT:
                processDummyEvent(cme);
                break;
            default:
                return;
        }
    }

    private void processSessionEvent(CMEvent cme) {
        CMSessionEvent se = (CMSessionEvent) cme;
        String strFilePath = System.getProperty("user.dir") + "\\client-file-path\\";
        switch(se.getID()) {
            case CMSessionEvent.LOGIN_ACK:
                if (se.isValidUser() == 0)
                    printMessage("This client fails authentication by the default server!\n");
                else if (se.isValidUser() == -1)
                    printMessage("This client is already in the login-user list!\n");
                else {
                    printMessage("This client successfully logs in to the default server.\n");
                    CMInteractionInfo interInfo = m_clientStub.getCMInfo().getInteractionInfo();
                    strFilePath += interInfo.getMyself().getName();
                    m_client.setTitle("CM Client [" + interInfo.getMyself().getName() + "]");
                    m_client.setButtonsAccordingToClientState();
                    m_clientStub.setTransferedFileHome(Paths.get(strFilePath));
                    try {
                        m_client.initFileDir();
                        m_client.startWatchService();
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
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
                printMessage("["+de.getUserName()+"] just logged off.\n");
                break;
            default:
                return;
        }
    }

    private void processFileEvent(CMEvent cme) {
        CMFileEvent fe = (CMFileEvent) cme;
        CMConfigurationInfo conInfo = null;
        CMFileTransferInfo fInfo = m_clientStub.getCMInfo().getFileTransferInfo();
        String sender = cme.getSender();

        int nOption = -1;
        long lTotalDelay = 0;
        long lTransferDelay = 0;

        switch(fe.getID()) {
            case CMFileEvent.REQUEST_PERMIT_PULL_FILE:
                if(sender == "SERVER") {
                    printMessage("Synchronizing a file" + fe.getFileName() + " to server.\n");
                    if(fe.getReturnCode() == -1) {
                        printMessage(fe.getFileName() + " does not exist in the client\n");
                    }
                }
                break;
            case CMFileEvent.REPLY_PERMIT_PULL_FILE:
                break;
            case CMFileEvent.REQUEST_PERMIT_PUSH_FILE:
                break;
            case CMFileEvent.REPLY_PERMIT_PUSH_FILE:
                break;
            case CMFileEvent.START_FILE_TRANSFER:
                if(fInfo.getStartRequestTime() != 0) {
                    printMessage("[" + fe.getFileSender() + "] starts to send file:  " + fe.getFileName() + ".\n");
                }
                break;
            case CMFileEvent.START_FILE_TRANSFER_ACK:
                if(fInfo.getStartRequestTime() != 0) {
                    printMessage("["+fe.getFileReceiver()+"] starts to receive file: " + fe.getFileName() +".\n");
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

    private void processDummyEvent(CMEvent cme) {
        CMDummyEvent due = (CMDummyEvent) cme;

        //Process the message
        // [mode / filename / size / lclock / sharedUsers]
        String[] msgPayload = due.getDummyInfo().split("/");
        String[] sharedUsers = msgPayload[4].split("\n");
        String sender = due.getSender();
        String myName = m_clientStub.getCMInfo().getInteractionInfo().getMyself().getName();
        long size = Long.parseLong(msgPayload[2]);
        int lclock = Integer.parseInt(msgPayload[3]);
        String strPath = System.getProperty("user.dir") + "\\client-file-path\\" + myName + "\\" + msgPayload[1];
        Path filePath = Paths.get(strPath);

        System.out.println("\nGot sync message from: " + sender);

        //at this point
        switch(msgPayload[0]) {
            case "share":
                printMessage("File Share from user: " + sender + "\n");
                printMessage(msgPayload[1] + " shared!\n");

                printMessage("Shared users: ");
                for(String u: sharedUsers) {
                    printMessage(u + "\n");
                }

                SyncFile myFile = new SyncFile(msgPayload[1], size, lclock, sharedUsers);
                m_client.HT.put(msgPayload[1], myFile);
                m_client.getFileDir();
                break;
            case "update":
                printMessage("File Sync from server: Updating the file " + msgPayload[1] + "\n");
                try {
                    Files.delete(filePath);
                    System.out.println(sender + " just deleted the file " + msgPayload[1]);
                } catch(NoSuchFileException e) {
                    System.out.println("File " + msgPayload[1] + " does not exist.");
                } catch(IOException e) {
                    e.printStackTrace();
                }

                SyncFile syncFile = new SyncFile(msgPayload[1], size, lclock, sharedUsers);
                m_client.HT.put(msgPayload[1], syncFile);
                m_clientStub.requestFile(sharedUsers[0] + "\\" + msgPayload[1], sender, CMInfo.FILE_DEFAULT);
                printMessage("Successfully synchronized the file: " + msgPayload[1] + " with lclock value " + msgPayload[3] + "\n");
                m_client.getFileDir();
                break;
            case "delete":
                printMessage("FileSync: file" + msgPayload[1] + " deleted\n");
                try {
                    Files.delete(filePath);
                    System.out.println(sender + " just deleted the file " + msgPayload[1]);
                    m_client.HT.remove(msgPayload[1]);
                    printMessage("Deleted the shared file: " + msgPayload[1] + "\n");
                    m_client.getFileDir();
                } catch(NoSuchFileException e) {
                    System.out.println("File " + msgPayload[1] + " does not exist.");
                } catch(IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                System.err.println("Error while processing the sync message!");
                break;
        }
        return;
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
