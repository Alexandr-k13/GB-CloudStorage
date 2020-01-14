package netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import messages.DirectoryMessage;
import messages.FileFragmentMessage;
import messages.FileMessage;
import utils.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The server's class for recognizing command messages and control command handlers.
 */
public class CommandMessageManager extends ChannelInboundHandlerAdapter {
    //принимаем объект соединения
    private ChannelHandlerContext ctx;
    //принимаем объект контроллера сетевого хранилища
    private final CloudStorageServer storageServer;

    //принимаем объект реального пути к корневой директории пользователя в сетевом хранилище
    private Path userStorageRoot;
    //объявляем объект файлового обработчика
    private FileUtils fileUtils;
    //объявляем переменную типа команды
    private int command;//TODO сделать локальной?

    public CommandMessageManager(CloudStorageServer storageServer) {
        this.storageServer = storageServer;
        //принимаем объект файлового обработчика
        fileUtils = storageServer.getFileUtils();
    }

    /**
     * Метод в полученном объекте сообщения распознает тип команды и обрабатывает ее.
     * @param ctx - объект соединения netty, установленного с клиентом
     * @param msg - входящий объект сообщения
     * @throws Exception - исключение
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //принимаем объект соединения
        this.ctx = ctx;
        //инициируем из объекта сообщения объект команды
        CommandMessage commandMessage = (CommandMessage) msg;
        //если сюда прошли, значит клиент авторизован
        //***блок обработки объектов сообщений(команд), полученных от клиента***
        //выполняем операции в зависимости от типа полученного не сервисного сообщения(команды)
        switch (commandMessage.getCommand()) {
            //обрабатываем полученный от клиента запрос на список объектов файлов и папок
            // в заданной директории в облачном хранилище
            case Commands.REQUEST_SERVER_ITEMS_LIST:
                //вызываем метод обработки запроса от клиента
                onDirectoryItemsListClientRequest(commandMessage);
                break;
            //обрабатываем полученный от клиента запрос на загрузку(сохранение) файла в облачное хранилище
            case Commands.REQUEST_SERVER_UPLOAD_ITEM:
                //вызываем метод обработки запроса от клиента на загрузку целого файла клиента
                // в директорию в сетевом хранилище.
                onUploadItemClientRequest(commandMessage);
                break;
            //обрабатываем полученный от клиента запрос на скачивание целого файла из облачного хранилища
            case Commands.REQUEST_SERVER_DOWNLOAD_ITEM:
                //вызываем метод обработки запроса от клиента на скачивание целого файла клиента
                // из директории в сетевом хранилище
                onDownloadItemClientRequest(commandMessage);
                break;
            //обрабатываем полученный от клиента запрос на загрузку(сохранение) фрагмента файла в облачное хранилище
            case Commands.REQUEST_SERVER_FILE_FRAG_UPLOAD:
                //вызываем метод обработки запроса от клиента на загрузку файла-фрагмента
                //в директорию в сетевом хранилище.
                onUploadFileFragClientRequest(commandMessage);
                break;
            //обрабатываем полученный от клиента запрос на переименование файла или папки
            // в заданной директории в сетевом хранилище
            case Commands.REQUEST_SERVER_RENAME_ITEM:
                //вызываем метод обработки запроса от клиента
                onRenameItemClientRequest(commandMessage);
                break;
            //обрабатываем полученный от клиента запрос на удаление файла или папки
            // в заданной директории в сетевом хранилище
            case Commands.REQUEST_SERVER_DELETE_ITEM:
                //вызываем метод обработки запроса от клиента
                onDeleteItemClientRequest(commandMessage);
                break;
            //обрабатываем полученный от AuthGateway проброшенный запрос на авторизацию клиента в облачное хранилище
            //возвращаем список объектов в корневой директорию пользователя в сетевом хранилище.
            case Commands.SERVER_RESPONSE_AUTH_OK:
                //вызываем метод обработки запроса от AuthGateway
                onAuthClientRequest(commandMessage);
                break;
        }
    }

    /**
     * Метод обрабатывает полученный от клиента запрос на список объектов(файлов и папок)
     * в заданной директории в облачном хранилище
     * @param commandMessage - объект сообщения(команды)
     */
    private void onDirectoryItemsListClientRequest(CommandMessage commandMessage) {
        //вынимаем объект сообщения о директории из объекта сообщения(команды)
        DirectoryMessage directoryMessage = (DirectoryMessage) commandMessage.getMessageObject();
        //инициируем объект для принятой директории сетевого хранилища
        Item storageDirItem = storageServer.createStorageDirectoryItem(
                directoryMessage.getDirectoryPathname(), userStorageRoot);
        //отправляем объект сообщения(команды) клиенту со списком объектов(файлов и папок) в
        // заданной директории клиента в сетевом хранилище
        sendItemsList(storageDirItem, Commands.SERVER_RESPONSE_ITEMS_LIST_OK);
    }

