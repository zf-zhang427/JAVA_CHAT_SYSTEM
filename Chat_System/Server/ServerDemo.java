package InterAddress.Chat_System.Server;

import InterAddress.Chat_System.Constant;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ServerDemo {
    //定义一个map集合存储用户名（值）称和Socket（键）
    public static final Map<Socket, String> onLineSockets = new HashMap<>();
    public static void main(String[] args) {
        System.out.println("==== 服务端启动 ====");
        try {
            //1.注册端口
            ServerSocket serverSocket = new ServerSocket(Constant.PORT);
            //2.循环接收客户端连接请求
            while(true){
                System.out.println("等待客户端连接...");
                Socket socket = serverSocket.accept();
                System.out.println("客户端"+socket.getInetAddress().getHostAddress()+" "+socket.getPort()+"上线了");
                //接受到客户端请求，创建线程并启动
                new ServerReaderThread(socket).start();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
