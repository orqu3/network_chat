package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private String nickname;
    private String login;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.startsWith("/reg ")) {
                                String[] token = str.split("\\s", 4);
                                boolean b = server.getAuthService()
                                        .registration(token[1], token[2], token[3]);
                                if (b) {
                                    sendMsg("/regok");
                                } else {
                                    sendMsg("/regno");
                                }
                            }

                            if (str.startsWith("/auth ")) {
                                String[] token = str.split("\\s", 3);
                                String newNick = server.getAuthService()
                                        .getNicknameByLoginAndPassword(token[1], token[2]);
                                if (newNick != null) {
                                    login = token[1];
                                    if (!server.isloginAuthenticated(login)) {
                                        nickname = newNick;
                                        out.writeUTF("/authok " + nickname);
                                        server.subscribe(this);
                                        break;
                                    } else {
                                        out.writeUTF("The account is already in use");
                                    }
                                } else {
                                    out.writeUTF("Invalid login / password");
                                }
                            }
                        }
                    }

                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.startsWith("/w")) {
                                String[] token = str.split("\\s+", 3);
                                if (token.length < 3) {
                                    continue;
                                }
                                server.privateMsg(this, token[1], token[2]);
                            }

                        } else {
                            server.broadcastMsg(this, str);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("Client disconnected!");
                    server.unsubscribe(this);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }

    public Server getServer() {
        return server;
    }

    public Socket getSocket() {
        return socket;
    }
}

