package utils.handlers;

import messages.AuthMessage;
import messages.Commands;
import messages.FileMessage;
import tcp.TCPClient;
import utils.CommandMessage;

/**
 * The client class for recognizing command messages and control command handlers.
 */
public class ObjectHandler {
    //принимаем объект клиента
    TCPClient client;
    //объявляем объект файлового сообщения для полного файла
    FileMessage fileMessage;
    //объявляем объект файлового хендлера
    FileCommandHandler fileCommandHandler;
    //объявляем пременную для клиентской директории
    String clientDir;
    //объявляем пременную для директории в сетевом хранилище
    String storageDir;

    public ObjectHandler(TCPClient client) {
        this.client = client;
    }

    /**
     * Метот распознает тип команды и обрабатывает ее.
     * @param messageObject - объект сообщения(команды)
     */
    public void recognizeAndArrangeMessageObject(CommandMessage messageObject) {
        //выполняем операции в зависимости от типа полученного сообщения(команды)
        switch (messageObject.getCommand()) {
            //обрабатываем полученное от сервера подтверждение успешной загрузки(сохранения)
            // файла в облачное хранилище
            case Commands.SERVER_RESPONSE_FILE_UPLOAD_OK:
                //вызываем метод обработки ответа сервера
                respondOnUploadFileOK(messageObject);
                break;
            //обрабатываем полученное от сервера сообщение об ошибке загрузки(сохранения)
            // файла в облачное хранилище
            case Commands.SERVER_RESPONSE_FILE_UPLOAD_ERROR:
                //вызываем метод обработки ответа сервера
                respondOnUploadFileError(messageObject);
                break;
            //обрабатываем полученное от сервера подтверждение успешного скачивания файла из облачного хранилища
            case Commands.SERVER_RESPONSE_FILE_DOWNLOAD_OK:
                //вызываем метод обработки ответа сервера со скачанным целым файлом внутри
                respondOnDownloadFileOK(messageObject);
                break;
            //обрабатываем полученное от сервера сообщение об ошибке скачивания файла из облачного хранилища
            case Commands.SERVER_RESPONSE_FILE_DOWNLOAD_ERROR:
                //вызываем метод обработки ответа сервера
                respondOnDownloadFileError(messageObject);
                break;
            //обрабатываем полученное от сервера подтверждение успешной авторизации в облачное хранилище
            case Commands.SERVER_RESPONSE_AUTH_OK:
                AuthMessage authMessage = (AuthMessage) messageObject.getMessageObject();
                ServiceCommandHandler serviceCommandHandler = new ServiceCommandHandler(authMessage);
                serviceCommandHandler.isAuthorized();
                break;
        }
    }

    /**
     * Метод обрабатывает полученное от сервера подтверждение успешной загрузки(сохранения)
     * файла в облачное хранилище
     * @param messageObject - объект сообщения(команды)
     */
    private void respondOnUploadFileOK(CommandMessage messageObject) {
        //FIXME fill me!
        client.printMsg("Client.respondOnUploadFileOK command: " + messageObject.getCommand());
    }

    /**
     * Метод обрабатывает полученное от сервера сообщение об ошибке загрузки(сохранения)
     * файла в облачное хранилище
     * @param messageObject - объект сообщения(команды)
     */
    private void respondOnUploadFileError(CommandMessage messageObject) {
        //FIXME fill me!
        client.printMsg("Client.respondOnUploadFileError command: " + messageObject.getCommand());
    }

    /**
     * Метод обработки ответа сервера со скачанным целым файлом внутри
     * @param messageObject - объект сообщения(команды)
     */
    private void respondOnDownloadFileOK(CommandMessage messageObject) {
        //вынимаем объект файлового сообщения из объекта сообщения(команды)
        fileMessage = (FileMessage) messageObject.getMessageObject();
        //инициируем объект файлового хендлера
        fileCommandHandler = new FileCommandHandler(fileMessage);
        //вынимаем заданную директорию сетевого хранилища из объекта сообщения(команды)
        storageDir = fileMessage.getFromDir();
        //вынимаем заданную клиентскую директорию из объекта сообщения(команды)
        clientDir = fileMessage.getToDir();
        //инициируем переменную типа команды(по умолчанию - ответ об ошибке)
        int command = Commands.CLIENT_RESPONSE_FILE_DOWNLOAD_ERROR;
        //если сохранение прошло удачно
        if(fileCommandHandler.saveDownloadedFile(client, clientDir, fileMessage)){
            //проверяем сохраненный файл по контрольной сумме//FIXME
            if(true){
                //отправляем сообщение на сервер: подтверждение, что все прошло успешно
                command = Commands.CLIENT_RESPONSE_FILE_DOWNLOAD_OK;
            }
        }
        //создаем объект файлового сообщения
        fileMessage = new FileMessage(storageDir, clientDir, fileMessage.getFilename());
        //отправляем объект сообщения(команды) на сервер
        client.getConnection().sendMessageObject(new CommandMessage(command, fileMessage));
    }

    /**
     * Метод обрабатывает полученное от сервера сообщение об ошибке
     * скачивания файла из облачного хранилища
     * @param messageObject - объект сообщения(команды)
     */
    private void respondOnDownloadFileError(CommandMessage messageObject) {
        //FIXME fill me!
        client.printMsg("Client.respondOnDownloadFileError command: " + messageObject.getCommand());
    }
}