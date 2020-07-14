package bgu.spl.net.api;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class EncoderDecoder implements MessageEncoderDecoder<String[]> {

    private byte[] bytes = new byte[(1 << 10)];
    private int len = 0;

    private byte[] opBytes = new byte[2];
    private byte[] helperByte = new byte[2];
    private int opIndex = 0;
    private short opCode = -1;
    private int zeroCount = 0;
    private int startCount = 0;
    private int follow;
    private int numOfUsers;
    private int len2 = 0;

    @Override
    public String[] decodeNextByte(byte nextByte) {

        if (len >= bytes.length)        //if there is no place to put new byte, increase the bytes array
            bytes = Arrays.copyOf(bytes, len * 2);

        if (opCode == -1) {     //if there is no opCode yet
            opBytes[opIndex] = nextByte;
            if (opIndex == 0) {
                opIndex++;
            } else {
                opCode = bytesToShort(opBytes);
                opIndex = 0;
                if (opCode == 3 || opCode == 7){
                    String[] toReturn = new String[1];
                    toReturn[0] = Integer.toString(opCode);
                    reset();
                    return toReturn;
                }
            }

        }
        else {
            switch (opCode) {
                case 1:
                case 2:
                case 6:
                    if (zeroCount < 2) {
                        if (nextByte == '\0') zeroCount++;
                        bytes[len++] = nextByte;
                    }
                    if (zeroCount == 2) {
                        String temp = new String(bytes, 0, len, StandardCharsets.UTF_8);
                        temp = Integer.toString(opCode) + '\0' + temp;
                        len = 0;
                        zeroCount = 0;
                        opCode = -1;
                        reset();
                        return temp.split("\0");
                    }
                    break;

//                case 3:
//                case 7:
//                    String[] toReturn = new String[1];
//                    toReturn[0] = Integer.toString(opCode);
//                    opCode = -1;
//                    return toReturn;

                case 4:
                    if (len < 3) {
                        bytes[len++] = nextByte;
                        if (startCount == 0) {
                            String f = new String (bytes,0,1,StandardCharsets.UTF_8);;
                            follow = Integer.parseInt(f);
                            startCount++;
                        }
                        else if (startCount == 1) {
                            startCount++;
                            helperByte[0] = nextByte;
                        }
                        else if (startCount == 2) {
                            helperByte[1] = nextByte;
                            numOfUsers = bytesToShort(helperByte);
                            startCount++;
                            bytes = new byte[(1 << 10)];
                        }
                    }
                    else{
                        bytes[len2++] = nextByte;
                        if (nextByte == '\0') zeroCount++;
                        if (zeroCount == numOfUsers) {
                            String users = new String(bytes, 0, len2, StandardCharsets.UTF_8);
                            String[] array = users.split("\0");
                            String[] array2 = new String[array.length + 3];
                            array2[0] = Integer.toString(opCode);
                            array2[1] = Integer.toString(follow);
                            array2[2] = String.valueOf(numOfUsers);
                            for (int i = 0; i < array.length; i++)
                                array2[i + 3] = array[i];
                            reset();
                            startCount = 0;
                            return array2;
                        }
                    }
                    break;

                case 5:
                case 8:
                    if (nextByte == '\0') zeroCount++;
                    if (zeroCount < 1) {
                        bytes[len++] = nextByte;
                    } else {
                        String[] result = {Integer.toString(opCode), new String(bytes, 0, len, StandardCharsets.UTF_8)};
                        len = 0;
                        zeroCount = 0;
                        opCode = -1;
                        reset();
                        return result;
                    }
                    break;
            }
        }
        return null;
    }


    @Override
    public byte[] encode(String[] message) {

        String opCode = message[0];
        byte[] opReturn = shortToBytes(Short.parseShort(opCode));

        switch (opCode) {
            case "9":
                String NotificationType = message[1];
                String PostingUser = message[2];
                String Content = message[3];

                byte[] restReturn = (NotificationType + PostingUser + '\0' + Content + '\0').getBytes();
                return Combined(opReturn, restReturn);


            case "10":
                String MessegeOpCode = message[1];
                byte[] MessageOpBytes = shortToBytes(Short.parseShort(MessegeOpCode));

                switch (MessegeOpCode) {
                    case "1":
                    case "2":
                    case "3":
                    case "5":
                    case "6":
                        return Combined(opReturn, MessageOpBytes);

                    case "4":
                    case "7":
                        short numOfUsers = Short.parseShort(message[2]);
                        byte[] numofUsers = shortToBytes(numOfUsers);
                        byte[] first = Combined(opReturn, MessageOpBytes);
                        byte[] toReturn = Combined(first, numofUsers);

                        for (int i = 3; i < numOfUsers + 3; i++) {
                            byte[] user = (message[i] + '\0').getBytes();
                            toReturn = Combined(toReturn, user);
                        }
                        return toReturn;

                    case "8":
                        byte[] NumPosts = shortToBytes(Short.parseShort(message[2]));
                        byte[] numFollowers = shortToBytes(Short.parseShort(message[3]));
                        byte[] numFollowing = shortToBytes(Short.parseShort(message[4]));
                        ;

                        byte[] first8 = Combined(opReturn, MessageOpBytes);
                        byte[] second8 = Combined(first8, NumPosts);
                        byte[] third8 = Combined(second8, numFollowers);
                        return Combined(third8, numFollowing);
                }

            case "11":
                String MessageOpcode = message[1];
                short MessageOp = Short.parseShort(MessageOpcode);
                byte[] restReturn11 = shortToBytes(MessageOp);
                byte[] combined11 = new byte[opReturn.length + restReturn11.length];

                for (int i = 0; i < combined11.length; ++i) {
                    combined11[i] = i < opReturn.length ? opReturn[i] : restReturn11[i - opReturn.length];
                }
                return combined11;
        }

        return null;

    }

    private byte[] Combined (byte[] one, byte[] two){
        byte[] combined = new byte[one.length + two.length];

        for (int i = 0; i < combined.length; ++i)
        {
            combined[i] = i < one.length ? one[i] : two[i - one.length];
        }
        return combined;
    }

    private String popString() {
        String result = new String(bytes, 0, len, StandardCharsets.UTF_8);
       result = opCode + result;
        len = 0;
        opCode = -1;
        zeroCount = 0;
        return result;
    }


    private short bytesToShort(byte[] byteArr)
    {
        short result = (short)((byteArr[0] & 0xff) << 8);
        result += (short)(byteArr[1] & 0xff);
        return result;
    }

    private byte[] shortToBytes(short num)
    {
        byte[] bytesArr = new byte[2];
        bytesArr[0] = (byte)((num >> 8) & 0xFF);
        bytesArr[1] = (byte)(num & 0xFF);
        return bytesArr;
    }

    private void reset(){
        bytes = new byte[(1 << 10)];
        helperByte = new byte[2];
        len = 0;
        opBytes = new byte[2];
        opIndex = 0;
        opCode = -1;
        zeroCount = 0;
        len2 = 0;
    }

}
