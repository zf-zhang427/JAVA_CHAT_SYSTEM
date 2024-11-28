package InterAddress.Chat_System.User;

import InterAddress.Chat_System.Server.ServerDemo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
//监听服务端发送的消息
public class ClientReaderThread extends Thread {
    private Socket socket;
    private DataInputStream dis;
    private ClientChatFrame win;

    public ClientReaderThread(Socket socket,ClientChatFrame win) {
        this.socket = socket;
        //为方便调用窗口功能，窗口对象作为参数传入
        this.win = win;
    }

    @Override
    public void run() {
        //数据发送协议
        //int 1 +String --登录信息
        //int 2 +String --群聊消息
        //注意此处所有处理消息都是服务端的
        try {
             dis = new DataInputStream(socket.getInputStream());
            while (true) {
                int type = dis.readInt(); //dis管道自动更新接收的数据
                switch (type) {
                    case 1:
                        //更新服务端的人数
                        updateClientList();
                        break;
                    case 2:
                        getMsgToWin();

                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getMsgToWin() throws Exception {
        String msg = dis.readUTF();
        win.showMsg(msg);
    }

    private void updateClientList() throws Exception {
        //1.确认多少人
        int count = dis.readInt();
        //2.接收这些名称信息
        String[] names = new String[count];
        for(int i = 0; i < count; i++){
            names[i] = dis.readUTF();
        }
        //3.打印到窗口界面
        win.showUserList(names);
    }
}