    /**
     * Метод обработки запроса от клиента на загрузку целого объекта(файла) клиента
     * в заданную директорию в сетевом хранилище.
     * @param commandMessage - объект сообщения(команды)
     */
    private void onUploadItemClientRequest(CommandMessage commandMessage) {
        //вынимаем объект файлового сообщения из объекта сообщения(команды)
        FileMessage fileMessage = (FileMessage) commandMessage.getMessageObject();
        //если сохранение прошло удачно
        if(storageServer.uploadItem(fileMessage.getStorageDirectoryItem(), fileMessage.getItem(),
                fileMessage.getData(), fileMessage.getFileSize(), userStorageRoot)){
            //отправляем сообщение на сервер: подтверждение, что все прошло успешно
            command = Commands.SERVER_RESPONSE_UPLOAD_ITEM_OK;
            //если что-то пошло не так
        } else {
            //выводим сообщение
            printMsg("[server]" + fileUtils.getMsg());
            //инициируем переменную типа команды(по умолчанию - ответ об ошибке)
            command = Commands.SERVER_RESPONSE_UPLOAD_ITEM_ERROR;
        }
        //отправляем объект сообщения(команды) клиенту со списком объектов(файлов и папок) в
        // заданной директории клиента в сетевом хранилище
        sendItemsList(fileMessage.getStorageDirectoryItem(), command);
    }

    /**
     * Метод обрабатывает полученное от клиента подтверждение успешного получения
     * обновленного списка файлов клиента в облачном хранилище
     * @param commandMessage - объект сообщения(команды)
     */
    private void onUploadFileOkClientResponse(CommandMessage commandMessage) {
        //FIXME fill me!
        printMsg("[server]CommandMessageManager.onUploadFileOkClientResponse() command: " + commandMessage.getCommand());
    }

    /**
     * Метод обрабатывает полученное от клиента сообщение об ошибке получения обновленного
     * списка файлов клиента в облачном хранилище
     * @param commandMessage - объект сообщения(команды)
     */
    private void onUploadFileErrorClientResponse(CommandMessage commandMessage) {
        //FIXME fill me!
        printMsg("[server]CommandMessageManager.onUploadFileErrorClientResponse() command: " + commandMessage.getCommand());
    }

    /**
     * Метод обработки запроса от клиента на скачивание целого объекта элемента(файла)
     * из директории в сетевом хранилище в клиента.
     * @param commandMessage - объект сообщения(команды)
     */
    private void onDownloadItemClientRequest(CommandMessage commandMessage) throws IOException {
        //вынимаем объект файлового сообщения из объекта сообщения(команды)
        FileMessage fileMessage = (FileMessage) commandMessage.getMessageObject();
        //запускаем процесс скачивания и отправки объекта элемента
        storageServer.downloadItem(fileMessage, userStorageRoot, ctx);
    }

    /**
     * Метод обрабатывает полученное от клиента подтверждение успешного сохранения целого файла,
     * скачанного из облачного хранилища
     * @param commandMessage - объект сообщения(команды)
     */
    private void onDownloadFileOkClientResponse(CommandMessage commandMessage) {
        //FIXME fill me!
        printMsg("[server]CommandMessageManager.onDownloadFileOkClientResponse() command: " + commandMessage.getCommand());
    }

    /**
     * Метод обрабатывает полученное от клиента сообщение об ошибке сохранения целого файла,
     * скачанного из облачного хранилища
     * @param commandMessage - объект сообщения(команды)
     */
    private void onDownloadFileErrorClientResponse(CommandMessage commandMessage) {
        //FIXME fill me!
        printMsg("[server]CommandMessageManager.onDownloadFileErrorClientResponse() command: " + commandMessage.getCommand());
    }

