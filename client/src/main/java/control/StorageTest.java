package control;

import messages.AuthMessage;
import messages.FileFragmentMessage;
import messages.FileMessage;
import tcp.TCPClient;
import tcp.TCPConnection;
import utils.CommandMessage;
import utils.Commands;
import utils.FileUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

/**
 * The class is responded for operation with storage by communication with command handlers.
 */
public class StorageTest {

    public StorageTest() {
        //инициируем объект сетевого соединения с сервером
        TCPClient tcpClient = new TCPClient(this);
        //принимаем объект соединения
        connection = tcpClient.getConnection();
        //запускаем тестирование
        startTest();
    }

    //TODO temporarily
    //объявляем объект защелки
    private CountDownLatch countDownLatch;

    //инициируем переменную для директории, заданной относительно userStorageRoot в сетевом хранилище
    private String storageDir = "";
    //инициируем строку названия директории облачного хранилища(сервера) для хранения файлов клиента
    private final String clientDefaultRoot = "storage/client_storage";
    //инициируем переменную для директории, заданной относительно clientRoot
    private String clientDir = "";
    //объявляем переменную для текущей директории клиента
    private String currentClientDir;
    //инициируем переменную для печати сообщений в консоль
    private final PrintStream log = System.out;
    //объявляем переменную сетевого соединения
    private TCPConnection connection;

    //объявляем объект файлового обработчика
    private FileUtils fileUtils;

    //FIXME удалить, когда будет реализован интерфейс
    public void startTest() {
        //инициируем объект файлового обработчика
        fileUtils = new FileUtils();

        //инициируем переменную для текущей директории клиента
        currentClientDir = clientDefaultRoot;
        //инициируем объект защелки на один сброс
        countDownLatch = new CountDownLatch(1);
        //отправляем на сервер запрос на авторизацию в облачное хранилище
        requestAuthorization("login1", "pass1");

        try {
            //ждем сброса защелки
            countDownLatch.await();
            //добавляем к корневой директории пользователя в сетевом хранилище
            // имя подпапки назначения
            storageDir = storageDir.concat("folderToUploadFile");
            //инициируем переменную для текущей директории клиента
            currentClientDir = clientDefaultRoot;
            //отправляем на сервер запрос на загрузку маленького файла в облачное хранилище
            uploadFile(currentClientDir, storageDir, "toUpload.txt");//TODO for test
            //отправляем на сервер запрос на загрузку большого файла в облачное хранилище
//            uploadFile(currentClientDir, storageDir, "toUploadBIG.mp4");//TODO for test
//            uploadFile(currentClientDir, storageDir, "toUploadMedium.png");//TODO for test

            //инициируем объект защелки на один сброс
            countDownLatch = new CountDownLatch(1);//TODO
            //ждем сброса защелки
            countDownLatch.await();
            //восстанавливаем начальное значение директории в сетевом хранилище//TODO temporarily
            storageDir = "";
            //добавляем к корневой директории клиента имя подпапки назначения на клиенте
            clientDir = clientDir.concat("folderToDownloadFile");
            //отправляем на сервер запрос на скачивание файла из облачного хранилища
            downloadFile(storageDir, clientDir, "toDownload.png");
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    //отправляем на сервер запрос на авторизацию в облачное хранилище
    private void requestAuthorization(String login, String password) {
        //TODO temporarily
        printMsg("***StorageTest.requestAuthorization() - has started***");

        //отправляем на сервер объект сообщения(команды)
        connection.sendMessageObject(new CommandMessage(Commands.REQUEST_SERVER_AUTH,
                new AuthMessage(login, password)));

        //TODO temporarily
        printMsg("***StorageTest.requestAuthorization() - has finished***");
    }

    //отправляем на сервер запрос на загрузку файла в облачное хранилище
    //FIXME перенести в контроллер интерфейса
    public void uploadFile(String fromDir, String toDir, String filename) throws IOException {
        //TODO temporarily
        printMsg("***StorageTest.uploadFile() - has started***");

        //вычисляем размер файла
        long fileSize = Files.size(Paths.get(fromDir, filename));
        //если размер файла больше константы размера фрагмента
        if(fileSize > FileFragmentMessage.CONST_FRAG_SIZE){
            //запускаем метод отправки файла по частям
            uploadFileByFrags(fromDir, toDir, filename, fileSize);
        //если файл меньше
        } else {
            //запускаем метод отправки целого файла
            uploadEntireFile(fromDir, toDir, filename, fileSize);
        }

        //TODO temporarily
        printMsg("***StorageTest.uploadFile() - has finished***");
    }

    /**
     * Метод отправки по частям большого файла размером более константы максмального размера фрагмента файла
     * @param fromDir - директория(относительно корня) клиента где хранится файл источник
     * @param toDir - директория(относительно корня) в сетевом хранилище
     * @param filename - строковое имя файла
     * @param fullFileSize - размер целого файла в байтах
     * @throws IOException - исключение
     */
    private void uploadFileByFrags(String fromDir, String toDir, String filename, long fullFileSize) throws IOException {
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
        System.out.println("StorageTest.uploadFileByFrags() - fullFileSize: " + fullFileSize);
        System.out.println("StorageTest.uploadFileByFrags() - totalFragsNumber: " + totalFragsNumber);
        System.out.println("StorageTest.uploadFileByFrags() - totalEntireFragsNumber: " + totalEntireFragsNumber);

        //устанавливаем началные значения номера текущего фрагмента и стартового байта
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
            //отправляем на сервер объект сообщения(команды)
            connection.sendMessageObject(new CommandMessage(Commands.REQUEST_SERVER_FILE_FRAG_UPLOAD,
                    fileFragmentMessage));
        }

        //TODO temporarily
        System.out.println("StorageTest.uploadFileByFrags() - currentFragNumber: " + totalFragsNumber);
        System.out.println("StorageTest.uploadFileByFrags() - finalFileFragmentSize: " + finalFileFragmentSize);

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
            //отправляем на сервер объект сообщения(команды)
            connection.sendMessageObject(new CommandMessage(Commands.REQUEST_SERVER_FILE_FRAG_UPLOAD,
                    fileFragmentMessage));
        }

        //TODO temporarily
        long finish = System.currentTimeMillis() - start;
        System.out.println("StorageTest.uploadFileByFrags() - duration(mc): " + finish);
    }

