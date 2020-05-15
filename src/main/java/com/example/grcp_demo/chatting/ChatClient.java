package com.example.grcp_demo.chatting;

import com.example.grcp_demo.chatting.Chat.ChatMessage;
import com.example.grcp_demo.chatting.ChatRoomGrpc.ChatRoomStub;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class ChatClient {
    private final ChatRoomStub asyncStub;
    private final String user;

    private StreamObserver<ChatMessage> outStream;

    public ChatClient(String user, String host, int port) {
        this.user = user;
        Channel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        this.asyncStub = ChatRoomGrpc.newStub(channel);
        this.connect();
    }

    public void send(String message) {
        if (this.outStream == null)
            throw new RuntimeException("Chat disconnected!!!");

        this.outStream.onNext(ChatMessage.newBuilder().setType(this.user).setMessage(message).build());
    }

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("h", "host", true, "The host. Default: 0.0.0.0");
        options.addOption("p", "port", true, "The port. Default: 8981");
        options.addOption("u", "user", true, "User name. Default: Guest");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);

            ChatClient client = new ChatClient(cmd.getOptionValue("u", "Guest"), cmd.getOptionValue("h", "0.0.0.0"), Integer.parseInt(cmd.getOptionValue("p", "8980")));
       
            while (true) {
                String msg = System.console().readLine("Message: ");
                if (msg.equals("exit()"))
                    break;
                client.send(msg);
            }
    
            client.outStream.onCompleted();
        } catch (ParseException e) {
            System.out.println( "Unexpected exception:" + e.getMessage() );
        }
    }

    private void connect() {
        this.outStream = this.asyncStub.chatStream(new StreamObserver<Chat.ChatMessage>(){
        
            @Override
            public void onNext(ChatMessage value) {
                System.out.println(String.format("[%s]: %s", value.getType(), value.getMessage()));
            }
        
            @Override
            public void onError(Throwable t) {
                
            }
        
            @Override
            public void onCompleted() {
                if (ChatClient.this.outStream != null) {
                    ChatClient.this.outStream.onCompleted();
                }
            }
        });
    }
}