    /**
     * Метод обработки запроса от клиента на загрузку файла-фрагмента
     * в директорию в сетевом хранилище.
     * @param commandMessage - объект сообщения(команды)
     */
    private void onUploadFileFragClientRequest(CommandMessage commandMessage) {
        //вынимаем объект файлового сообщения из объекта сообщения(команды)
        FileFragmentMessage fileFragmentMessage = (FileFragmentMessage) commandMessage.getMessageObject();
        //собираем временную директорию в целевой директории пользователя в сетевом хранилище
        //для сохранения фрагментов
        String realToDirTemp = realStorageDirectory(fileFragmentMessage.getToTempDir());
        //создаем объект пути к папке с загруженным файлом
        String realToDir = Paths.get(realToDirTemp).getParent().toString();
        //если сохранение полученного фрагмента файла во временную папку сетевого хранилища прошло удачно
        if(fileUtils.saveFileFragment(realToDirTemp, fileFragmentMessage)){
            //отправляем сообщение на сервер: подтверждение, что все прошло успешно
            command = Commands.SERVER_RESPONSE_FILE_FRAG_UPLOAD_OK;
        //если что-то пошло не так
        } else {
            //выводим сообщение
            printMsg("[server]" + fileUtils.getMsg());
            //инициируем переменную типа команды - ответ об ошибке
            command = Commands.SERVER_RESPONSE_FILE_FRAG_UPLOAD_ERROR;
        }
        //если это последний фрагмент
        if(fileFragmentMessage.isFinalFileFragment()){
            //если корректно собран файл из фрагментов сохраненных во временную папку
            if(fileUtils.compileFileFragments(realToDirTemp, realToDir, fileFragmentMessage)){
                //ответ сервера, что сборка файла из загруженных фрагментов прошла успешно
                command = Commands.SERVER_RESPONSE_FILE_FRAGS_UPLOAD_OK;
            //если что-то пошло не так
            } else {
                //выводим сообщение
                printMsg("[server]" + fileUtils.getMsg());
                //инициируем переменную типа команды - ответ об ошибке
                command = Commands.SERVER_RESPONSE_FILE_FRAGS_UPLOAD_ERROR;
            }
            //отправляем объект сообщения(команды) клиенту со списком файлов и папок в
            // заданной директории клиента в сетевом хранилище
//            sendFileObjectsList(fileFragmentMessage.getToDir(), realToDir, command);//FIXME
        }
    }

