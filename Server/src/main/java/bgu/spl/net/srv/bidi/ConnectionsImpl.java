package bgu.spl.net.srv.bidi;

import bgu.spl.net.api.bidi.Connections;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsImpl<T> implements Connections<T> {

    //------------------------fields---------------
    private ConcurrentHashMap<Integer, ConnectionHandler<T>> theMap;


    public ConnectionsImpl(){
        theMap = new ConcurrentHashMap<>();
    }

    public void addClient (ConnectionHandler<T> CH, int id){
        theMap.put(id,CH);
    }

    @Override
    public boolean send(int connectionId, T msg) {
        if (theMap.containsKey(connectionId)) {
            theMap.get(connectionId).send(msg);
            return true;
        }
        return false;
    }

    @Override
    public void broadcast(T msg) {

        for (ConnectionHandler<T> CH: theMap.values()) {
            CH.send(msg);
        }
    }

    @Override
    public void disconnect(int connectionId) {
        ConnectionHandler<T> CH = theMap.remove(connectionId);
        try {
            CH.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
