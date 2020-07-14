package bgu.spl.net.api.bidi;

import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.srv.bidi.ConnectionsImpl;
import org.omg.PortableInterceptor.INACTIVE;

import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class BGSProtocolImpl implements BidiMessagingProtocol<String[]> {

    /**
     * Used to initiate the current client protocol with it's personal connection ID and the connections implementation
     **/

    private ConnectionsImpl<String[]> connections;
    private int clientID;
    private BGSInformation bgsInformation;
    private UserData userData;
    private boolean terminate;

    public BGSProtocolImpl(BGSInformation bgsInformation){
        this.bgsInformation = bgsInformation;
    }

    @Override
    public void start(int connectionId, Connections<String[]> connections) {

        this.connections = (ConnectionsImpl<String[]>) connections;
        this.clientID = connectionId;
        this.terminate = false;
    }

    @Override
    public void process(String[] message) {

        int opCode = Integer.parseInt(message[0]); //get the first string that represent a opCode

        switch (opCode) {
            case 1:
                String userName = message[1];
                String password = message[2];
                synchronized (bgsInformation.usersMap) {
                    if (bgsInformation.usersMap.containsKey(userName)) {
                        String[] error1 = {Integer.toString(11), Integer.toString(opCode)};
                        connections.send(clientID, error1);
                        //return error message
                    } else {
                        UserData a = new UserData(userName, password);
                        bgsInformation.usersMap.put(userName, a);
                        String[] ackMsg1 = {Integer.toString(10), Integer.toString(opCode)};
                        connections.send(clientID, ackMsg1);
                    }
                }
                break;


            case 2:
                synchronized (bgsInformation.usersMap) {
                    String username2 = message[1];
                    String password2 = message[2];

                    if ((!bgsInformation.usersMap.containsKey(username2))
                            || (!bgsInformation.usersMap.get(username2).getPassword().equals(password2))
                            || (bgsInformation.usersMap.get(username2).getLoggedIn())
                            || userData != null) {
                        String[] error2 = {Integer.toString(11), Integer.toString(opCode)};
                        connections.send(clientID, error2);
                        //return error message
                    } else {
                        userData = bgsInformation.usersMap.get(username2);
                        userData.setLoggedIn(true);
                        bgsInformation.loggedInUsersMap.put(username2, clientID);
                        String[] ackMsg2 = {Integer.toString(10), Integer.toString(opCode)};
                        connections.send(clientID, ackMsg2);

                        while (!userData.getMsgQueue().isEmpty()) {
                            connections.send(clientID, userData.getMsgQueue().poll());
                        }
                    }
                }
                break;

            case 3:
                synchronized (bgsInformation.usersMap) {
                    String[] ackMsg3 = {Integer.toString(10), Integer.toString(opCode)};
                    if (userData != null) {
                        connections.send(clientID, ackMsg3);
                        userData.setLoggedIn(false);
                        bgsInformation.loggedInUsersMap.remove(userData.getUserName());
                        terminate = true;
                        userData = null;

                    } else {
                        String[] error3 = {Integer.toString(11), Integer.toString(opCode)};
                        connections.send(clientID, error3);
                    }
                }
                break;

            case 4:
                //get all the relevant Data for the act
                boolean toFollow = (message[1].equals("0"));
                int NumOfUsers = Integer.parseInt(message[2]);
                ConcurrentLinkedQueue<String> following = userData.getFollowing();
                String[] userNameList = new String[NumOfUsers];
                ConcurrentLinkedQueue<String> listToReturn = new ConcurrentLinkedQueue<>();
                String[] error4 = {Integer.toString(11), Integer.toString(opCode)};
                int len = 0;
                for (int i = 0; i < NumOfUsers; i++)
                    userNameList[i] = message[i+3];

                if ((!userData.getLoggedIn())) {
                    connections.send(clientID, error4);
                }
                else {
                    for (int i = 0; i < userNameList.length; i++) {
                        if (toFollow) {
                            if((bgsInformation.usersMap.containsKey(userNameList[i])) && (!following.contains(userNameList[i]))
                                && !userData.getUserName().equals(userNameList[i])) {
                                userData.setFollowing(userNameList[i]);
                                bgsInformation.usersMap.get(userNameList[i]).setFollower(userData.getUserName());
                                listToReturn.add(userNameList[i]);
                                len++;
                            }
                        }
                        else {
                            if ((bgsInformation.usersMap.containsKey(userNameList[i])) && following.contains(userNameList[i])
                                && !userData.getUserName().equals(userNameList[i])) {
                                userData.removeFollowing(userNameList[i]);
                                bgsInformation.usersMap.get(userNameList[i]).removeFollower(userData.getUserName());
                                listToReturn.add(userNameList[i]);
                                len++;
                            }
                        }
                    }
                    if (len == 0) {
                        connections.send(clientID, error4);
                    }
                    else {
                        String[] ackMsg4 = new String[listToReturn.size()+3];
                        ackMsg4[0] = Integer.toString(10);
                        ackMsg4[1] = Integer.toString(opCode);
                        ackMsg4[2] = Integer.toString(listToReturn.size());
                        int i = 3;
                        for (String user : listToReturn) {
                            ackMsg4[i++] = user;
                        }
                        connections.send(clientID, ackMsg4);
                    }
                }
                break;

            case 5:
                if (!userData.getLoggedIn()) {
                    String[] error5 = {Integer.toString(11), Integer.toString(opCode)};
                    connections.send(clientID, error5);
                }
                else {
                    String content = message[1];
                    int strudelInd;
                    int spaceInd;
                    Vector<String> postList = new Vector<>();
                    for (int i = 0; i < content.length(); i++) { //run all over the content and search the strudel character
                        if (content.charAt(i) == '@') {
                            strudelInd = i;
                            spaceInd = content.indexOf(' ', strudelInd);
                            if (spaceInd == -1)
                                spaceInd = content.length(); //if there is no space until the end of the content, put the last index

                            String user = content.substring(strudelInd + 1, spaceInd);
                            if (bgsInformation.usersMap.containsKey(user)
                                    && (!postList.contains(user)))
                                postList.add(user);
                        }
                    }
                    //add the followers users to the postList
                    ConcurrentLinkedQueue<String> followers = userData.getFollowers();
                    for (String follower : followers) {
                        if (!postList.contains(follower))
                            postList.add(follower);
                    }
                    String[] returnMsg5 = {Integer.toString(9), Integer.toString(1),userData.getUserName(),content};
                    for (String user : postList) {
                        //send a notification to recipient user
                        if (bgsInformation.usersMap.get(user).getLoggedIn()) {
                            int Id = bgsInformation.loggedInUsersMap.get(user);
                            connections.send(Id, returnMsg5);
                        }
                        else
                            bgsInformation.usersMap.get(user).addNotification(returnMsg5);
                    }
                    bgsInformation.postsList.add(content);
                    userData.increasePostCount();
                    String[] ackMsg5 = {Integer.toString(10), Integer.toString(opCode)};
                    connections.send(clientID,ackMsg5);
                }
                break;

            case 6:
                String recipient = message[1];
                String content6 = message[2];
                if (!userData.getLoggedIn() || !bgsInformation.usersMap.containsKey(recipient)) {
                    String[] error6 = {Integer.toString(11), Integer.toString(opCode)};
                    connections.send(clientID, error6);
                }
                else {
                    //send a notification to recipient user
                    String[] returnMsg6 = {Integer.toString(9), Integer.toString(0), userData.getUserName(),content6};

                    if (bgsInformation.usersMap.get(recipient).getLoggedIn())
                        connections.send(bgsInformation.loggedInUsersMap.get(recipient), returnMsg6);
                    else
                        bgsInformation.usersMap.get(recipient).addNotification(returnMsg6);

                    bgsInformation.postsList.add(content6);
                    String[] ackMsg6 = {Integer.toString(10), Integer.toString(opCode)};
                    connections.send(clientID,ackMsg6);
                }

                break;
            case 7:
                if (userData == null) {
                    String[] error7 = {Integer.toString(11), Integer.toString(opCode)};
                    connections.send(clientID, error7);
                }
                else {
                    String[] ack7 = new String[bgsInformation.usersMap.keySet().size() + 3];
                    ack7[0] = Integer.toString(10);
                    ack7[1] = Integer.toString(opCode);
                    ack7[2] = Integer.toString(bgsInformation.usersMap.size());
                    int i = 3;
                    for (String us: bgsInformation.usersMap.keySet()) {
                        ack7[i] = us;
                        i++;
                    }
                    connections.send(clientID, ack7);
                }

                break;
            case 8:
                if ((!userData.getLoggedIn()) || !(bgsInformation.usersMap.containsKey(message[1]))) {
                    String[] error8 ={Integer.toString(11), Integer.toString(opCode)};
                    connections.send(clientID, error8);
                }

                else {
                    UserData statUser = bgsInformation.usersMap.get(message[1]);
                    int numberOfPosts = statUser.getPostCount();
                    int numberOfFollowers = statUser.getFollowers().size();
                    int numberOfFollowing = statUser.getFollowing().size();

                    String[] ack8 = new String [5];
                    ack8[0] = Integer.toString(10);
                    ack8[1] = Integer.toString(opCode);
                    ack8[2] = Integer.toString(numberOfPosts);
                    ack8[3] = Integer.toString(numberOfFollowers);
                    ack8[4] = Integer.toString(numberOfFollowing);
                    connections.send(clientID,ack8);
                }
                break;

        }

    }

    @Override
    public boolean shouldTerminate() {
        return terminate;
    }
}
