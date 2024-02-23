package ru.geekbrains.junior.chat.server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientManager implements Runnable {

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String name;
    public static ArrayList<ClientManager> clients = new ArrayList<>();
    private boolean isPrivateMassageErr;
    private ClientManager destinationClient;
    private ClientManager srcClient;

    public ClientManager(Socket socket) {
        try {
            this.socket = socket;
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            clients.add(this);
            //TODO: ...
            name = bufferedReader.readLine();
            System.out.println(name + " подключился к чату.");
            broadcastMessage("Server: " + name + " подключился к чату.");
        }
        catch (Exception e){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    private void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        // Удаление клиента из коллекции
        removeClient();
        try {
            // Завершаем работу буфера на чтение данных
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            // Завершаем работу буфера для записи данных
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            // Закрытие соединения с клиентским сокетом
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Удаление клиента из коллекции
     */
    private void removeClient() {
        clients.remove(this);
        System.out.println(name + " покинул чат.");
        broadcastMessage("Server: " + name + " покинул чат.");
    }

    /**
     * Отправка сообщения всем слушателям
     *
     * @param message сообщение
     */
    private void broadcastMessage(String message) {
            for (ClientManager client : clients) {
                try {

                    if (!client.equals(this) && message != null) {
                        client.bufferedWriter.write(message);
                        client.bufferedWriter.newLine();
                        client.bufferedWriter.flush();

                    }

                } catch (Exception e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
    }

    /**
     * Отправка сообщения одному слушателю
     * @param message
     */
    private void unicastMessage(String message){
        try {
            if (isPrivateMassageErr){
                srcClient.bufferedWriter.write(message);
                srcClient.bufferedWriter.newLine();
                srcClient.bufferedWriter.flush();
                isPrivateMassageErr = false;
            }else {
                destinationClient.bufferedWriter.write(message);
                destinationClient.bufferedWriter.newLine();
                destinationClient.bufferedWriter.flush();
            }
        } catch (Exception e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    /**
     * Обработка приватного сообщения
     * для вызова обработки необходим патерн @name
     * @param massage сообщение
     * @return
     */
    private String privateMassage(String massage){
        String temp = massage.substring(massage.indexOf("@")+1);
        String[] destination = temp.split(" ");
        String[] src = massage.split(":");
        clients.stream()
                .filter(clientManager -> clientManager.name.equals(src[0]))
                .forEach(clientManager -> srcClient = clientManager);
        if (clients.stream().anyMatch(clientManager -> clientManager.name.equals(destination[0]))){
            clients.stream()
                    .filter(clientManager -> clientManager.name.equals(destination[0]))
                    .forEach(clientManager -> destinationClient = clientManager);

            temp = massage.replace(": @", " [private]:");
            return temp.replace(destination[0], "");
        }else {
            isPrivateMassageErr = true;
            return "Системное сообщение: извините мы не нашли адресата и не смогли доставить ваше сообщения";
        }


    }

    @Override
    public void run() {
        String massageFromClient;
        while (!socket.isClosed()) {
            try {
                // Чтение данных
                massageFromClient = bufferedReader.readLine();
                if (massageFromClient.contains(": @")){
                    massageFromClient = privateMassage(massageFromClient);
                    unicastMessage(massageFromClient);
                }else {
                    // Отправка данных всем слушателям
                    broadcastMessage(massageFromClient);
                }
            }
            catch (Exception e){
                closeEverything(socket, bufferedReader, bufferedWriter);
                //break;
            }
        }
    }
}
