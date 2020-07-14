package bgu.spl.net.api.bidi;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class UserData {

    private String userName;
    private String password;
    private Boolean loggedIn = false;
    private ConcurrentLinkedQueue<String> following = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<String> followers = new ConcurrentLinkedQueue<>();
    private int postCount = 0;
    private ConcurrentLinkedQueue<String[]> msgQueue = new ConcurrentLinkedQueue<>();

    public UserData(String userName, String password){
        this.userName = userName;
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }


    public Boolean getLoggedIn() {
        return loggedIn;
    }

    public ConcurrentLinkedQueue<String> getFollowing() {
        return following;
    }

    public ConcurrentLinkedQueue<String> getFollowers() {
        return followers;
    }

    public int getPostCount() {
        return postCount;
    }

    public void increasePostCount (){
        postCount++;
    }

    public void setFollowing(String toAdd) {
        following.add(toAdd);
    }

    public void setFollower(String toAdd){
        followers.add(toAdd);
    }

    public void removeFollowing(String toRemove){
        following.remove(toRemove);
    }

    public void removeFollower(String toRemove){
        followers.remove(toRemove);
    }

    public void setLoggedIn(Boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public ConcurrentLinkedQueue <String[]> getMsgQueue(){ return msgQueue;}

    public void addNotification (String msg[]){ msgQueue.add(msg); }


}

