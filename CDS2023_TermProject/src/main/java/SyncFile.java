import java.util.ArrayList;

public class SyncFile {
    public ArrayList<String> sharedUsers;
    public int lclock;

    public String fileName;
    public long fileSize;

    public SyncFile(String name, long size, String owner) {
        sharedUsers = new ArrayList<String>();
        sharedUsers.add(owner);
        lclock = 0;
        fileName = name;
        fileSize = size;
    }

    public SyncFile(String name, long size, int lc, String[] users) {
        sharedUsers = new ArrayList<String>();
        for(String u: users) {
            sharedUsers.add(u);
        }
        lclock = lc;
        fileName = name;
        fileSize = size;
    }

    // should be called before synchronization
    public void updateFile() {
        lclock++;
    }

    public void shareFile(String usr) {
        if(!sharedUsers.contains(usr)) sharedUsers.add(usr);
    }

    public void updateSize(long size) {
        fileSize = size;
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

    public String[] getRowValue() {
        String usrs = "";

        for(String u: sharedUsers) {
            usrs += u + ", ";
        }
        usrs = usrs.substring(0, usrs.length()-2);

        String[] ret = { fileName, String.valueOf(fileSize), String.valueOf(lclock), usrs };
        return ret;
    }
}