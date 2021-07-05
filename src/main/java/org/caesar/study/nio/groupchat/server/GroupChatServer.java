package org.caesar.study.nio.groupchat.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

public class GroupChatServer {
    private Selector selector;
    private ServerSocketChannel listenChannel;
    private static final int PORT = 6667;

    GroupChatServer(){
        try{
            //得到选择器
            selector = Selector.open();
            //ServerSocketChannel
            listenChannel = ServerSocketChannel.open();
            //绑定端口
            listenChannel.socket().bind(new InetSocketAddress(PORT));
            //设置非阻塞模式
            listenChannel.configureBlocking(false);
            //将listenChannel注册到selector
            listenChannel.register(selector, SelectionKey.OP_ACCEPT);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * 监听
     */
    public void listen(){
        try {
            while (true){
                int count = selector.select();
                if(count > 0){
                    //有事件处理
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        //取出selectionKey
                        SelectionKey key = iterator.next();

                        //监听到accept
                        if (key.isAcceptable()) {
                            SocketChannel socketChannel = listenChannel.accept();
                            socketChannel.configureBlocking(false);
                            //将socketChannel注册到selector
                            socketChannel.register(selector,SelectionKey.OP_READ);
                            System.out.println(socketChannel.getRemoteAddress()+"上线");
                        }
                        if (key.isReadable()) {
                            //通道发送read事件
                            readData(key);
                        }
                        //当前key删除，防止重复处理
                        iterator.remove();
                    }
                }else {
                    System.out.println("等待....");
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取客户端消息
     * @param key 对应的SelectionKey
     */
    private void readData(SelectionKey key){
        SocketChannel channel = (SocketChannel) key.channel();
        try{
            ByteBuffer buffer = ByteBuffer.allocate(1023);
            int read = channel.read(buffer);
            if (read > 0) {
                //将缓冲区的数据转成字符串
                String msg = new String(buffer.array());
                //输出消息
                System.out.println("from 客户端："+ msg);

                //转发消息
                sendMessageToOtherClients(msg,channel);
            }
        }catch (IOException e) {
            try {
                System.out.println(channel.getRemoteAddress()+ "离线了");
                channel.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }


    }

    /**
     * 转发消息
     * @param msg 消息
     * @param self 发消息的channel
     */
    private void sendMessageToOtherClients(String msg,SocketChannel self) throws IOException {
        for (SelectionKey key : selector.keys()) {
            Channel targetChannel = key.channel();

            //排除自己
            if (targetChannel instanceof SocketChannel && targetChannel != self) {
                SocketChannel dest = (SocketChannel)targetChannel;
                //将msg存储到buffer
                ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
                //将buffer写入通道
                dest.write(buffer);
            }
        }
    }


    public static void main(String[] args) {
        GroupChatServer groupChatServer = new GroupChatServer();
        groupChatServer.listen();
    }
}