    /**
     * Метод скачивания и отправки по частям большого файла размером более
     * константы максмального размера фрагмента файла
     * @param fromDir - директория(относительно корня) клиента где хранится файл источник
     * @param toDir - директория(относительно корня) в сетевом хранилище
     * @param filename - строковое имя файла
     * @param fullFileSize - размер целого файла в байтах
     * @throws IOException - исключение
     */
    private void downloadFileByFrags(String fromDir, String toDir,
                                     String filename, long fullFileSize) throws IOException {
        //TODO temporarily
        long start = System.currentTimeMillis();

        //***разбиваем файл на фрагменты***
        //рассчитываем количество полных фрагментов файла
        int totalEntireFragsNumber = (int) fullFileSize / FileFragmentMessage.CONST_FRAG_SIZE;
        //рассчитываем размер последнего фрагмента файла
        int finalFileFragmentSize = (int) fullFileSize - FileFragmentMessage.CONST_FRAG_SIZE * totalEntireFragsNumber;
        //рассчитываем общее количество фрагментов файла
        //если есть последний фрагмент, добавляем 1 к количеству полных фрагментов файла
        int totalFragsNumber = (finalFileFragmentSize == 0) ?
                totalEntireFragsNumber : totalEntireFragsNumber + 1;

        //TODO temporarily
        printMsg("[server]CommandMessageManager.downloadFileByFrags() - fullFileSize: " + fullFileSize);
        printMsg("[server]CommandMessageManager.downloadFileByFrags() - totalFragsNumber: " + totalFragsNumber);
        printMsg("[server]CommandMessageManager.downloadFileByFrags() - totalEntireFragsNumber: " + totalEntireFragsNumber);

        //устанавливаем начальные значения номера текущего фрагмента и стартового байта
        long startByte = 0;
        //инициируем байтовый массив для чтения данных для полных фрагментов
        byte[] data = new byte[FileFragmentMessage.CONST_FRAG_SIZE];
        //инициируем массив имен фрагментов файла
        String[] fragsNames = new String[totalFragsNumber];
        //***в цикле создаем целые фрагменты, читаем в них данные и отправляем***
        for (int i = 1; i <= totalEntireFragsNumber; i++) {
            //инициируем объект фрагмента файлового сообщения
            FileFragmentMessage fileFragmentMessage =
                    new FileFragmentMessage(fromDir, toDir, filename, fullFileSize,
                            i, totalFragsNumber, FileFragmentMessage.CONST_FRAG_SIZE, fragsNames, data);
            //читаем данные во фрагмент с определенного места файла
            fileFragmentMessage.readFileDataToFragment(fromDir, filename, startByte);
            //увеличиваем указатель стартового байта на размер фрагмента
            startByte += FileFragmentMessage.CONST_FRAG_SIZE;

            //отправляем объект сообщения(команды) клиенту
            ctx.writeAndFlush(new CommandMessage(Commands.SERVER_RESPONSE_FILE_FRAGS_DOWNLOAD_OK,
                    fileFragmentMessage));
        }

        //TODO temporarily
        printMsg("[server]CommandMessageManager.downloadFileByFrags() - currentFragNumber: " + totalFragsNumber);
        printMsg("[server]CommandMessageManager.downloadFileByFrags() - finalFileFragmentSize: " + finalFileFragmentSize);

        //***отправляем последний фрагмент, если он есть***
        if(totalFragsNumber > totalEntireFragsNumber){
            //инициируем байтовый массив для чтения данных для последнего фрагмента
            byte[] dataFinal = new byte[finalFileFragmentSize];
            //инициируем объект фрагмента файлового сообщения
            FileFragmentMessage fileFragmentMessage =
                    new FileFragmentMessage(fromDir, toDir, filename, fullFileSize,
                            totalFragsNumber, totalFragsNumber, finalFileFragmentSize, fragsNames, dataFinal);
            //читаем данные во фрагмент с определенного места файла
            fileFragmentMessage.readFileDataToFragment(fromDir, filename, startByte);

            //отправляем объект сообщения(команды) клиенту
            ctx.writeAndFlush(new CommandMessage(Commands.SERVER_RESPONSE_FILE_FRAGS_DOWNLOAD_OK,
                    fileFragmentMessage));
        }

        //TODO temporarily
        long finish = System.currentTimeMillis() - start;
        printMsg("[server]CommandMessageManager.downloadFileByFrags() - duration(mc): " + finish);
    }

//    /**
//     * Метод скачивания и отправки целого небольшого файла размером менее
//     * константы максмального размера фрагмента файла
//     * @param fromDir - директория(относительно корня) клиента где хранится файл источник
//     * @param clientDir - директория(относительно корня) в сетевом хранилище
//     * @param filename - строковое имя файла
//     */
//    private void downloadEntireFile(String fromDir, String clientDir, String filename){
//        //создаем объект файлового сообщения
//        FileMessage fileMessage = new FileMessage(fromDir, clientDir, filename);
//        //если скачивание прошло удачно
//        if(fileUtils.readFile(fromDir, fileMessage)){
//            //инициируем переменную типа команды - ответ cо скачанным файлом
//            command = Commands.SERVER_RESPONSE_FILE_DOWNLOAD_OK;
//        //если что-то пошло не так
//        } else {
//            //выводим сообщение
//            printMsg("[server]" + fileUtils.getMsg());
//            //инициируем переменную типа команды - ответ об ошибке скачивания
//            command = Commands.SERVER_RESPONSE_FILE_DOWNLOAD_ERROR;
//        }
//        //отправляем объект сообщения(команды) клиенту
//        ctx.writeAndFlush(new CommandMessage(command, fileMessage));
//    }
//    private void downloadEntireFile(String fromDir, String clientDir, String filename){
//        //создаем объект файлового сообщения
//        FileMessage fileMessage = new FileMessage(fromDir, clientDir, filename);
//        //если скачивание прошло удачно
//        if(fileUtils.readFile(fromDir, fileMessage)){//TODO заменить fromDir на itemPathname
//            //инициируем переменную типа команды - ответ cо скачанным файлом
//            command = Commands.SERVER_RESPONSE_FILE_DOWNLOAD_OK;
//            //если что-то пошло не так
//        } else {
//            //выводим сообщение
//            printMsg("[server]" + fileUtils.getMsg());
//            //инициируем переменную типа команды - ответ об ошибке скачивания
//            command = Commands.SERVER_RESPONSE_FILE_DOWNLOAD_ERROR;
//        }
//        //отправляем объект сообщения(команды) клиенту
//        ctx.writeAndFlush(new CommandMessage(command, fileMessage));
//    }

