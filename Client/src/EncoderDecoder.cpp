#include "../include/EncoderDecoder.h"
#include <string>
#include<sstream>
#include <iostream>
#include <EncoderDecoder.h>
#include <boost/algorithm/string.hpp>
#include <boost/algorithm/string/classification.hpp>

using namespace std;
using std::string;
using std::vector;
using boost::lexical_cast;
using boost::bad_lexical_cast;




EncoderDecoder::EncoderDecoder() {}

const string EncoderDecoder::encode(string message) {


    string opString;
    vector<char> bytesAnswer;
    string restMessage;
    short opShort;

    //check what is the first word to know the opCode
    for (char ch: message) {
        if (ch != ' ')
            opString = opString + ch;
        else{
            restMessage = message.substr(opString.length() + 1);
            break;
        }
    }




    if (opString == "REGISTER" || opString == "LOGIN") {
        if (opString == "REGISTER")
            opShort = 1;
        if (opString == "LOGIN")
            opShort = 2;

        shortToBytes(opShort, bytesAnswer);

        for (char ch: restMessage) {
            if (ch == ' ')
                ch = '\0';
            bytesAnswer.push_back(ch);
        }
        bytesAnswer.push_back('\0');
    }

    if (opString == "LOGOUT" )
    {
        opShort = 3;
        shortToBytes(opShort, bytesAnswer);
    }
    if (opString == "USERLIST") {
        opShort = 7;
        shortToBytes(opShort, bytesAnswer);
    }

    if (opString == "FOLLOW"){
        opShort = 4;
        shortToBytes(opShort, bytesAnswer);
        string numOfUsers;
        char follow = restMessage.at(0);
        bytesAnswer.push_back(follow);

        for (unsigned int i = 2; i < restMessage.length(); i++) {

            if (restMessage.at(i) != ' ')
                numOfUsers += restMessage.at(i);
            else {
                short num = stoi(numOfUsers);
                shortToBytes(num, bytesAnswer);
                restMessage = restMessage.substr(i+1);
                break;
            }
        }
            for (char ch : restMessage){
                if (ch == ' ')
                    ch = '\0';
                bytesAnswer.push_back(ch);
            }
        bytesAnswer.push_back('\0');
    }

    if (opString == "POST" || opString ==  "STAT") {
        if (opString == "POST")
            opShort = 5;
        if (opString == "STAT")
            opShort = 8;
        shortToBytes(opShort, bytesAnswer);

        for (char ch: restMessage)
            bytesAnswer.push_back(ch);
        bytesAnswer.push_back('\0');
    }

    if (opString == "PM")
    {
        opShort = 6;
        shortToBytes(opShort, bytesAnswer);
        bool foundZero = false;
        for (char ch: restMessage) {
            if (ch == ' ' && !foundZero) {
                ch = '\0';
                foundZero = true;
            }
            bytesAnswer.push_back(ch);
        }
        bytesAnswer.push_back('\0');
    }

    string returnValue;
    for (char ch: bytesAnswer)
        returnValue = returnValue + ch;
    return  returnValue;

}

