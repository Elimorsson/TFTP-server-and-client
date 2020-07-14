package bgu.spl.net.impl.BGSServer;
import bgu.spl.net.api.EncoderDecoder;
import bgu.spl.net.api.bidi.BGSInformation;
import bgu.spl.net.api.bidi.BGSProtocolImpl;
import bgu.spl.net.srv.Server;


public class ReactorMain {



    public static void main (String[] args) {
        BGSInformation bgsInformation = new BGSInformation();
        Server.reactor(Integer.parseInt(args[1]),Integer.parseInt(args[0]),
                ()-> new BGSProtocolImpl(bgsInformation),
                ()-> new EncoderDecoder()).serve();
    }
}


