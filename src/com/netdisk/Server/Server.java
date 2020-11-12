package com.netdisk.Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;

import com.netdisk.Dao.UserDao;
import com.netdisk.Dao.UserFileDao;
import com.netdisk.config.GlobalConfig;
import com.netdisk.model.Message;
import com.netdisk.model.User;
import com.netdisk.model.UserFile;
import com.netdisk.utils.DbUtil;

/**
 * 这是服务器类，为客户端提供连接服务，连接后提供上传和下载服务
 *
 * @author caoxi
 */
public class Server {
    private ServerSocket server;
    UserFileDao userFileDao = new UserFileDao();
    UserDao userDao = new UserDao();

    public Server() {
        try {
            server = new ServerSocket(GlobalConfig.port);
            System.out.println("服务器启动成功！");
            while (true) {
                Socket client = server.accept();
                System.out.println(client.getInetAddress() + "连接进来了");
                ClientThread c = new ClientThread(client);
                c.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 在服务器中封装一个内部类写网盘客户端连接进来的线程方法
     */

    class ClientThread extends Thread {
        private Socket client;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        Connection con;
        DbUtil db = new DbUtil();

        public ClientThread(Socket client) {
            this.client = client;
            try {
                in = new ObjectInputStream(client.getInputStream());
                out = new ObjectOutputStream(client.getOutputStream());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                Message message = (Message) in.readObject();
                System.out.println(message);

                switch (message.getType()) {
                    case UPLOAD: {
                        System.out.println("用户当前要执行上传文件操作");
                        File dir = new File(GlobalConfig.serverStoreFilePath + message.getFrom().getUserName());
                        if (!dir.exists()) {
                            dir.mkdir();
                        }

                        System.out.println(dir.toString());
                        FileOutputStream fileOut = new FileOutputStream(dir + "/" + message.getFilename());
                        int readBit = 0;
                        double fileSize = message.getFileSize();
                        //改变读取文件的速度
                        //ChangeReadBit(fileSize,readBit);
                        if (fileSize < 1024) {
                            readBit = 1024;
                        } else if (fileSize >= 1024 && fileSize < 1048576) {
                            readBit = 1024;
                        } else if (fileSize >= 1048576) {
                            readBit = 1024 * 100;
                        }
                        byte[] bs = new byte[readBit];
                        int length = -1;
                        while ((length = in.read(bs)) != -1) {
                            fileOut.write(bs, 0, length);
                            fileOut.flush();
                        }
                        fileOut.close();
                        System.out.println("用户[" + message.getFrom().getUserName() + "]的 " + message.getFilename() + " " + message.getFileSize() + " 文件上传成功！");
                        break;
                    }

                    case DOWNLOAD: {
                        System.out.println("用户当前是要执行下载文件的操作");
                        User user = message.getFrom();
                        double fileSize = 0;
                        int readBit = 0;
                        String username = message.getFrom().getUserName();//获取用户名
                        String fileName = message.getFilename();//获取用户要下载的文件名
                        for (UserFile uF : user.getUserFileSet()) {
                            if (uF.getFileName().equals(fileName)) {
                                fileSize = uF.getFileSize();
                                break;
                            }
                        }
                        File file = new File(GlobalConfig.serverStoreFilePath + username + "/" + fileName);//创建一个文件对象指向要下载的文件
                        FileInputStream fileIn = new FileInputStream(file);//准备一个文件输入流指向这个要下载的文件对象
                        //如下代码就是真正的下载业务传输数据的代码
                        //使用文件输入流从文件里读取数据，然后使用socket的输出流将文件的数据写向网络通道的客户端那一方

                        //改变读取文件的速度
                        if (fileSize < 1024) {
                            readBit = 1024;
                        } else if (fileSize >= 1024 && fileSize < 1048576) {
                            readBit = 1024;
                        } else if (fileSize >= 1048576) {
                            readBit = 1024 * 100;
                        }
                        byte[] bs = new byte[readBit];
                        int length = -1;
                        while ((length = fileIn.read(bs)) != -1) {
                            out.write(bs, 0, length);
                            out.flush();
                        }
                        fileIn.close();
                        out.close();
                        in.close();
                        System.out.println("下载文件操作完毕");
                        break;
                    }

                    case DELETE: {
                        System.out.println("用户当前是要执行删除文件到回收站的操作");
                        User user = message.getFrom();
                        String ToRecycleBinfile = message.getFilename();
                        String userNum = user.getUserNum();
                        try {
                            con = db.getcon();
                            Message successToRecycle = new Message();
                            int update = userFileDao.FileToRecycleBin(con, ToRecycleBinfile, userNum);
                            int ChangeCapacity = userDao.UpdateCapacity(user, con);
                            if (update > 0 && ChangeCapacity > 0) {
                                System.out.println("删除成功！更新成功！");
                                ///向客户端发送消息:解决当客户端的删除线程完成，接收消息
                                successToRecycle.setUpdate(update);

                            } else {
                                successToRecycle.setUpdate(update);
                                System.out.println("删除失败！");
                            }
                            out.writeObject(successToRecycle);//将消息对象发送给服务器
                            out.flush();
                            out.close();
                            in.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new Server();
    }
}
