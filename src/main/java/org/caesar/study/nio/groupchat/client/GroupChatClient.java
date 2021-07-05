package org.caesar.study.nio.groupchat.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GroupChatClient {
    //服务器地址
    private final String HOST = "127.0.0.1";
    //服务器端口
    private final int PORT = 6667;
    private Selector selector;
    private SocketChannel socketChannel;
    private String userName;

    GroupChatClient(){
        try {
            selector = Selector.open();

            socketChannel = SocketChannel.open(new InetSocketAddress(HOST,PORT));

            socketChannel.configureBlocking(false);

            socketChannel.register(selector, SelectionKey.OP_READ);

            userName = socketChannel.getLocalAddress().toString().substring(1);

            System.out.println(userName + " is ok...");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String msg){
        msg = userName + "说：" + msg;

        try {
            socketChannel.write(ByteBuffer.wrap(msg.getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readMessage(){
        try {
            int read = selector.select();
            if(read > 0){
                //有可用通道
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    if (key.isReadable()) {
                        SocketChannel channel = (SocketChannel)key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        socketChannel.read(buffer);

                        String msg = new String(buffer.array());
                        System.out.println(msg.trim());
                    }
                }

            }else{
                //System.out.println("没有可用的通道");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        GroupChatClient groupChatClient = new GroupChatClient();

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.execute(()->{
            while (true) {
                groupChatClient.readMessage();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        });

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()){
            String msg = scanner.nextLine();
            groupChatClient.sendMessage(msg);
        }
    }
}
