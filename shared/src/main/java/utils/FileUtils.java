package utils;

import messages.FileFragmentMessage;
import messages.FileMessage;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * The common class for operating with fileMessages and fileFragmentMessages.
 */
public class FileUtils {
    //инициируем строковую переменную для вывода сообщений
    private String msg;

    /**
     * Метод читает данные из целого файла в заданной директорию.
     * @param realItemPath - объект реального пути к объекту элемента
     * @param fileMessage - объект файлового сообщения
     * @return - результат чтения данных из файла
     */
    public boolean readFile(Path realItemPath, FileMessage fileMessage) {
        try {
            //считываем данные из файла и записываем их в объект файлового сообщения
            fileMessage.readFileData(realItemPath.toString());
            //записываем размер файла для скачивания
            fileMessage.setFileSize(Files.size(realItemPath));
            //если длина скачанного файла отличается от длины исходного файла в хранилище
            if(fileMessage.getFileSize() != fileMessage.getData().length){
                msg = "FileUtils.downloadFile() - Wrong the read file size!";
                return false;
            }
        } catch (IOException e) {
            msg = "FileUtils.downloadFile() - Something wrong with the directory or the file!";
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Метод сохраняет данные из байтового массива в целый файл.
     * @param realItemPath - объект реального пути к объекту элемента
     * @param data - байтовый массив из источника
     * @param fileSize - размер источника
     * @return - результат сохранения данных из байтового массива в целый файл
     */
    public boolean saveFile(Path realItemPath, byte[] data, long fileSize) {
        try {
            //создаем новый файл и записываем в него данные из объекта файлового сообщения
            Files.write(realItemPath, data, StandardOpenOption.CREATE);
            //если длина сохраненного файла отличается от длины принятого файла
            //проверяем сохраненный файл по контрольной сумме//FIXME
            if(Files.size(realItemPath) != fileSize){
                msg = "FileUtils.saveFile() - Wrong the saved file size!";
                return false;
            }
        } catch (IOException e) {
            msg = "FileUtils.saveFile() - Something wrong with the directory or the file!";
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Метод создает временную директорию, если нет, создает в ней временные файлы-фрагменты,
     * куда сохраняет данные из сообщения фрагмента файла.
     * @param toTempDir - временная папка для файлов-фрагментов
     * @param fileFragmentMessage - объект сообщения фрагмента файла
     * @return true, если файл-фрагмент сохранен без ошибок
     */
    public boolean saveFileFragment(String toTempDir, FileFragmentMessage fileFragmentMessage) {
        try {
            //инициируем объект пути к фрагменту файла
            //-1 из-за разницы начала нумерации фрагментов(с 1) и элементов массива(с 0)
            Path path = Paths.get(toTempDir,
                    fileFragmentMessage.getFragsNames()[fileFragmentMessage.getCurrentFragNumber() - 1]);

            //инициируем объект временной директории
            File dir = new File(toTempDir);//TODO возможно можно упростить?
            //если временной директории нет
            if(!dir.exists()){
                //создаем временную директорию
                dir.mkdir();
            }
            //создаем новый файл-фрагмент и записываем в него данные из объекта файлового сообщения
            Files.write(path, fileFragmentMessage.getData(), StandardOpenOption.CREATE);

            System.out.println("FileUtils.saveUploadedFileFragment() - " +
                    "Files.size(path): " + Files.size(path) +
                    ". fileFragmentMessage.getFileFragmentSize(): " +
                    fileFragmentMessage.getFileFragmentSize());

            //если длина сохраненного файла-фрагмента отличается от длины принятого фрагмента файла
            //проверяем сохраненный файл по контрольной сумме//FIXME добавить
            if(Files.size(path) != fileFragmentMessage.getFileFragmentSize()){
                msg = "FileUtils.saveUploadedFileFragment() - " +
                        "Wrong the saved file fragment size!";
                return false;
            }
        } catch (IOException e) {
            msg = "FileUtils.saveUploadedFileFragment() - " +
                    "Something wrong with the directory or the file!";
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Метод собирает целый файл из файлов-фрагментов, сохраненных во временной папке,
     * сохраняет его в директорию назначения и удаляет временную папку с файлами-фрагментами
     * @param toTempDir - временная папка для файлов-фрагментов
     * @param toDir - заданная директория для загрузки целого файла
     * @param fileFragmentMessage - объект сообщения фрагмента файла
     * @return true, если целый файл собран и сохранен без ошибок
     */
    public boolean compileFileFragments(
            String toTempDir, String toDir, FileFragmentMessage fileFragmentMessage
    ) {
        //TODO temporarily
        long start = System.currentTimeMillis();

        try {
            //инициируем объект пути к временной папке с фрагментами файла
            Path pathToFile = Paths.get(toDir, fileFragmentMessage.getFilename());
            //удаляем файл, если уже существует
            Files.deleteIfExists(pathToFile);
            //создаем новый файл для сборки загруженных фрагментов файла
            Files.createFile(pathToFile);

            //в цикле листаем временную папку и добавляем в файл данные из файлов-фрагментов
            for (int i = 1; i <= fileFragmentMessage.getFragsNames().length; i++) {
                //ищем требуемый фрагмент во временной папке и инициируем канал для чтения из него
                ReadableByteChannel source = Channels.newChannel(
                        Files.newInputStream(Paths.get(toTempDir, fileFragmentMessage.getFragsNames()[i - 1])));
                //инициируем канал для записи в файл назначения
                WritableByteChannel destination = Channels.newChannel(
                        Files.newOutputStream(pathToFile, StandardOpenOption.APPEND));
                //переписываем данные из файла фрагмента в файл-назначения через канал
                copyData(source, destination);
                //закрываем потоки и каналы
                source.close();
                destination.close();
            }

//            //добавлено по требованию IDEA
//            assert fragsNames != null;
//            //если количество файлов-фрагментов не совпадает с требуемым
//            if(fragsNames.length != fileFragmentMessage.getTotalFragsNumber()){
//                server.printMsg("(Server)FileCommandHandler.compileUploadedFileFragments() - " +
//                        "Wrong the saved file fragments count!");
//                return false;
//            }

            //если длина сохраненного файла-фрагмента отличается от длины принятого фрагмента файла
            if(Files.size(pathToFile) != fileFragmentMessage.getFullFileSize()){
                msg = "FileUtils.compileUploadedFileFragments() - " +
                        "Wrong the saved entire file size!";
                return false;
            //если файл собран без ошибок
            } else {
                //***удаляем временную папку***//FIXME уже есть метод (deleteFolder)
                //в цикле листаем временную папку и удаляем все файлы-фрагменты
                for (String fragName : fileFragmentMessage.getFragsNames()) {
                    //удаляем файл-фрагмент
                    Files.delete(Paths.get(toTempDir, fragName));
                }
                //теперь можем удалить пустую папку
                Files.delete(Paths.get(toTempDir));
            }
        } catch (IOException e) {
            msg = "FileUtils.compileUploadedFileFragments() - " +
                    "Something wrong with the directory or the file!";
            e.printStackTrace();
            return false;
        }

        //TODO temporarily
        long finish = System.currentTimeMillis() - start;
        System.out.println("FileUtils.compileUploadedFileFragments() - duration(mc): " + finish);

        return true;
    }

    /**
     * Метод переписывает данные из канала-источника в канал-назначения с применением буфера
     * @param source - канал чтения данных из источника
     * @param destination - канал записи данных
     * @throws IOException - исключение
     */
    private void copyData(ReadableByteChannel source, WritableByteChannel destination) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
        while (source.read(buffer) != -1) {
            // The Buffer Is Used To Be Drained
            buffer.flip();
            // Make Sure That The Buffer Was Fully Drained
            while (buffer.hasRemaining()) {
                destination.write(buffer);
            }
            // Now The Buffer Is Empty!
            buffer.clear();
        }
    }

    /**
     * Метод удаляет файловый объект.
     * @param fileObject - файловый объект
     * @return true - удаление прошло успешно
     */
    public boolean deleteFileObject(File fileObject) {
        boolean result;
        //если это директория
        if(fileObject.isDirectory()){
            //очищаем и удаляем папку
            result = deleteFolder(fileObject);
        } else{
            //удаляем файл
            result = fileObject.delete();
        }
        return result;
    }

    /**
     * Метод удаляет заданную папку и все объекты в ней.
     * @param folder - файловый объект заданной папки
     * @return true - удалена папка и все объекты в ней
     */
    private boolean deleteFolder(File folder) {
        //в цикле листаем временную папку и удаляем все файлы-фрагменты
        for (File f : Objects.requireNonNull(folder.listFiles())) {
            //если это директория
            if(f.isDirectory()){
                //очищаем и удаляем папку
                deleteFolder(f);
            } else{
                //удаляем файл
                f.delete();
            }
        }
        //теперь можем удалить пустую папку
        return Objects.requireNonNull(folder.listFiles()).length == 0 && folder.delete();
    }

    public String getMsg() {
        return msg;
    }
}
