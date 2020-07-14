#include <stdlib.h>
#include <connectionHandler.h>
#include <iostream>
#include <mutex>
#include <thread>
#include <condition_variable>

using std::string;
using std::vector;
using std::thread;


std::mutex mutexLock;
std::condition_variable conditionVariable;
std::unique_lock<std::mutex> lock(mutexLock);
bool terminateSocket = false;


class KeyboardTask{
private:
    ConnectionHandler* connectionHandler;
public:
    KeyboardTask (ConnectionHandler* connectionHandler) : connectionHandler(connectionHandler) {}

    void run() {
        while (!terminateSocket) {
            const short bufsize = 1024;
            char buf[bufsize];
            std::cout<< "waiting for input -> come on Do it"<<std::endl;
            std::cin.getline(buf, bufsize);
            std::string line(buf);
            if (!connectionHandler->sendLine(line)) {
                std::cout << "Disconnected. Exiting...\n" << std::endl;
            }
            if (line == "LOGOUT" || line == "REGISTER") {
                conditionVariable.wait(lock);
            }
        }
    }
};


class SocketTask{
private:
    ConnectionHandler* connectionHandler;
public:
    SocketTask (ConnectionHandler* connectionHandler) : connectionHandler(connectionHandler) {}

    void run(){
        while (!terminateSocket)
        {
            std::string answer;
            // Get back an answer: by using the expected number of bytes (len bytes + newline delimiter)
            // We could also use: connectionHandler.getline(answer) and then get the answer without the newline char at the end
            if (connectionHandler->getLine(answer)){}
            else
                std::cout << "Disconnected. Exiting...\n" << std::endl;

            if (connectionHandler->AskForTerminate())
                terminateSocket = true;

            conditionVariable.notify_all();
        }
    }
};




/**
* This code assumes that the server replies the exact text the client sent it (as opposed to the practical session example)
*/
int main (int argc, char *argv[]) {
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }
    std::string host = argv[1];
    short port = atoi(argv[2]);
    
    ConnectionHandler connectionHandler(host, port);
    if (!connectionHandler.connect()) {
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
        return 1;
    }


    KeyboardTask task1(&connectionHandler);
    SocketTask task2(&connectionHandler);

    thread thread1(&KeyboardTask::run, &task1);
    thread thread2(&SocketTask::run, &task2);

    thread1.join();
    thread2.join();
    connectionHandler.close();
    return 0;

//
//	//From here we will see the rest of the ehco client implementation:
//    while (1) {
//        const short bufsize = 1024;
//        char buf[bufsize];
//        std::cin.getline(buf, bufsize);
//		std::string line(buf);
//		int len=line.length();
//        if (!connectionHandler.sendLine(line)) {
//            std::cout << "Disconnected. Exiting...\n" << std::endl;
//            break;
//        }
//		// connectionHandler.sendLine(line) appends '\n' to the message. Therefor we send len+1 bytes.
//        std::cout << "Sent " << len+1 << " bytes to server" << std::endl;
//
//
//        // We can use one of three options to read data from the server:
//        // 1. Read a fixed number of characters
//        // 2. Read a line (up to the newline character using the getline() buffered reader
//        // 3. Read up to the null character
//        std::string answer;
//        // Get back an answer: by using the expected number of bytes (len bytes + newline delimiter)
//        // We could also use: connectionHandler.getline(answer) and then get the answer without the newline char at the end
//        if (!connectionHandler.getLine(answer)) {
//            std::cout << "Disconnected. Exiting...\n" << std::endl;
//            break;
//        }
//
//		len=answer.length();
//		// A C string must end with a 0 char delimiter.  When we filled the answer buffer from the socket
//		// we filled up to the \n char - we must make sure now that a 0 char is also present. So we truncate last character.
//        answer.resize(len-1);
//        std::cout << "Reply: " << answer << " " << len << " bytes " << std::endl << std::endl;
//        if (answer == "bye") {
//            std::cout << "Exiting...\n" << std::endl;
//            break;
//        }
//    }
//    return 0;
}
