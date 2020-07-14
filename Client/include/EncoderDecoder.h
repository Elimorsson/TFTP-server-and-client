#ifndef ENCODERDECODER_H
#define ENCODERDECODER_H

#include <string>
#include <vector>
#include <iostream>
#include <boost/lexical_cast.hpp>
using std::string;
using std::vector;


class EncoderDecoder{

private:

    vector<char> bytes;

    int zeroCounter = 0;

    int byteCounter = 0;

    short opCode = -1;

    short messageOpCode = -1;

    string userNameList;

    short numOfUsers = 0;


public:
    EncoderDecoder();

    string decode(string msg);

    const string encode (string message);

    void shortToBytes(short num, vector<char> &bytesArr);

    short bytesToShort(vector<char> bytesArr);

    string decodeNextByte(char* nextByte, string& result);

    string prepareToPrint (string message);

    void resetDecoder();


};









#endif //ENCODERDECODER_H
