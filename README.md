# JAVA_CHAT_SYSTEM
# 登录模块设计



## 模块划分

1. #### **服务端**

   - `ServerDemo`：启动服务器并监听客户端连接。
   - `ServerReaderThread`：处理每个客户端的消息读取和响应。

2. #### **客户端**

   - `ChatEntryFrame`：登录窗口，允许用户输入昵称并连接到服务器。

   - `ClientChatFrame`：聊天窗口，显示聊天消息、发送消息、查看在线用户列表与更新UI。

   - `ClientReaderThread`：处理从服务器接收到的消息并反馈给客户端模块。

     

## **实现思路与逻辑**

### 1.服务端实现

1. #### **启动服务器**

   - `ServerDemo`类启动服务器，绑定到指定端口（常量`Constant.PORT`定义）。

   - 进入无限循环，等待客户端连接请求。

   - 定义静态变量`onLineSockets`用于存储在线用户的Socket连接信息。

     ```java
     public static final Map<Socket, String> onLineSockets = new HashMap<>();
     ```

   - 当有新的客户端连接时，创建一个新的线程`ServerReaderThread`来处理该客户端的消息。

     ```java
     ServerSocket serverSocket = new ServerSocket(Constant.PORT);
     
     while(true){
         System.out.println("等待客户端连接...");
         Socket socket = serverSocket.accept();
         System.out.println("客户端"+socket.getInetAddress().getHostAddress()+" "+socket.getPort()+"上线了");
         //接受到客户端请求，创建线程并启动
         new ServerReaderThread(socket).start();
     }
     ```

   

2. #### **处理客户端消息**

   - `ServerReaderThread`处理客户端的消息，每个客户端连接由一个独立的线程处理。

   - ```java
     public ServerReaderThread(Socket socket){this.socket = socket;}
     ```

   - 使用`DataInputStream`读取消息类型和具体内容。

   - 根据消息类型执行不同的操作：

     - 类型为1（登录信息）：
       - 将用户信息保存到`onLineSockets`中，并调用`updateClientList`通知所有客户端。
     - 类型为2（群聊消息）：
       - 转发消息给所有在线客户端。

   - 异常处理：当客户端断开连接时，移除该客户端的Socket并广播最新的在线用户列表。

     ```java
     try {
         DataInputStream dis = new DataInputStream(socket.getInputStream());
         while (true) {
             int type = dis.readInt(); //dis管道自动更新接收的数据
             switch (type){
                 case 1://接收登录信息，更新客户端内的用户列表    
                     String userName = dis.readUTF();
                     ServerDemo.onLineSockets.put(socket,userName);
                     updateClientList();//更新用户列表
                     break;
                 case 2: //客户端发送数据，转发至所有客户端
                     String msg = dis.readUTF();
                     sendMsgToAll(msg);
                     break;
             }
         }
     } catch (Exception e) {
         System.out.println("客户端下线了："+socket.getInetAddress().getHostAddress()+" "+socket.getPort());
         ServerDemo.onLineSockets.remove(socket);//将下线的用户移除
         updateClientList();//更新所有用户列表
     }
     ```

   

3. #### **具体方法**

   - ```
     sendMsgToAll
     ```

     - 拼装完整的群聊消息，包括用户名、时间戳和消息内容。

       ```java
       LocalDateTime now = LocalDateTime.now();//时间
       DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss EEE a");//创建格式
       String nowTime = dtf.format(now);
       
       String allMsg = sb.append(name).append(" ").append(nowTime)
               .append("\r\n").append(msg).toString();
       ```

     - 遍历所有在线客户端，将消息发送给每个客户端。

       ```java
       for(Socket socket:ServerDemo.onLineSockets.keySet()){
           try {
               DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
               dos.writeInt(2);
               dos.writeUTF(allMsg);
               dos.flush();//刷新管道，确保数据发送出去}
       ```

   

   - ```
     updateClientList
     ```

     - 获取所有在线用户的用户名。

       ```java
       Collection<String> onLineUsers = ServerDemo.onLineSockets.values();
       ```

     - 遍历所有在线客户端，将最新的用户列表发送给每个客户端。

       ```java
       for(Socket socket:ServerDemo.onLineSockets.keySet()){
           try {
               //将集合中的用户名发送给客户端
               DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
               //此处消息发给客户端
               dos.writeInt(1);
               dos.writeInt(onLineUsers.size());
               for(String userName:onLineUsers){
                   dos.writeUTF(userName);}
       ```



### 2.客户端实现

