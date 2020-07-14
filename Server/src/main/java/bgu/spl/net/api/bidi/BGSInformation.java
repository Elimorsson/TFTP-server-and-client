package bgu.spl.net.api.bidi;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class BGSInformation {

    /*
        map for registers users :
        key - is String of the userName
        value - is the UserData object
     */
    public ConcurrentHashMap<String,UserData> usersMap = new ConcurrentHashMap<>();
    /*
        map for logged users :
        key - is String of the userName
        value - is int of the clientId
     */
    public ConcurrentHashMap<String,Integer> loggedInUsersMap = new ConcurrentHashMap<>();


    public Vector <String> postsList = new Vector<>();
}
