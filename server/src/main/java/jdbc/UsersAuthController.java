package jdbc;

import io.netty.channel.ChannelHandlerContext;
import messages.AuthMessage;
import control.CloudStorageServer;
import java.util.HashMap;
import java.util.Map;

/**
 * Класс для организации сервиса авторизации и связи с БД
 * Связь БД и приложения осуществляется через посредника, JDBC драйвер(библиотека).
 */
public class UsersAuthController {
    //инициируем объект класса
    private static UsersAuthController ounInstance = new UsersAuthController();

    public static UsersAuthController getOunInstance(CloudStorageServer storageServer) {
        ounInstance.storageServer = storageServer;
        //инициируем множество авторизованных клиентов
        ounInstance.authorizedUsers = new HashMap<>();
        return ounInstance;
    }

    //принимаем объект сервера
    private CloudStorageServer storageServer;
    //объявляем множество авторизованных клиентов <соединение, логин>
    private Map<ChannelHandlerContext, String> authorizedUsers;
    //инициируем объект имитации соединения с БД
    UsersDB usersDB = UsersDB.getOwnInstance();

    /**
     * Метод-прокладка запускает процесс регистрации нового пользователя в БД
     * @param ctx - сетевое соединение
     * @param authMessage - объект авторизационного сообщения
     * @return - результат операции регистрации в БД
     */
    public boolean registerUser(ChannelHandlerContext ctx, AuthMessage authMessage) {
        //если пользователь с таким логином уже зарегистрирован в БД
        if(isUserRegistered(authMessage.getLogin())){
            //выводим сообщение в консоль
            printMsg("[server]UsersAuthController.registerUser() - A user with this login has been registered already!");
            //и выходим с false
            return false;
        }
        //если регистрация нового пользователя в БД прошла не удачно
        if(!addUserIntoDB(authMessage.getLogin(), authMessage.getPassword())){
            //выводим сообщение в консоль
            printMsg("[server]UsersAuthController.authorizeUser() - This user has not been registered yet!");
            //и выходим с false
            return false;
        }
        //если создание конрневой директории для нового пользователяпрошла не удачно
        if(!storageServer.createNewUserRootFolder(authMessage.getLogin())){
            //выводим сообщение в консоль
            printMsg("[server]UsersAuthController.authorizeUser() - This user's root directory exists already!");
            //и выходим с false
            return false;
        }
        return true;
    }

    /**
     * Метод обработки авторизации клиента в сетевом хранилище.
     * @param ctx - сетевое соединение
     * @param authMessage - объект авторизационного сообщения
     * @return true, если авторизация прошла успешно
     */
    public boolean authorizeUser(ChannelHandlerContext ctx, AuthMessage authMessage){
        //если пользователь еще не зарегистрирован в БД
        if(!isUserRegistered(authMessage.getLogin())){
            //выводим сообщение в консоль
            printMsg("[server]UsersAuthController.authorizeUser() - This user has not been registered yet!");
            //и выходим с false
            return false;
        }
        //если пользователь уже авторизован
        if(isUserAuthorized(ctx, authMessage.getLogin())){
            //выводим сообщение в консоль
            printMsg("[server]UsersAuthController.authorizeUser - This user has been authorised already!");
            //и выходим с false
            return false;
        }
        //авторизуем пользователя, если он еще не авторизован
        authorizedUsers.put(ctx, authMessage.getLogin());
        return true;
    }

    //Метод добавления данных пользователя в БД
    public boolean addUserIntoDB(String login, String password){
        return usersDB.addUserIntoMap(login, password);
    }

    //Метод проверки введенного логина в БД на уникальность(зарегистрирован уже такой логин?)
    public boolean isUserRegistered(String login) {
        return usersDB.isUserExistInMap(login);
    }

    /**
     * Метод проверяет не авторизован ли уже пользовательс таким логином.
     * @param ctx - сетевое соединение
     * @param login - логин пользователя
     * @return - результат проверки
     */
    private boolean isUserAuthorized(ChannelHandlerContext ctx, String login) {
        //возвращаем результат проверки есть ли уже элемент в списке авторизованных с такими
        // объектом соединения или логином
        return authorizedUsers.containsKey(ctx) || authorizedUsers.containsValue(login);
    }

    public void printMsg(String msg){
        storageServer.printMsg(msg);
    }

    public Map<ChannelHandlerContext, String> getAuthorizedUsers() {
        return authorizedUsers;
    }

}

