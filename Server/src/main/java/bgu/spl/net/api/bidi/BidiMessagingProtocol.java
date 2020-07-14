package bgu.spl.net.api.bidi;

import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.api.bidi.Connections;

import java.util.function.Supplier;

public interface BidiMessagingProtocol<T> {
	/**
	 * Used to initiate the current client protocol with it's personal connection ID and the connections implementation
	**/
    void start(int connectionId, Connections<T> connections);
    
    void process(T message);
	
	/**
     * @return true if the connection should be terminated
     */
    boolean shouldTerminate();
}
