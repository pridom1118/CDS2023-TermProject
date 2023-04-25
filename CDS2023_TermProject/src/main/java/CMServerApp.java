import kr.ac.konkuk.ccslab.cm.entity.CMGroup;
import kr.ac.konkuk.ccslab.cm.entity.CMList;
import kr.ac.konkuk.ccslab.cm.entity.CMMember;
import kr.ac.konkuk.ccslab.cm.entity.CMMessage;
import kr.ac.konkuk.ccslab.cm.entity.CMRecvFileInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMSendFileInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMServer;
import kr.ac.konkuk.ccslab.cm.entity.CMSession;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.CMBlockingEventQueue;
import kr.ac.konkuk.ccslab.cm.event.CMDummyEvent;
import kr.ac.konkuk.ccslab.cm.info.CMCommInfo;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMFileTransferInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInteractionInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMCommManager;
import kr.ac.konkuk.ccslab.cm.manager.CMConfigurator;
import kr.ac.konkuk.ccslab.cm.manager.CMFileSyncManager;
import kr.ac.konkuk.ccslab.cm.manager.CMMqttManager;
import kr.ac.konkuk.ccslab.cm.sns.CMSNSUserAccessSimulator;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;

import java.nio.file.Paths;
import java.util.Vector;
import java.util.*;

public class CMServerApp {
    private CMServerStub m_serverStub;
    private CMServerEventHandler m_eventHandler;

    public CMServerApp() {
        m_serverStub = new CMServerStub();
        m_eventHandler = new CMServerEventHandler(m_serverStub);
    }

    public CMServerStub getServerStub() { return m_serverStub; }
    public CMServerEventHandler getServerEventHandler() { return m_eventHandler; }

    public void printAllMenu() {
        System.out.println("---------- Menu ----------");
        System.out.println("1. Display currently logged-in users.");
        System.out.println("---------- File Transfer ----------");
        System.out.println("2. Set file path.");
        System.out.println("3. Request file.");
        System.out.println("4. Push file.");
        System.out.println("5. Cancel receiving the file.");
        System.out.println("6. Cancel sending the file.");
        System.out.println("7. Print sending / requesting the file");
        System.out.println("------------------------------");
        System.out.println("999: TerminateCM.");
    }

    public void printLoginUsers() {
        System.out.println("=============== USER LIST ===============");
        CMMember loginUsers = m_serverStub.getLoginUsers();
        if(loginUsers == null) {
            System.err.println("There are no users online.");
            return;
        }
        System.out.println("Currently " + loginUsers.getMemberNum() + " users are online.");
        Vector<CMUser> loginUserVector = loginUsers.getAllMembers();
        Iterator<CMUser> iter = loginUserVector.iterator();

        System.out.println("==============================");
        while(iter.hasNext()) {
            CMUser user = iter.next();
            System.out.println(user.getName());
        }
        System.out.println("==============================");
    }

    public void setFilePath() {
        String strPath = null;
        Scanner scan_path = new Scanner(System.in);
        strPath = scan_path.nextLine();
        m_serverStub.setTransferedFileHome(Paths.get(strPath));
        System.out.println("Set file path to " + strPath);
    }

    public void processInput(String strInput) {
        int nCommand = -1;
        try {
            nCommand = Integer.parseInt(strInput);
        } catch (NumberFormatException e) {
            System.out.println("Wrong input");
            return;
        }

        switch(nCommand) {
            case 0:
                printAllMenu();
                break;
            case 1:
                printLoginUsers();
                break;
            case 2:
        }
    }

    public static void main(String[] args) {
        CMServerApp server = new CMServerApp();
        CMServerStub cmStub = server.getServerStub();
        cmStub.setAppEventHandler(server.getServerEventHandler());
        cmStub.startCM();

        Scanner scan = new Scanner(System.in);
        String cmd = "hi";

        while(true) {
            cmd = scan.nextLine();
            if(cmd.equals("exit")) {
                cmStub.terminateCM();
                System.out.println("CM Server terminates.");
                break;
            } else server.processInput(cmd);
        }
    }
}