string EncoderDecoder::decodeNextByte(char *nextByte, string &result) {

    bytes.push_back(*nextByte);
    byteCounter++;

    if (*nextByte == '\0')
        zeroCounter++;

    if (byteCounter == 3) {
        vector<char> tempBytes;
        tempBytes.push_back(bytes[0]);
        tempBytes.push_back(bytes[1]);
        opCode = bytesToShort(tempBytes);
    }

    if (opCode == 9) {
        if (byteCounter == 3) zeroCounter = 0;
        if (zeroCounter == 2){
            char type(bytes.at(2));
            string toReturn = to_string(opCode) + '\0' + type + '\0';
            for (unsigned int i = 3; i < bytes.size(); i++)
                toReturn = toReturn + bytes.at(i);
            return toReturn;
        }
    }

    if (opCode == 10) {
        if (byteCounter == 4) {
            vector<char> tempBytes;
            tempBytes.push_back(bytes.at(2));
            tempBytes.push_back(bytes.at(3));
            messageOpCode = bytesToShort(tempBytes);
        }

        if (messageOpCode == 1 || messageOpCode == 2 || messageOpCode == 3 || messageOpCode == 5 ||
            messageOpCode == 6) {
            string toReturn = to_string(opCode) + '\0' + to_string(messageOpCode) + '\0';
            return toReturn;
        }

        if (messageOpCode == 4 || messageOpCode == 7) {
            if (byteCounter == 6) {
                vector<char> tempBytes;
                tempBytes.push_back(bytes.at(4));
                tempBytes.push_back(bytes.at(5));
                numOfUsers = bytesToShort(tempBytes);
                zeroCounter = 0;
            }
            if (byteCounter >= 7 && zeroCounter < numOfUsers)
                userNameList = userNameList + nextByte[0];

            if (zeroCounter == numOfUsers) {
                string toReturn =
                        to_string(opCode) + '\0' + to_string(messageOpCode) + '\0' + to_string(numOfUsers) + '\0' +
                        userNameList;
                return toReturn;
            }
        }

        if (messageOpCode == 8) {
            short numPosts;
            short numFollowers;
            short numFollowing;
            if (byteCounter == 10) {
                vector<char> tempBytes;
                tempBytes.push_back(bytes.at(4));
                tempBytes.push_back(bytes.at(5));
                numPosts = bytesToShort(tempBytes);
                tempBytes.clear();
                tempBytes.push_back(bytes.at(6));
                tempBytes.push_back(bytes.at(7));
                numFollowers = bytesToShort(tempBytes);
                tempBytes.clear();
                tempBytes.push_back(bytes.at(8));
                tempBytes.push_back(bytes.at(9));
                numFollowing = bytesToShort(tempBytes);

                string toReturn =
                        to_string(opCode) + '\0' + to_string(messageOpCode) + '\0' + to_string(numPosts) + '\0' +
                        to_string(numFollowers) + '\0' + to_string(numFollowing) + '\0';
                return toReturn;
            }
        }
        return "";
    }
    if (opCode == 11)
    {
        if (byteCounter == 4) {
            vector<char> tempBytes;
            tempBytes.push_back(bytes.at(2));
            tempBytes.push_back(bytes.at(3));
            messageOpCode = bytesToShort(tempBytes);

            return to_string(opCode) + '\0' + std::to_string(messageOpCode) + '\0';
        }

    }
    return "";
}


//helper methods
string EncoderDecoder::prepareToPrint(string message) {

    vector<string> theSplit;
    boost::split(theSplit, message, [](char c){return c == '\0';});

    if (stoi(theSplit[0]) == 9) {
        string command = "NOTIFICATION ";
        string type;
        if(stoi(theSplit[1]) == 0)
            type = "PM ";
        else
            type = "PUBLIC ";

        string postingUser = theSplit[2];
        string content = theSplit[3];

        return command + type + postingUser + " " + content;
    }

    if (stoi(theSplit[0]) == 10){
        string command = "ACK ";
        string msgOpCode = theSplit[1];
        if (msgOpCode == "1" || msgOpCode == "2" || msgOpCode == "3" || msgOpCode == "5" || msgOpCode == "6")
            return command + msgOpCode;
        if (msgOpCode == "4" || msgOpCode == "7"){
            string numOfUsers = theSplit[2];
            string userNameList;
            for (unsigned int i = 3; i < theSplit.size(); i++)
                userNameList = userNameList + theSplit[i] + " ";

            return command + msgOpCode + " " + numOfUsers + " " + userNameList;
        }
        if (msgOpCode == "8"){
            string numPosts = theSplit[2];
            string numFollowers = theSplit[3];
            string numFollowing = theSplit[4];
            return command + " " + msgOpCode + " " + numPosts + " " +numFollowers + " " + numFollowing;
        }
    }
    if (stoi(theSplit[0]) == 11) {
        string command = "ERROR ";
        string opError = theSplit[1];
        return command + opError;
    }
    return "";
}

void EncoderDecoder::resetDecoder() {
    bytes.clear();
    zeroCounter = 0;
    byteCounter = 0;
    opCode = -1;
    messageOpCode = -1;
    userNameList = "";
    numOfUsers = 0;
    numOfUsers = 0;
}


short EncoderDecoder::bytesToShort(vector<char> bytesArr)
{
    short result = (short)((bytesArr[0] & 0xff) << 8);
    result += (short)(bytesArr[1] & 0xff);
    return result;
}

void EncoderDecoder::shortToBytes(short num, vector<char> &bytesArr)
{
    bytesArr.push_back((num >> 8) & 0xFF);
    bytesArr.push_back(num & 0xFF);
}
