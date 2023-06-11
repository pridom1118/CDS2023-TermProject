import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.*;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.CMFileTransferInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInteractionInfo;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Hashtable;

public class CMServerEventHandler implements CMAppEventHandler {
    private CMServerStub m_serverStub;
    private Hashtable<String, FileInfo> fileHashTable;


    public class FileInfo {
        public String fileName;
        public long fileSize;
        public int lclock;
        public ArrayList<String> sharedUsers;

        public FileInfo(String name, long size, String username) {
            fileName = name;
            fileSize = size;
            lclock = 0;
            sharedUsers = new ArrayList<String>();
            sharedUsers.add(username);
        }

        public FileInfo(String name, long size, String[] users) {
            fileName = name;
            fileSize = size;
            lclock = 0;
            sharedUsers = new ArrayList<String>();

            for(String u: users) {
                sharedUsers.add(u);
            }
        }

        // should be called before synchronization
        public void updateFile() {
            lclock++;
        }

        public void shareFile(String usr) {
            sharedUsers.add(usr);
        }

        public void updateSize(long size) {
            fileSize = size;
        }

        public void setUsers(String[] users) {
            sharedUsers.clear();
            for(String u: users) {
                sharedUsers.add(u);
            }
        }

        public String[] getValue() {
            String usrs = "";

            for(String u: sharedUsers) {
                usrs += u + "\n";
            }
            usrs = usrs.substring(0, usrs.length()-1);

            String[] ret = { fileName, String.valueOf(fileSize), String.valueOf(lclock), usrs };
            return ret;
        }
    }


    public CMServerEventHandler(CMServerStub serverStub) {
        m_serverStub = serverStub;
        fileHashTable = new Hashtable<String, FileInfo>();
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
            case CMInfo.CM_DUMMY_EVENT:
                processDummyEvent(cme);
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

    private void processDummyEvent(CMEvent cme) {
        CMDummyEvent due = (CMDummyEvent) cme;

        //Process the message
        // [mode / filename / size / lclock / sharedUsers]
        String[] msgPayload = due.getDummyInfo().split("/");
        String[] sharedUsers = msgPayload[4].split("\n");
        String sender = due.getSender();
        long size = Long.parseLong(msgPayload[2]);
        int lclock = Integer.parseInt(msgPayload[3]);
        String strPath = System.getProperty("user.dir") + "\\server-file-path\\" + sender + "\\" + msgPayload[1];
        FileInfo fileInfo = null;

        System.out.println("\nGot sync message from the client: " + sender);

        //at this point
        switch(msgPayload[0]) {
            case "create":
                if(!fileHashTable.contains(msgPayload[1])) {
                    System.out.println(sender + " just created the file " + msgPayload[1]);
                    fileInfo = new FileInfo(msgPayload[1], size, sender);
                    fileHashTable.put(msgPayload[1], fileInfo);
                }
                break;
            case "update":
                int serverLclock = 0;
                if(fileHashTable.contains(msgPayload[1])) serverLclock = fileHashTable.get(msgPayload[1]).lclock;
                System.out.println(sender + " just updated the file " + msgPayload[1]);

                //logical clock
                if(lclock > fileHashTable.get(msgPayload[1]).lclock) {
                    fileHashTable.remove(msgPayload[1]);
                    fileInfo = new FileInfo(msgPayload[1], size, sharedUsers);
                    System.out.println(msgPayload[1] + " / " + msgPayload[2] + " bytes / " + msgPayload[3]);
                    fileHashTable.put(msgPayload[1], fileInfo);

                    Path filePath = Paths.get(strPath);

                    try {
                        Files.delete(filePath);
                        System.out.println(sender + " just deleted the file " + msgPayload[1]);
                        fileHashTable.remove(msgPayload[1]);
                    } catch(NoSuchFileException e) {
                        System.out.println("File " + msgPayload[1] + " does not exist.");
                    } catch(IOException e) {
                        e.printStackTrace();
                    }

                    m_serverStub.requestFile(sender + "\\" + msgPayload[1], sender, CMInfo.FILE_DEFAULT);

                    //if there are shared users
                    if(sharedUsers.length > 1) {
                        for(String u: sharedUsers) {
                            if(!u.equals(sender)) {
                                CMInteractionInfo interInfo = m_serverStub.getCMInfo().getInteractionInfo();
                                CMUser myself = interInfo.getMyself();
                                CMDummyEvent syncEvent = new CMDummyEvent();
                                syncEvent.setHandlerSession(myself.getCurrentSession());
                                syncEvent.setHandlerGroup(myself.getCurrentGroup());
                                syncEvent.setDummyInfo(due.getDummyInfo());

                                m_serverStub.send(syncEvent, u);
                            }
                        }
                    }
                } else System.out.println(msgPayload[1] + " is outdated!");
                break;
            case "delete":
                Path filePath = Paths.get(strPath);

                try {
                    Files.delete(filePath);
                    System.out.println(sender + " just deleted the file " + msgPayload[1]);
                    fileHashTable.remove(msgPayload[1]);
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
}