    /**
     * Метод обрабатываем полученный от клиента запрос на переименование объекта элемента
     * списка(файла или папки) в заданной директории в сетевом хранилище
     * @param commandMessage - объект сообщения(команды)
     */
    private void onRenameItemClientRequest(CommandMessage commandMessage) {
        //вынимаем объект файлового сообщения из объекта сообщения(команды)
        FileMessage fileMessage = (FileMessage) commandMessage.getMessageObject();
        //если сохранение прошло удачно
        if(storageServer.renameStorageItem(fileMessage.getItem(), fileMessage.getNewName(), userStorageRoot)){
            //отправляем сообщение на сервер: подтверждение, что все прошло успешно
            command = Commands.SERVER_RESPONSE_RENAME_ITEM_OK;
            //если что-то пошло не так
        } else {
            //выводим сообщение
            printMsg("[server]" + fileUtils.getMsg());
            //инициируем переменную типа команды(по умолчанию - ответ об ошибке)
            command = Commands.SERVER_RESPONSE_RENAME_ITEM_ERROR;
        }
        //отправляем объект сообщения(команды) клиенту со списком объектов(файлов и папок) в
        // заданной директории клиента в сетевом хранилище
        sendItemsList(fileMessage.getStorageDirectoryItem(), command);
    }

    /**
     * Метод обрабатываем полученный от клиента запрос на удаление объекта элемента
     * списка(файла или папки) в заданной директории в сетевом хранилище
     * @param commandMessage - объект сообщения(команды)
     */
    private void onDeleteItemClientRequest(CommandMessage commandMessage) {
        //вынимаем объект файлового сообщения из объекта сообщения(команды)
        FileMessage fileMessage = (FileMessage) commandMessage.getMessageObject();
        //если удаление прошло удачно
        if(storageServer.deleteClientItem(fileMessage.getItem(), userStorageRoot)){
            //отправляем сообщение на сервер: подтверждение, что все прошло успешно
            command = Commands.SERVER_RESPONSE_DELETE_ITEM_OK;
            //если что-то пошло не так
        } else {
            //выводим сообщение
            printMsg("[server]" + fileUtils.getMsg());
            //инициируем переменную типа команды(по умолчанию - ответ об ошибке)
            command = Commands.SERVER_RESPONSE_DELETE_ITEM_ERROR;
        }
        //отправляем объект сообщения(команды) клиенту со списком объектов(файлов и папок) в
        // заданной директории клиента в сетевом хранилище
        sendItemsList(fileMessage.getStorageDirectoryItem(), command);
    }

    /**
     * Метод обрабатывает полученный от AuthGateway проброшенный запрос на авторизацию клиента в облачное хранилище
     * Возвращает список объектов в корневой директорию пользователя в сетевом хранилище.
     * @param commandMessage - объект сообщения(команды)
     */
    private void onAuthClientRequest(CommandMessage commandMessage) {
        //вынимаем объект реального пути к его корневой директории клиента в сетевом хранилище
        userStorageRoot = Paths.get(commandMessage.getDirectory());
        //инициируем объект для принятой директории сетевого хранилища
        Item storageDirItem = new Item(storageServer.getSTORAGE_DEFAULT_DIR());
        //отправляем объект сообщения(команды) клиенту со списком файлов и папок в
        // заданной директории клиента в сетевом хранилище
        sendItemsList(storageDirItem, commandMessage.getCommand());
        //удаляем входящий хэндлер AuthGateway, т.к. после авторизации он больше не нужен
        printMsg("[server]CommandMessageManager.onAuthClientRequest() - " +
                "removed pipeline: " + ctx.channel().pipeline().remove(AuthGateway.class));
    }

    /**
     * Метод формирует и отправляет клиенту сообщение(команду) с массивом объектов
     * в заданной директории пользователя в сетевом хранилище.
     * @param storageDirItem - объект заданной директории пользователя в сетевом хранилище
     * @param command - комманда об успешном или не успешном формировании списка
     */
    private void sendItemsList(Item storageDirItem, int command) {
        //инициируем объект сообщения о директории с массивом объектов
        // в заданной директории клиента в сетевом хранилище
        DirectoryMessage directoryMessage = new DirectoryMessage(storageDirItem,
                storageServer.storageItemsList(storageDirItem, userStorageRoot));
        //отправляем объект сообщения(команды) клиенту
        ctx.writeAndFlush(new CommandMessage(command, directoryMessage));
    }

    private String realStorageDirectory(String storageDir) {
        return Paths.get(userStorageRoot.toString(), storageDir).toString();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    public void printMsg(String msg){
        storageServer.printMsg(msg);
    }
}