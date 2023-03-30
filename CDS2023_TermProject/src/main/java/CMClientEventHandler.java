import kr.ac.konkuk.ccslab.cm.event.*;
import kr.ac.konkuk.ccslab.cm.event.filesync.CMFileSyncEvent;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;

public class CMClientEventHandler implements CMAppEventHandler {
    private CMClientStub m_clientStub;

    public CMClientEventHandler(CMClientStub stub) { m_clientStub = stub; }

    @Override
    public void processEvent(CMEvent cme) {
        switch(cme.getType()) {
            case CMInfo.CM_SESSION_EVENT:
                processSessionEvent(cme);
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
                    System.err.println("This client fails authentication by the default server!");
                else if (se.isValidUser() == -1)
                    System.err.println("This client is already in the login-user list!");
                else
                    System.out.println("This client successfully logs in to the default server.");
                break;
            case CMSessionEvent.SESSION_ADD_USER:
                System.out.println(se.getUserName() + " just logged in.");
                break;
            default:
                return;
        }
    }
}
