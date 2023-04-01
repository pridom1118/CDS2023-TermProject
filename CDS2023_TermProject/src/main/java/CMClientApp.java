import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;

import java.util.Scanner;

public class CMClientApp {
    private CMClientStub m_clientStub;
    private CMClientEventHandler m_eventHandler;

    public CMClientApp() {
        m_clientStub = new CMClientStub();
        m_eventHandler = new CMClientEventHandler(m_clientStub);
    }

    public CMClientStub getClientStub() { return m_clientStub; }
    public CMClientEventHandler getClientEventHandler() { return m_eventHandler; }

    public void printAllMenu() {
        System.out.println("---------- Menu ----------");
        System.out.println("1. ");
    }

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        CMClientApp client = new CMClientApp();
        CMClientStub clientStub = client.getClientStub();
        CMClientEventHandler eventHandler = client.getClientEventHandler();
        boolean ret = false;

        /* init CM */
        clientStub.setAppEventHandler(client.getClientEventHandler());
        ret = clientStub.startCM();

        if(ret) System.out.println("init success");
        else {
            System.out.println("init error!");
            return;
        }

        /* login CM Server */
        String user_id, user_pw, user_input;
        System.out.println("Input ID: ");
        user_id = scan.nextLine();
        System.out.println("Input PW: ");
        user_pw = scan.nextLine();
        ret = clientStub.loginCM(user_id, user_pw);

        if(ret) System.out.println("LOGIN SUCCESSFUL");
        else {
            System.out.println("LOGIN REQUEST FAILED");
            return;
        }
        /* wait before executing next API */
        System.out.println("Press Enter to execute next API");
        user_input = scan.nextLine();

        if(user_input.equals("exit")) {
            clientStub.logoutCM();
            clientStub.terminateCM();
        }
    }
}
