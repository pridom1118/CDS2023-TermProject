import kr.ac.konkuk.ccslab.cm.entity.CMList;
import kr.ac.konkuk.ccslab.cm.entity.CMRecvFileInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMSendFileInfo;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMFileTransferInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInteractionInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMCommManager;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

public class CMClientApp extends JFrame {
    private JTextPane m_outTextPane;
    private JTextPane m_fileTextPane;
    private JTextField m_inTextField;
    private JTextField m_fileTextField;
    private CMClientStub m_clientStub;
    private CMClientEventHandler m_eventHandler;
    private MyMouseListener cmMouseListener;

    private JButton m_startStopButton;
    private JButton m_loginLogoutButton;
    private JButton m_fileUploadButton;
    private JButton m_fileShareButton;
    private JButton m_refreshButton;

    // For displaying the file list using JTable
    private JScrollPane m_fileScrollPane;
    private JTable m_fileTable;
    //The header and contents of the table
    private String[] header;
    private ArrayList<String[]> contents;

    //WatchService: Automatically detects creation / deletion / update of the file
    private WatchKey watchkey;
    private static String user_path;

    public CMClientApp() {
        MyKeyListener cmKeyListener = new MyKeyListener();
        MyActionListener cmActionListener = new MyActionListener();

        setTitle("CM Client");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        //Table init
        header = new String[] {"Name", "Size", "Lclock"};
        contents = new ArrayList<String[]>();

        m_outTextPane = new JTextPane();
        m_outTextPane.setBackground(new Color(245, 245, 245));
        m_outTextPane.setEditable(false);

        StyledDocument doc = m_outTextPane.getStyledDocument();
        addStylesToDocument(doc);
        add(m_outTextPane, BorderLayout.SOUTH);
        JScrollPane centerScroll = new JScrollPane (m_outTextPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        getContentPane().add(centerScroll, BorderLayout.EAST);

        m_fileTextPane = new JTextPane();
        m_fileTextPane.setBackground(new Color(245, 245, 245));
        m_fileTextPane.setEditable(false);

        StyledDocument fdoc = m_fileTextPane.getStyledDocument();
        addStylesToDocument(fdoc);
        add(m_fileTextPane, BorderLayout.CENTER);
        JScrollPane fScroll = new JScrollPane (m_fileTextPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        getContentPane().add(fScroll, BorderLayout.CENTER);

        JPanel topButtonPanel = new JPanel();
        topButtonPanel.setBackground(new Color(220, 220, 220));
        topButtonPanel.setLayout(new FlowLayout());
        add(topButtonPanel, BorderLayout.NORTH);

        m_startStopButton = new JButton("Start CM Client");
        m_startStopButton.addActionListener(cmActionListener);
        m_startStopButton.setEnabled(false);
        topButtonPanel.add(m_startStopButton);

        m_loginLogoutButton = new JButton("Login");
        m_loginLogoutButton.addActionListener(cmActionListener);
        m_loginLogoutButton.setEnabled(false);
        topButtonPanel.add(m_loginLogoutButton);

        m_fileUploadButton = new JButton("File Upload");
        m_fileUploadButton.addActionListener(cmActionListener);
        m_fileUploadButton.setEnabled(false);
        topButtonPanel.add(m_fileUploadButton);

        m_fileShareButton = new JButton("File Share");
        m_fileShareButton.addActionListener(cmActionListener);
        m_fileShareButton.setEnabled(false);
        topButtonPanel.add(m_fileShareButton);

        JPanel botButtonPanel = new JPanel();
        botButtonPanel.setBackground(new Color(220, 220, 220));
        botButtonPanel.setLayout(new FlowLayout());
        add(botButtonPanel, BorderLayout.SOUTH);

        m_refreshButton = new JButton("Refresh");
        m_refreshButton.addActionListener(cmActionListener);
        m_refreshButton.setEnabled(false);
        botButtonPanel.add(m_refreshButton);

        setVisible(true);
        m_clientStub = new CMClientStub();
        m_eventHandler = new CMClientEventHandler(m_clientStub, this);
        startCM();
    }

    public CMClientStub getClientStub() { return m_clientStub; }
    public CMClientEventHandler getClientEventHandler() { return m_eventHandler; }

    private void startCM() {
        boolean ret = false;
        // get local address
        List<String> localAddressList = CMCommManager.getLocalIPList();
        if(localAddressList == null) {
            System.err.println("Local address not found!");
            return;
        }
        String strCurrentLocalAddress = localAddressList.get(0).toString();

        // set config home
        m_clientStub.setConfigurationHome(Paths.get("."));
        // set file-path home
        m_clientStub.setTransferedFileHome(m_clientStub.getConfigurationHome().resolve("client-file-path"));

        // get the saved server info from the client configuration file
        String strSavedServerAddress = null;
        int nSavedServerPort = -1;

        strSavedServerAddress = m_clientStub.getServerAddress();
        nSavedServerPort = m_clientStub.getServerPort();

        ret = m_clientStub.startCM();
        if(!ret) {
            printStyledMessage("CM initialization error!\n", "bold");
        } else {
            m_startStopButton.setEnabled(true);
            m_loginLogoutButton.setEnabled(true);
            m_fileUploadButton.setEnabled(true);
            m_fileShareButton.setEnabled(true);
            printStyledMessage("Client CM starts.\n Welcome!\n" , "bold");
            setButtonsAccordingToClientState();
        }
    }

    private void stopCM() {
        m_clientStub.terminateCM();
        printMessage("Client CM terminates.\n");
        initializeButtons();
        setTitle("CM Client");
    }

    private void addStylesToDocument(StyledDocument doc) {
        Style defStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

        Style regularStyle = doc.addStyle("regular", defStyle);
        StyleConstants.setFontFamily(regularStyle, "SansSerif");

        Style boldStyle = doc.addStyle("bold", defStyle);
        StyleConstants.setBold(boldStyle, true);

        Style linkStyle = doc.addStyle("link", defStyle);
        StyleConstants.setForeground(linkStyle, Color.BLUE);
        StyleConstants.setUnderline(linkStyle, true);
    }

    public void clearMessage() {
        StyledDocument doc = m_outTextPane.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
            m_outTextPane.setCaretPosition(m_outTextPane.getDocument().getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }


    public void printMessage(String strText) {
        StyledDocument doc = m_outTextPane.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), strText, null);
            m_outTextPane.setCaretPosition(m_outTextPane.getDocument().getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    public void printStyledMessage(String strText, String strStyleName) {
        StyledDocument doc = m_outTextPane.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), strText, doc.getStyle(strStyleName));
            m_outTextPane.setCaretPosition(m_outTextPane.getDocument().getLength());

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        return;
    }

    public void printFileMessage(String strText) {
        StyledDocument doc = m_fileTextPane.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), strText, null);
            m_fileTextPane.setCaretPosition(m_fileTextPane.getDocument().getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void clearFileMessage() {
        StyledDocument doc = m_fileTextPane.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
            m_fileTextPane.setCaretPosition(m_fileTextPane.getDocument().getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void printFilePath(String strPath) {
        JLabel pathLabel = new JLabel(strPath);
        pathLabel.addMouseListener(cmMouseListener);
        m_outTextPane.insertComponent(pathLabel);
        printMessage("\n");
    }

    public void setButtonsAccordingToClientState() {
        int nClientState = m_clientStub.getCMInfo().getInteractionInfo().getMyself().getState();

        switch(nClientState) {
            case CMInfo.CM_INIT:
            case CMInfo.CM_CONNECT:
                m_startStopButton.setText("Stop CM Client");
                m_loginLogoutButton.setText("Login");
                break;
            case CMInfo.CM_LOGIN:
            case CMInfo.CM_SESSION_JOIN:
                m_startStopButton.setText("Stop CM Client");
                m_loginLogoutButton.setText("Logout");
                m_refreshButton.setEnabled(true);
                break;
            default:
                m_startStopButton.setText("Start CM Client");
                m_loginLogoutButton.setText("Login");
                break;
        }
        revalidate();
        repaint();
    }

    private void initializeButtons() {
        m_startStopButton.setText("Start CM Client");
        m_loginLogoutButton.setText("Login");
        m_fileUploadButton.setEnabled(false);
        m_fileShareButton.setEnabled(false);
        revalidate();
        repaint();
    }

    private void printAllMenu() {
        printMessage("---------- Menu ----------\n");
        printMessage("0. Print Menu\n");
        printMessage("1. Login\n");
        printMessage("2. Show currently logged in users\n");
        printMessage("\n---------- File ----------\n");
        printMessage("3. Upload / Send the file\n");
        printMessage("4. Download / Request the file\n");
        printMessage("5. Print currently sending / receiving file\n");
        printMessage("\nclear. Literally clears the display.\n");
    }

    private void processInput(String strInput) {
        int nCommand = -1;

        if(strInput.equals("clear") || strInput.equals("Clear")) {
            clearMessage();
            return;
        }

        try {
            nCommand = Integer.parseInt(strInput);
        } catch (NumberFormatException e) {
            printMessage("Incorrect command number!\n");
            return;
        }

        switch(nCommand) {
            case 0:
                printAllMenu();
                break;
            case 1:
                requestLogin();
                break;
            case 2:
                //show logged in users
                break;
            case 3:
                pushFile();
                break;
            case 4:
                pullFile();
                break;
            case 5:
                printCurrentFileInfo();
                break;
            default:
                System.err.println("Wrong Input.");
                break;
        }
    }

    private void requestLogin() {
        /* login CM Server */
        String username = null;
        boolean ret = false;
        JTextField userNameField = new JTextField();

        Object[] message = {
                "Username:", userNameField
        };

        int option = JOptionPane.showConfirmDialog(null, message, "Login", JOptionPane.OK_CANCEL_OPTION);

        if(option == JOptionPane.OK_OPTION) {
            username = userNameField.getText();
            ret = m_clientStub.loginCM(username, "");
            if(ret) System.out.println("successfully sent the login request.\n");
            else System.out.println("failed to send the login request.\n");
            getFileDir();
            user_path = System.getProperty("user.dir");
            user_path += "\\client-file-path\\" + username;

            try {
                startWatchService();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void requestLogout() {
        printMessage("Requesting logout.\n");
        boolean ret = m_clientStub.logoutCM();
        if(ret) printMessage("successfully sent the logout request\n");
        else printMessage("logout request failed.\n");

        setButtonsAccordingToClientState();
        setTitle("CM Client");
    }

    private void requestAttachedFile(String strFileName) {
        boolean bRet = m_clientStub.requestAttachedFileOfSNSContent(strFileName);
        if(bRet) m_eventHandler.setReqAttachedFile(true);
        else
            printMessage(strFileName+" not found in the downloaded content list!\n");
        return;
    }

    private void accessAttachedFile(String strFileName) {
        boolean bRet = m_clientStub.accessAttachedFileOfSNSContent(strFileName);
        if(!bRet)
            printMessage(strFileName+" not found in the downloaded content list!\n");
        return;
    }
    private void pullFile() {
        boolean ret = false;
        String strFileName = null;
        String strFileOwner = null;
        byte byteFileAppendMode = -1;
        CMInteractionInfo interInfo = m_clientStub.getCMInfo().getInteractionInfo();

        printMessage("---------- File Request ----------\n");

        JTextField fnameField = new JTextField();
        JTextField fownerField = new JTextField();
        String[] fAppendMode = { "Default", "Overwrite", "Append" };
        JComboBox<String> fAppendBox = new JComboBox<String>(fAppendMode);

        Object[] message = {
                "File Name: ", fnameField,
                "File Owner(empty for default server): ", fownerField,
                "File Append Mode: ", fAppendBox
        };

        int option = JOptionPane.showConfirmDialog(null, message, "File Request", JOptionPane.OK_CANCEL_OPTION);
        if(option != JOptionPane.OK_OPTION) {
            printMessage("File request canceled\n");
            return;
        }

        strFileName = fnameField.getText().trim();
        if(strFileName.isEmpty()) {
            printMessage("Empty file name!\n");
            return;
        }
        strFileOwner = fownerField.getText().trim();
        if(strFileOwner.isEmpty()) {
            strFileOwner = interInfo.getDefaultServerInfo().getServerName();
        }

        switch(fAppendBox.getSelectedIndex()) {
            case 0:
                byteFileAppendMode = CMInfo.FILE_DEFAULT;
                break;
            case 1:
                byteFileAppendMode = CMInfo.FILE_OVERWRITE;
                break;
            case 2:
                byteFileAppendMode = CMInfo.FILE_APPEND;
                break;
        }

        ret = m_clientStub.requestFile(strFileName, strFileOwner, byteFileAppendMode);
        if(!ret) printMessage("An error occurred for the file request: [" + strFileOwner + "] - " + strFileName + "\n");
        printMessage("----------------------------------");
    }
    private void pushFile() {
        String strFilePath = null;
        File[] files = null;
        String strReceiver = null;
        byte byteFileAppendMode = -1;
        CMInteractionInfo interInfo = m_clientStub.getCMInfo().getInteractionInfo();
        boolean ret = false;

        printMessage("---------- File Upload ----------\n\n");

        JTextField freceiverField = new JTextField();
        String[] fAppendMode = {"Default", "Overwrite", "Append"};
        JComboBox<String> fAppendBox = new JComboBox<String>(fAppendMode);

        Object[] message = {
                "File Receiver(empty for default server): ", freceiverField,
                "File Append Mode: ", fAppendBox
        };
        int option = JOptionPane.showConfirmDialog(null, message, "File Push", JOptionPane.OK_CANCEL_OPTION);
        if(option != JOptionPane.OK_OPTION) {
            printMessage("canceled.\n");
            return;
        }

        strReceiver = freceiverField.getText().trim();
        if(strReceiver.isEmpty()) strReceiver = interInfo.getDefaultServerInfo().getServerName();

        switch(fAppendBox.getSelectedIndex())
        {
            case 0:
                byteFileAppendMode = CMInfo.FILE_DEFAULT;
                break;
            case 1:
                byteFileAppendMode = CMInfo.FILE_OVERWRITE;
                break;
            case 2:
                byteFileAppendMode = CMInfo.FILE_APPEND;
                break;
        }

        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);

        CMConfigurationInfo confInfo = m_clientStub.getCMInfo().getConfigurationInfo();
        File curDir = new File(confInfo.getTransferedFileHome().toString());
        fc.setCurrentDirectory(curDir);
        int fcRet = fc.showOpenDialog(this);
        if(fcRet != JFileChooser.APPROVE_OPTION) return;

        files = fc.getSelectedFiles();
        if(files.length < 1) return;

        for(int i=0; i < files.length; i++) {
            strFilePath = files[i].getPath();
            ret = m_clientStub.pushFile(strFilePath, strReceiver, byteFileAppendMode);
            if(!ret) {
                printMessage("push file error! file("+strFilePath+"), receiver(" +strReceiver+")\n");
            }
        }
        printMessage("----------------------------------");
    }

    private void shareFile() {

    }

    public void printCurrentFileInfo() {
        CMFileTransferInfo fInfo = m_clientStub.getCMInfo().getFileTransferInfo();
        Hashtable<String, CMList<CMSendFileInfo>> sendHashtable = fInfo.getSendFileHashtable();
        Hashtable<String, CMList<CMRecvFileInfo>> recvHashtable = fInfo.getRecvFileHashtable();
        Set<String> sendKeySet = sendHashtable.keySet();
        Set<String> recvKeySet = recvHashtable.keySet();

        printMessage("====== sending file info ======\n");
        for(String receiver : sendKeySet)
        {
            CMList<CMSendFileInfo> sendList = sendHashtable.get(receiver);
            printMessage(sendList+"\n");
        }

        printMessage("====== receiving file info ======\n");
        for(String sender : recvKeySet)
        {
            CMList<CMRecvFileInfo> recvList = recvHashtable.get(sender);
            printMessage(recvList+"\n");
        }
    }

    //Only called when logged-in
    public void getFileDir() {
        clearFileMessage();
        String username = m_clientStub.getCMInfo().getInteractionInfo().getMyself().getName();
        File dir = new File("..\\CDS2023_TermProject\\client-file-path\\" + username);
        //File files[] = dir.listFiles();
        String filenames[] = dir.list();

        if(filenames != null) {
            for(int i = 0; i < filenames.length; i++) {
                printFileMessage(filenames[i] + "\n");
            }
        } else printFileMessage("Folder empty.");
    }

    public void startWatchService() throws IOException {
        int nClientState = m_clientStub.getCMInfo().getInteractionInfo().getMyself().getState();

        if(nClientState != CMInfo.CM_LOGIN && nClientState != CMInfo.CM_SESSION_JOIN) {
            printMessage("WatchService currently unavailable: Client is not logged in!\n");
            return;
        }

        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path path = Paths.get(user_path);
        path.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.OVERFLOW);

        Thread thread = new Thread(() -> {
            while(true) {
                int currentState = m_clientStub.getCMInfo().getInteractionInfo().getMyself().getState();
                if(currentState != CMInfo.CM_LOGIN && currentState != CMInfo.CM_SESSION_JOIN) break;

                try {
                    watchkey = watchService.take(); // wait for an event
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }

                List<WatchEvent<?>> events = watchkey.pollEvents(); // get events
                for(WatchEvent<?> event: events) {
                    Kind<?> kind = event.kind();
                    Path paths = (Path)event.context();

                    if(kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                        printMessage("Created a file " + paths.getFileName() + " in the directory\n");
                        getFileDir();
                    } else if(kind.equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
                        printMessage("Modified the file " + paths.getFileName() + " in the directory\n");
                        getFileDir();
                    } else if(kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
                        printMessage("Deleted the file " + paths.getFileName() + " in the directory\n");
                        getFileDir();
                    } else {
                        printMessage("Something wrong in WatchService.\n");
                    }
                }

                if(!watchkey.reset()) {
                    try {
                        watchService.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    public class MyKeyListener implements KeyListener {
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();
            if(key == KeyEvent.VK_ENTER) {
                JTextField input = (JTextField)e.getSource();
                String strText = input.getText();
                printMessage(strText + "\n");
                processInput(strText);
                input.setText("");
                input.requestFocus();
            }
        }
        public void keyTyped(KeyEvent e) {}
        public void keyReleased(KeyEvent e) {}
    }

    public class MyMouseListener implements MouseListener {
        @Override
        public void mouseClicked(MouseEvent e) {
            if(e.getSource() instanceof JLabel)
            {
                JLabel pathLabel = (JLabel)e.getSource();
                String strPath = pathLabel.getText();
                File fPath = new File(strPath);
                try {
                    int index = strPath.lastIndexOf(File.separator);
                    String strFileName = strPath.substring(index+1, strPath.length());
                    if(fPath.exists())
                    {
                        accessAttachedFile(strFileName);
                        Desktop.getDesktop().open(fPath);
                    }
                    else
                    {
                        requestAttachedFile(strFileName);
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        @Override
        public void mouseEntered(MouseEvent e) {
            if(e.getSource() instanceof JLabel) {
                Cursor cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
                setCursor(cursor);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if(e.getSource() instanceof JLabel) {
                Cursor cursor = Cursor.getDefaultCursor();
                setCursor(cursor);
            }
        }
        @Override
        public void mousePressed(MouseEvent e) { }

        @Override
        public void mouseReleased(MouseEvent e) { }
    }

    public class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            JButton button = (JButton)e.getSource();
            if(button.getText().equals("Start CM Client")) startCM();
            else if(button.getText().equals("Stop CM Client")) stopCM();
            else if(button.getText().equals("Login")) requestLogin();
            else if(button.getText().equals("Logout")) requestLogout();
            else if(button.getText().equals("File Upload")) pushFile();
            else if(button.getText().equals("File Share")) shareFile();
            else if(button.getText().equals("Refresh")) getFileDir();
        }
    }

    public static void main(String[] args) {

        CMClientApp client = new CMClientApp();
        CMClientStub clientStub = client.getClientStub();
        clientStub.setAppEventHandler(client.getClientEventHandler());
    }
}
