CFLAGS:=-c -Wall -g -std=c++11 -Iinclude
LDFLAGS:=-lboost_system

all: EchoClient
	g++ -pthread -o bin/BGSclient bin/connectionHandler.o bin/BGSclient.o bin/EncoderDecoder.o $(LDFLAGS)

EchoClient: bin/connectionHandler.o bin/BGSclient.o bin/EncoderDecoder.o
	
bin/connectionHandler.o: src/connectionHandler.cpp
	g++ $(CFLAGS) -o bin/connectionHandler.o src/connectionHandler.cpp

bin/BGSclient.o: src/echoClient.cpp
	g++ $(CFLAGS) -o bin/BGSclient.o src/echoClient.cpp

bin/EncoderDecoder.o: src/EncoderDecoder.cpp
	g++ $(CFLAGS) -o bin/EncoderDecoder.o src/EncoderDecoder.cpp


.PHONY: clean
clean:
	rm -f bin/*