    /**
     * Метод отправки целого файла размером менее константы максмального размера фрагмента файла
     * @param fromDir - директория(относительно корня) клиента где хранится файл источник
     * @param toDir - директория(относительно корня) в сетевом хранилище
     * @param filename - строковое имя файла
     * @param fileSize - размер файла в байтах
     */
    private void uploadEntireFile(String fromDir, String toDir, String filename, long fileSize) {
        //инициируем объект файлового сообщения
        FileMessage fileMessage = new FileMessage(fromDir, toDir, filename, fileSize);
        //читаем файл и записываем данные в байтовый массив объекта файлового сообщения
        //FIXME Разобраться с абсолютными папкими клиента
        //если скачивание прошло удачно
        if(fileUtils.readFile(currentClientDir, fileMessage)){
            //отправляем на сервер объект сообщения(команды)
            connection.sendMessageObject(new CommandMessage(Commands.REQUEST_SERVER_FILE_UPLOAD,
                    fileMessage));
            //если что-то пошло не так
        } else {
            //выводим сообщение
            printMsg("(Client)" + fileUtils.getMsg());
        }
    }

    //отправляем на сервер запрос на скачивание файла из облачного хранилища
    //FIXME перенести в контроллер интерфейса
    public void downloadFile(String fromDir, String toDir, String filename){
        //TODO temporarily
        printMsg("***StorageTest.downloadFile() - has started***");

        //инициируем объект файлового сообщения
        FileMessage fileMessage = new FileMessage(fromDir, toDir, filename);
        //отправляем на сервер объект сообщения(команды)
        connection.sendMessageObject(new CommandMessage(Commands.REQUEST_SERVER_FILE_DOWNLOAD,
                fileMessage));

        //TODO temporarily
        printMsg("***StorageTest.downloadFile() - has finished***");
    }

    public TCPConnection getConnection() {
        return connection;
    }

    public String getClientDefaultRoot() {
        return clientDefaultRoot;
    }

    //TODO temporarily
    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }

    public synchronized void printMsg(String msg){
        log.append(msg).append("\n");
    }

}
