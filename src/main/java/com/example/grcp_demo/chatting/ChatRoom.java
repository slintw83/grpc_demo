package com.example.grcp_demo.chatting;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.example.grcp_demo.chatting.ChatRoomGrpc;
import com.example.grcp_demo.chatting.Chat.ChatMessage;
import com.example.grcp_demo.chatting.Chat.Empty;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

public class ChatRoom {
    private final Server server;
    private final ChatServer chatServer;

    public ChatRoom(int port) {
        this.chatServer = new ChatServer();
        this.server = ServerBuilder.forPort(port).addService(this.chatServer).build();
    }

    public void waitForConnection() throws InterruptedException {
        System.out.println("Waiting for client connection.");
        this.chatServer.countDownLatch.await();
        System.out.println("Client connected.");
    }

    public void start() throws IOException {
        this.server.start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    ChatRoom.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void stop() throws InterruptedException {
        if (this.server != null) {
            this.server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (this.server != null) {
            this.server.awaitTermination();
        }
    }

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("p", "port", true, "The port. Default: 8980");
        options.addOption("u", "user", true, "User name. Default: Admin");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);

            String user = cmd.getOptionValue("u", "Admin");
            ChatRoom room = new ChatRoom(Integer.parseInt(cmd.getOptionValue("p", "8980")));
            room.start();
            room.waitForConnection();

            while (true) {
                String msg = System.console().readLine("Message: ");
                if (msg.equals("exit()"))
                    break;
                room.chatServer.send(user, msg);
            }

            room.blockUntilShutdown();
        }
        catch (ParseException e) {
            System.err.println( "Unexpected exception:" + e.getMessage() );
        }
        catch (InterruptedException e) {
            System.err.println("Client could not connect.");
        }        
    }
    
    private static class ChatServer extends ChatRoomGrpc.ChatRoomImplBase {
        private StreamObserver<ChatMessage> inStream;
        public CountDownLatch countDownLatch = new CountDownLatch(1);

        @Override
        public StreamObserver<ChatMessage> chatStream(StreamObserver<ChatMessage> responseObserver) {
            this.countDownLatch.countDown();
            this.inStream = responseObserver;
            return new StreamObserver<Chat.ChatMessage>() {

                @Override
                public void onNext(ChatMessage value) {
                    System.out.println(String.format("[%s]: %s", value.getType(), value.getMessage()));
                }

                @Override
                public void onError(Throwable t) {
                    System.err.println("Client disconnected!");
                }

                @Override
                public void onCompleted() {
                    System.out.println("Client Exited!");
                }
            };
        }

        @Override
        public void listen(Empty request, StreamObserver<ChatMessage> responseObserver) {
            // TODO Auto-generated method stub
            super.listen(request, responseObserver);
        }

        @Override
        public StreamObserver<ChatMessage> onlySend(StreamObserver<Empty> responseObserver) {
            // TODO Auto-generated method stub
            return super.onlySend(responseObserver);
        }

        public void send(String user, String message) {
            this.inStream.onNext(ChatMessage.newBuilder().setType(user).setMessage(message).build());
        }
    }
}