package InterAddress.Chat_System.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;

public class ServerReaderThread extends Thread{
    private Socket socket;
    public ServerReaderThread(Socket socket){
        this.socket = socket;
    }
    @Override
    public void run(){
        //数据发送协议
        //int 1 +String --登录信息
        //int 2 +String --群聊消息
        //注意此处所有处理消息都是服务端的
        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            while (true) {
                int type = dis.readInt(); //dis管道自动更新接收的数据
                switch (type){
                    case 1:
                        //接收登录信息，更新客户端内的用户列表
                        String userName = dis.readUTF();
                        //将用户信息保存到map中
                        ServerDemo.onLineSockets.put(socket,userName);
                        //更新用户列表
                        updateClientList();
                        break;
                    case 2:
                        //客户端发送数据，转发至所有客户端
                        String msg = dis.readUTF();
                        sendMsgToAll(msg);
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("客户端下线了："+socket.getInetAddress().getHostAddress()+" "+socket.getPort());
            ServerDemo.onLineSockets.remove(socket);//将下线的用户移除
            updateClientList();//更新所有用户列表
        }
    }

    private void sendMsgToAll(String msg) {
        //拼装信息(用户名+时间+内容)
        StringBuilder sb = new StringBuilder();
        //获取用户名
        String name = ServerDemo.onLineSockets.get(socket);
        //获取时间
        LocalDateTime now = LocalDateTime.now();//时间
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss EEE a");//创建格式
        String nowTime = dtf.format(now);

        String allMsg = sb.append(name).append(" ").append(nowTime)
                .append("\r\n").append(msg).toString();
        //遍历所有客户端,将打包信息送出
        for(Socket socket:ServerDemo.onLineSockets.keySet()){
            try {
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeInt(2);
                dos.writeUTF(allMsg);
                dos.flush();//刷新管道，确保数据发送出去
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateClientList() {
        //更新列表数据
        //拿到客户端的用户名，并传到所有客户端中
        Collection<String> onLineUsers = ServerDemo.onLineSockets.values();
        //遍历所有客户端
        for(Socket socket:ServerDemo.onLineSockets.keySet()){
            try {
                //将集合中的用户名发送给客户端
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                //此处消息发给客户端
                dos.writeInt(1);
                dos.writeInt(onLineUsers.size());
                for(String userName:onLineUsers){
                    dos.writeUTF(userName);
                }
                dos.flush();//刷新管道，确保数据发送出去
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