1. #### **登录窗口**

   - `ChatEntryFrame`类用于显示登录窗口。

   - 包含昵称输入框、登录按钮和取消按钮。

   - 登录按钮点击事件：

     - 检查昵称是否为空。
     - 如果不为空：
       - 调用`login`方法将登录信息发送给服务器。
       - 启动`ClientChatFrame`聊天窗口并将昵称和套接字传递给它。
       - 关闭当前登录窗口。
     - 如果昵称为空，弹出提示框要求用户输入昵称。

   - 取消按钮点击事件：退出程序。

     ```java
     enterButton.addActionListener(e -> {
         String nickname = nicknameField.getText();
         if (!nickname.isEmpty()) {
             try {
                 login(nickname);
                 new ClientChatFrame(nickname, socket);//启动聊天室(一个代码只会创建一个客户端，所以把名称与客户端直接传给聊天框)
             } catch (Exception ex) {
                 ex.printStackTrace();
             }
             dispose(); // 关闭窗口
         } else {
             JOptionPane.showMessageDialog(this, "请输入昵称!");
         }});
     ```

   

2. #### **聊天窗口**

   - `ClientChatFrame`类用于显示聊天窗口。

   - 包含消息内容区域、消息发送区域、在线用户列表和发送按钮。

   - 发送按钮点击事件：

     - 获取输入框内容。

     - 清空输入框。

     - 调用`sendMsg`方法发送消息。

       ```java
       sendBn.addActionListener(e -> {
           //获取输入框内容
           String msg = smsSend.getText();
           //清空输入框
           smsSend.setText("");
           //发送消息
           sendMsg(msg);
       });
       ```

   

   3. #### **具体方法**

   - ```
     login
     ```

     - 创建Socket对象连接到服务器。

     - 发送登录类型标识（整数1）和昵称字符串。

     - 刷新输出流以确保数据发送出去。

       ```java
       private void login(String nickname) throws Exception {
           //将登录信息发送给服务端
           //创建socket管道与服务端连接
           socket = new Socket(Constant.IP, Constant.PORT);
           DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
           dos.writeInt(1);
           dos.writeUTF(nickname);
           dos.flush();}
       ```

   

   - ```
     sendMsg
     ```

     - 通过套接字向服务器发送消息类型标识（整数2）和消息内容。

     - 刷新输出流以确保数据发送出去。

       ```java
       try {
           //定义输出流
           DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
           //把消息发给服务器
           dos.writeInt(2);
           dos.writeUTF(msg);
           dos.flush();
       } catch (IOException e) {
           e.printStackTrace();}
       ```

   

   - ```
     showUserList
     ```

     - 更新在线用户列表`onLineUsers`的内容。

       ```java
       public void showUserList(String[] names) {
           //接受到服务器返回的所有在线用户，更新用户列表
           onLineUsers.setListData(names);}
       ```

   

   - ```
     showMsg
     ```

     - 将接收到的消息追加到消息内容框`smsContent`中。

       ```java
       public void showMsg(String msg) {
           smsContent.append(msg+"\n");}
       ```

   

   4. #### **处理服务器消息**

   - `ClientReaderThread`类处理从服务器接收到的消息。

   - 使用`DataInputStream`读取消息类型和具体内容。

   - 根据消息类型执行不同的操作：

     - 类型为1（登录信息）：
       - 调用`updateClientList`方法更新在线用户列表。
     - 类型为2（群聊消息）：
       - 调用`getMsgToWin`方法将消息显示在聊天窗口中。

   - 异常处理：打印堆栈跟踪信息。

     ```java
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
                     ...
     ```

     ```java
     private void getMsgToWin() throws Exception {
         String msg = dis.readUTF();
         win.showMsg(msg);}
     ```

     ```java
     private void updateClientList() throws Exception {
         //1.确认多少人
         int count = dis.readInt();
         //2.接收这些名称信息
         String[] names = new String[count];
         for(int i = 0; i < count; i++){
             names[i] = dis.readUTF();
         }
         //3.打印到窗口界面
         win.showUserList(names);}
     ```

     

   5. #### **难点**

   - 因为`ClientReaderThread`类对象中需要调用`ClientChatFrame`中的方法，所以需要通过`this`将`ClientChatFrame`对象打包交给前者引用。

     ```java
     public ClientReaderThread(Socket socket,ClientChatFrame win) {
         this.socket = socket;
         //为方便调用窗口功能，窗口对象作为参数传入
         this.win = win;}
     ```



## 模块交互

1. **服务端与客户端的通信**
   - 客户端通过`Socket`连接到服务器。
   - 客户端发送登录信息和群聊消息到服务器。
   - 服务器接收并处理这些消息，然后广播给所有在线客户端。
2. **多线程处理**
   - 服务端使用`ServerReaderThread`为每个客户端创建一个独立的线程来处理消息。
   - 客户端使用`ClientReaderThread`处理从服务器接收到的消息。
3. **UI更新**
   - 客户端的`ClientChatFrame`类包含`showUserList`和`showMsg`方法，用于更新在线用户列表和显示聊天消息。
   - `ClientReaderThread`通过回调这些方法来更新UI。
