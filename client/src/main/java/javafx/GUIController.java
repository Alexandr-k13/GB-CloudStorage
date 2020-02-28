package javafx;

import control.CloudStorageClient;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import utils.Item;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The client class is for operations with GUI.
 */
public class GUIController implements Initializable {
    //объявляем объекты пунктов верхнего меню
    @FXML
    private MenuItem disconnectMenuItem, changePasswordMenuItem;

    @FXML
    private StackPane connectToCloudStorageStackPane;

    @FXML
    private Button connectToCloudStorageButton;

    //объявляем объекты кнопок для коллекции файловых объектов клиента и сервера
    @FXML
    private Button clientHomeButton, storageHomeButton,//"В корневую директорию"
            clientGoUpButton, storageGoUpButton,//"Подняться на папку выше"
            clientRefreshButton, storageRefreshButton,//обновить папку
            clientNewFolderButton, storageNewFolderButton;//"Создать новую папку"

    //объявляем объекты меток для коллекций файловых объектов
    @FXML
    private Label clientDirLabel, storageDirLabel;

    //объявляем объекты коллекций объектов элементов
    @FXML
    private ListView<Item> clientItemListView, storageItemListView;

    //объявляем объект метки уведомлений
    @FXML
    private Label noticeLabel;

    //объявляем объект контроллера клиента облачного хранилища
    private CloudStorageClient storageClient;
    //инициируем константу строки названия директории по умолчанию относительно корневой директории
    // для списка в клиентской части GUI
    private final String CLIENT_DEFAULT_DIR = "";
    //инициируем константу строки названия корневой директории для списка в серверной части GUI
    private final String STORAGE_DEFAULT_DIR = "";
    //объявляем объекты директории по умолчанию в клиентской и серверной части GUI
    private Item clientDefaultDirItem, storageDefaultDirItem;
    //объявляем объекты текущей папки списка объектов элемента в клиентской и серверной части GUI
    private Item clientCurrentDirItem, storageCurrentDirItem;
    //объявляем переменную введенного нового имени объекта элемента
    private String newName = "";
    //объявляем переменную стадии приложения
    private Stage stage;
    //объявляем объект контроллера окна регистрации
    private RegistrationController registrationController;
    //объявляем объект контроллера окна авторизации
    private AuthorisationController authorisationController;
    //объявляем объект контроллера окна изменения пароля пользователя
    private ChangePasswordController changePasswordController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //инициируем объект клиента облачного хранилища
        storageClient = new CloudStorageClient(GUIController.this);
        //устанавливаем настройки приложения
        storageClient.initConfiguration();
        //инициируем объекты директории по умолчанию в клиентской и серверной части GUI
        clientDefaultDirItem = new Item(CLIENT_DEFAULT_DIR);
        storageDefaultDirItem = new Item(STORAGE_DEFAULT_DIR);
        //выводим текст в метку
        noticeLabel.setText("Server disconnected. Press \"Connect to the Cloud Storage\" button.");
        //инициируем в клиентской части интерфейса список объектов в директории по умолчанию
        initializeClientItemListView();
        //инициируем в серверной части интерфейса список объектов в директории по умолчанию
        initializeStorageItemListView();
    }

    /**
     * Метод обрабатывает нажатие connectToCloudStorageButton и запускает процесс
     * подключения к серверу облачного хранилища.
     */
    @FXML
    private void onConnectToCloudStorageButtonClick() {
        //выводим текст в метку
        noticeLabel.setText("Connecting to the Cloud Storage server, please wait..");
        //в отдельном потоке
        new Thread(() -> {
            try {
                //запускаем логику клиента облачного хранилища
                storageClient.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Метод-прокладка запускает процессы: показа окна авторизации в режиме авторизации
     * и процесс регистрации пользователя в сетевом хранилище.
     * @param login - логин пользователя
     * @param first_name - имя пользователя
     * @param last_name - фамилия пользователя
     * @param email - email пользователя
     * @param password - пароль пользователя
     */
    public void demandRegistration(String login, String first_name, String last_name,
                                   String email, String password) {
        //если окно авторизации закрыто штатно(не закрыто по крестику выхода)
        //FIXME добавить проверку всех полей формы
        if(isLoginPasswordNotEmpty(login, password)){//TODO лишняя проверка? (см.demandChangePassword)
            //запускаем процесс авторизации
            storageClient.demandRegistration(login, first_name, last_name, email, password);
            //если окно закрыто по крестику выхода
        } else {
            noticeLabel.setText("");
        }
    }

    /**
     * Метод-прокладка запускает процесс показа основного окна и процесс
     * авторизации пользователя в сетевом хранилище.
     * @param login - логин пользователя
     * @param password - пароль пользователя
     */
    public void demandAuthorisation(String login, String password) {
        //если окно авторизации закрыто штатно(не закрыто по крестику выхода)
        if(isLoginPasswordNotEmpty(login, password)){//TODO лишняя проверка? (см.demandChangePassword)
            //запускаем процесс авторизации
            storageClient.demandAuthorization(login, password);
            //если окно закрыто по крестику выхода
        } else {
            noticeLabel.setText("");
        }
    }

    /**
     * Метод-прокладка запускает процесс отправки запроса на изменение пароля пользователя
     * в сетевое хранилище.
     * @param login - логин пользователя
     * @param password - текущий пароль пользователя
     * @param newPassword - новый пароль пользователя
     */
    public void demandChangePassword(String login, String password, String newPassword) {
        storageClient.demandChangePassword(login, password, newPassword);
    }

    /**
     * Метод инициирует в клиентской части интерфейса список объектов в директории по умолчанию
     */
    public void initializeClientItemListView() {
        //выводим в клиентской части интерфейса список объектов в директории по умолчанию
        updateClientItemListInGUI(clientDefaultDirItem);
    }

    /**
     * Метод инициирует в серверной части интерфейса список объектов в директории по умолчанию
     */
    public void initializeStorageItemListView() {
        //выводим в клиентской части интерфейса список объектов в директории по умолчанию
        updateStorageItemListInGUI(new Item(STORAGE_DEFAULT_DIR),
                new Item[]{new Item("waiting for an item list from the server...",
                        "", "waiting for an item list from the server...",
                        "", false)});
    }

    /**
     * Метод обновляет список элементов списка в заданной директории клиентской части
     * @param directoryItem - объект элемента заданной директории
     */
    public void updateClientItemListInGUI(Item directoryItem) {
        //обновляем объект текущей директории
        clientCurrentDirItem = directoryItem;
        //в отдельном потоке запускаем обновление интерфейса
        Platform.runLater(() -> {
            //записываем в метку относительный строковый путь текущей директории
            clientDirLabel.setText(">>" + clientCurrentDirItem.getItemPathname());
            //обновляем заданный список объектов элемента
            updateListView(clientItemListView, storageClient.clientItemsList(clientCurrentDirItem));
        });
    }

    /**
     * Метод выводит в GUI список объектов(файлов и папок)
     * в корневой пользовательской директории в сетевом хранилище.
     * @param directoryItem - объект полученной пользовательской директории в сетевом хранилище
     * @param items - массив объектов элементов в директории
     */
    public void updateStorageItemListInGUI(Item directoryItem, Item[] items){
        //обновляем объект текущей директории
        storageCurrentDirItem = directoryItem;
        //в отдельном потоке запускаем обновление интерфейса
        Platform.runLater(() -> {
            //выводим текущую директорию в метку серверной части
            storageDirLabel.setText(">>" + storageCurrentDirItem.getItemPathname());
            //обновляем заданный список файловых объектов
            updateListView(storageItemListView, items);
        });
    }

    /**
     * Метод обновляет заданный список объектов элемента.
     * @param listView - коллекция объектов элемента
     * @param items - массив объектов элемента
     */
    private void updateListView(ListView<Item> listView, Item[] items) {
        //очищаем список элементов
        listView.getItems().clear();
        //обновляем список элементов списка
        listView.getItems().addAll(items);
        //инициируем объект кастомизированного элемента списка
        listView.setCellFactory(itemListView -> new FileListCell());
        //инициируем контекстное меню
        setContextMenu(listView);
    }

    /**
     * Метод инициирует контекстное меню для переданной в параметре коллекции объектов элемента.
     * @param listView - коллекция объектов элемента
     */
    private void setContextMenu(ListView<Item> listView){
        //инициируем объект контестного меню
        ContextMenu contextMenu = new ContextMenu();
        //если текущий список клиентский
        if(listView.equals(clientItemListView)){
            // добавляем скопом элементы в контестное меню
            contextMenu.getItems().add(menuItemUpload(listView));
            //если текущий список облачного хранилища
        } else if(listView.equals(storageItemListView)){
            // добавляем скопом элементы в контестное меню
            contextMenu.getItems().add(menuItemDownload(listView));
        }
        // добавляем скопом оставщиеся элементы в контестное меню
        contextMenu.getItems().addAll(menuItemRename(listView), menuItemDelete(listView));
        //создаем временный элемент контекстного меню
        MenuItem menuItem = menuItemGetInto(listView);
        //устаналиваем событие на клик правой кнопки мыши по элементу списка
        listView.setOnContextMenuRequested(event -> {
            //если контекстное меню уже показывается или снова кликнуть на пустой элемент списка
            if(contextMenu.isShowing() ||
                    listView.getSelectionModel().getSelectedItems().isEmpty()){
                //скрываем контекстное меню
                contextMenu.hide();
                //очищаем выделение
                listView.getSelectionModel().clearSelection();
                return;
            }
            // и если выбранный элемент это директория
            if(listView.getSelectionModel().getSelectedItem().isDirectory()){
                //если контекстное меню не показывается
                if(!contextMenu.getItems().contains(menuItem)){
                    // добавляем элемент в контестное меню
                    contextMenu.getItems().add(0, menuItem);
                }
            //если не директория
            } else {
                // удаляем элемент из контестного меню
                contextMenu.getItems().remove(menuItem);
            }
            //показываем контекстное меню в точке клика(позиция левого-верхнего угла контекстного меню)
            contextMenu.show(listView, event.getScreenX(), event.getScreenY());
        });
    }

    /**
     * Метод инициирует элемент контекстного меню "Get into".
     * Запрашивает список объектов для выбранной директории.
     * @param listView - текущий список объектов элемента
     * @return - объект элемента контекстного меню "Get into"
     */
    private MenuItem menuItemGetInto(ListView<Item> listView) {
        //инициируем пункт контекстного меню "Получить список файловых объектов"
        MenuItem menuItemGetInto = new MenuItem("Get into");
        //устанавливаем обработчика нажатия на этот пункт контекстного меню
        menuItemGetInto.setOnAction(event -> {
            //запоминаем кликнутый элемент списка
            Item item = listView.getSelectionModel().getSelectedItem();
            //если текущий список клиентский
            if(listView.equals(clientItemListView)){
                //обновляем список объектов элемента клиентской части
                updateClientItemListInGUI(item);
            //если текущий список облачного хранилища
            } else if(listView.equals(storageItemListView)){
                //отправляем на сервер запрос на получение списка элементов заданной директории
                //пользователя в сетевом хранилище
                storageClient.demandDirectoryItemList(item.getItemPathname());
            }
            //сбрасываем выделение после действия
            listView.getSelectionModel().clearSelection();
        });
        return menuItemGetInto;
    }

    /**
     * Метод инициирует элемент контекстного меню "Загрузить в облачное хранилище"
     * @param listView - текущий список объектов элемента
     * @return - объект элемента контекстного меню "Upload"
     */
    private MenuItem menuItemUpload(ListView<Item> listView) {
        //инициируем пункт контекстного меню "Загрузить в облачное хранилище"
        MenuItem menuItemUpload = new MenuItem("Upload");
        //устанавливаем обработчика нажатия на этот пункт контекстного меню
        menuItemUpload.setOnAction(event -> {
            //запоминаем кликнутый элемент списка
            Item item = listView.getSelectionModel().getSelectedItem();
            try {
                //выводим сообщение в нижнюю метку
                noticeLabel.setText("Uploading a file...");
                //отправляем на сервер запрос на загрузку файла в облачное хранилище
                storageClient.demandUploadItem(storageCurrentDirItem, item);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //сбрасываем выделение после действия
            listView.getSelectionModel().clearSelection();
        });
        return menuItemUpload;
    }

    /**
     * Метод инициирует элемент контекстного меню "Скачать из облачного хранилища"
     * @param listView - текущий список объектов элемента
     * @return - объект элемента контекстного меню "Download"
     */
    private MenuItem menuItemDownload(ListView<Item> listView) {
        //инициируем пункт контекстного меню "Скачать из облачного хранилища"
        MenuItem menuItemDownload = new MenuItem("Download");
        //устанавливаем обработчика нажатия на этот пункт контекстного меню
        menuItemDownload.setOnAction(event -> {
            //запоминаем кликнутый элемент списка
            Item item = listView.getSelectionModel().getSelectedItem();
            //выводим сообщение в нижнюю метку
            noticeLabel.setText("File downloading. Waiting for a cloud server response...");
            //отправляем на сервер запрос на скачивание файла из облачного хранилища
            storageClient.demandDownloadItem(storageCurrentDirItem, clientCurrentDirItem, item);
            //сбрасываем выделение после действия
            listView.getSelectionModel().clearSelection();
        });
        return menuItemDownload;
    }

    /**
     * Метод инициирует элемент контекстного меню "Переименовать"
     * @param listView - текущий список объектов элемента
     * @return - объект элемента контекстного меню "Rename"
     */
    private MenuItem menuItemRename(ListView<Item> listView) {
        //инициируем пункт контекстного меню "Переименовать"
        MenuItem menuItemRename = new MenuItem("Rename");
        //устанавливаем обработчика нажатия на этот пункт контекстного меню
        menuItemRename.setOnAction(event -> {
            //запоминаем выбранный элемент списка
            Item origin = listView.getSelectionModel().getSelectedItem();
            //открываем диалоговое окно переименования файлового объекта
            //если окно было просто закрыто по крестику, то выходим без действий
            if(!openNewNameWindow(origin)){
                return;
            }

            //если текущий список клиентский
            if(listView.equals(clientItemListView)){
                //переименовываем файловый объект
                //если произошла ошибка при переименовании
                if(!storageClient.renameClientItem(origin, newName)){
                    //TODO добавить диалоговое окно - предупреждение об ошибке
                    writeToLog("GUIController.menuItemRename() - Some thing wrong with item renaming!");
                }
                //обновляем список объектов элемента в текущей директории
                updateClientItemListInGUI(clientCurrentDirItem);
                //если текущий список облачного хранилища
            } else if(listView.equals(storageItemListView)){
                //отправляем на сервер запрос на переименования объекта в заданной директории
                //пользователя в сетевом хранилище
                storageClient.demandRenameItem(storageCurrentDirItem, origin, newName);
            }
            //сбрасываем выделение после действия
            listView.getSelectionModel().clearSelection();
            //очищаем переменную имени
            newName = "";
        });
        return menuItemRename;
    }

    /**
     * Метод инициирует элемент контекстного меню "Удалить"
     * @param listView - текущий список объектов элемента
     * @return - объект элемента контекстного меню "Delete"
     */
    private MenuItem menuItemDelete(ListView<Item> listView) {
        //инициируем пункт контекстного меню "Удалить"
        MenuItem menuItemDelete = new MenuItem("Delete");
        //устанавливаем обработчика нажатия на этот пункт контекстного меню
        menuItemDelete.setOnAction(event -> {
            //TODO добавить диалоговое окно - предупреждение-подтверждение

            //запоминаем выбранный элемент списка
            Item item = listView.getSelectionModel().getSelectedItem();
            //если текущий список клиентский
            if(listView.equals(clientItemListView)){
                //удаляем файл или папку в текущей директории на клиенте
                //если произошла ошибка при удалении
                if(!storageClient.deleteClientItem(item)){
                    //TODO добавить диалоговое окно - предупреждение об ошибке
                    writeToLog("GUIController.menuItemRename() - Some thing wrong with item deleting!");
                }
                //обновляем список элементов списка клиентской части
                updateClientItemListInGUI(clientCurrentDirItem);
            //если текущий список облачного хранилища
            } else if(listView.equals(storageItemListView)){
                //отправляем на сервер запрос на удаление объекта в заданной директории
                //пользователя в сетевом хранилище
                storageClient.demandDeleteItem(storageCurrentDirItem, item);
            }
            //сбрасываем выделение после действия
            listView.getSelectionModel().clearSelection();
        });
        return menuItemDelete;
    }

    /**
     * Метод отрабатывает нажатие на пунтк меню "About".
     * @param actionEvent - событие(здесь клик мыши)
     */
    @FXML
    public void onAboutMenuItemClick(ActionEvent actionEvent) {
        //открываем сцену с информацией о программе
        openAboutScene();
    }

    /** //FIXME Убрать задвоение - вызвать перегруженный метод openNewNameWindow(Item origin)
     * Метод отрабатывает нажатие на пунтк меню "Change Client Root".
     * @param actionEvent - событие(здесь клик мыши)
     */
    @FXML
    public void onChangeClientRootMenuItemClick(ActionEvent actionEvent) {
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/changeRoot.fxml"));
            Parent root = loader.load();
            ChangeRootController changeRootController = loader.getController();

            //определяем действия по событию закрыть окно по крестику через лямбда
            stage.setOnCloseRequest(event -> {
                writeToLog("GUIController.menuItemRename() - " +
                        "the newNameWindow was closed forcibly!");
                //сбрасываем текстовое поле имени пути
                changeRootController.setNewPathnameText("");
            });

            //записываем текущее имя в текстовое поле
            changeRootController.setNewPathnameText(CloudStorageClient.CLIENT_ROOT_PATH.toAbsolutePath().toString());
            changeRootController.setBackController(this);

            stage.setTitle("insert a new root pathname");
            stage.setScene(new Scene(root, 800, 50));
            stage.isAlwaysOnTop();
            stage.setResizable(false);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод отрабатывает нажатие на пункт меню "ChangePassword".
     * @param actionEvent - событие(здесь клик мыши)
     */
    @FXML
    public void onChangePasswordMenuItemClick(ActionEvent actionEvent) {
        //запускаем процесс отправки запроса на изменение пароля пользователя
        openChangingPasswordWindow();
    }

    /**
     * Метод отрабатывает нажатие на пункт меню "Disconnect".
     * @param actionEvent - событие(здесь клик мыши)
     */
    @FXML
    public void onDisconnectMenuItemClick(ActionEvent actionEvent) {
        writeToLog("GUIController.onDisconnectLinkClick()");
        //запускаем процесс отправки запроса на отключение
        storageClient.demandDisconnect();
    }

    /**
     * Метод отрабатывает нажатие на кнопку "Home" в клиентской части GUI.
     * Выводит список объектов элемента в корневой директории в клиентской части
     * @param mouseEvent - любой клик мышкой
     */
    @FXML
    public void onClientHomeBtnClicked(MouseEvent mouseEvent) {
        updateClientItemListInGUI(clientDefaultDirItem);
    }

    /**
     * Метод отрабатывает нажатие на кнопку "Home" в серверной части GUI.
     * Запрашивает у сервера список объектов элемента в корневой директории в серверной части
     * @param mouseEvent - любой клик мышкой
     */
    @FXML
    public void onStorageHomeBtnClicked(MouseEvent mouseEvent) {
        storageClient.demandDirectoryItemList(STORAGE_DEFAULT_DIR);
    }

    /**
     * Метод отрабатывает нажатие на кнопку "GoUp" в клиентской части GUI.
     * Выводит список объектов элемента в родительской директории в клиентской части
     * @param mouseEvent - любой клик мышкой
     */
    @FXML
    public void onClientGoUpBtnClicked(MouseEvent mouseEvent) {
        //выводим список в родительской директории
        updateClientItemListInGUI(storageClient.getParentDirItem(
                clientCurrentDirItem, clientDefaultDirItem,
                CloudStorageClient.CLIENT_ROOT_PATH));
    }

    /**
     * Метод отрабатывает нажатие на кнопку "GoUp" в серверной части GUI.
     * Запрашивает у сервера список объектов элемента в родительской директории в серверной части
     * @param mouseEvent - любой клик мышкой
     */
    @FXML
    public void onStorageGoUpBtnClicked(MouseEvent mouseEvent) {
        storageClient.demandDirectoryItemList(storageCurrentDirItem.getParentPathname());
    }

    /**
     * Метод отрабатывает нажатие на кнопку "Refresh" в клиентской части GUI.
     * Запрашивает у сервера список объектов элемента в текущей директории в клиентской части
     * @param mouseEvent - любой клик мышкой
     */
    @FXML
    public void onClientRefreshBtnClicked(MouseEvent mouseEvent) {
        //обновляем список объектов элемента клиентской части
        updateClientItemListInGUI(clientCurrentDirItem);
    }

    /**
     * Метод отрабатывает нажатие на кнопку "Refresh" в серверной части GUI.
     * Запрашивает у сервера список объектов элемента в текущей директории в серверной части
     * @param mouseEvent - любой клик мышкой
     */
    @FXML
    public void onStorageRefreshBtnClicked(MouseEvent mouseEvent) {
        //отправляем на сервер запрос на получение списка элементов заданной директории
        //пользователя в сетевом хранилище
        storageClient.demandDirectoryItemList(storageCurrentDirItem.getItemPathname());
    }

    /**
     * Метод отрабатывает нажатие на кнопку "NewFolder" в клиентской части GUI.
     * Запрашивает у сервера создать новую папку в текущей директории в клиентской части
     * @param mouseEvent - любой клик мышкой
     */
    @FXML
    public void onClientNewFolderBtnClicked(MouseEvent mouseEvent) {
        //открываем модальное окно для ввода нового имени
        openNewNameWindow();
        //если окно было закрыто штатно, а не по крестику выхода
        if(newName.isEmpty()){
            return;
        }
        //если новая папка создана удачно
        if(!storageClient.createNewFolder(storageCurrentDirItem.getItemPathname(), newName)){
            noticeLabel.setText("A folder has not created!");
            return;
        }
        //обновляем список объектов в текущей клиентской директории
        updateClientItemListInGUI(clientCurrentDirItem);
    }

    /**
     * Метод отрабатывает нажатие на кнопку "NewFolder" в серверной части GUI.
     * Запрашивает у сервера создать новую папку в текущей директории в серверной части
     * @param mouseEvent - любой клик мышкой
     */
    @FXML
    public void onStorageNewFolderBtnClicked(MouseEvent mouseEvent) {
        //открываем модальное окно для ввода нового имени
        openNewNameWindow();
        //если окно было закрыто штатно, а не по крестику выхода
        if(newName.isEmpty()){
            return;
        }
        //отправляем на сервер запрос на получение списка элементов заданной директории
        //пользователя в сетевом хранилище
        storageClient.demandCreateNewDirectory(storageCurrentDirItem.getItemPathname(), newName);
    }

    /**
     * Перегруженный метод открывает модальное окно для ввода логина и пароля пользователя.
     */
    void openAuthorisationWindow() {
        //вызываем перегруженный метод с пустыми логином и паролем
        openAuthorisationWindow("", "");
    }

    /**
     * Перегруженный метод открывает модальное окно для ввода логина и пароля пользователя.
     * @param login - логин пользователя
     * @param password - пароль пользователя
     */
    void openAuthorisationWindow(String login, String password) {
        //выводим сообщение в нижнюю метку GUI
        noticeLabel.setText("Server has connected, insert login and password.");

        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/authorisation.fxml"));
            Parent root = loader.load();
            authorisationController = loader.getController();
            //сохраняем ссылку на контроллер открываемого окна авторизации/регистрации
            authorisationController.setBackController(this);
            //устанавливаем значения из формы регистрации
            authorisationController.setLoginString(login);
            authorisationController.setPasswordString(password);

            //определяем действия по событию закрыть окно по крестику через лямбда
            stage.setOnCloseRequest(event -> {
                //вызываем разрыв соединения, если выйти по крестику
                GUIController.this.setAuthorizedMode(false);
            });

            stage.setTitle("Authorisation to the Cloud Storage by LYS");
            stage.setScene(new Scene(root, 300, 200));
            stage.isAlwaysOnTop();
            stage.setResizable(false);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод открывает модальное окно с регистрационной формой пользователя.
     */
    void openRegistrationWindow() {
        //выводим сообщение в нижнюю метку GUI
        noticeLabel.setText("Insert your data to get registration please.");

        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/registration_form.fxml"));
            Parent root = loader.load();
            registrationController = loader.getController();
            //сохраняем ссылку на контроллер открываемого окна авторизации/регистрации
            registrationController.setBackController(this);

            //определяем действия по событию закрыть окно по крестику через лямбда
            stage.setOnCloseRequest(event -> {
                //вызываем разрыв соединения, если выйти по крестику
                GUIController.this.setAuthorizedMode(false);
            });

            stage.setTitle("Registration to the Cloud Storage by LYS");
            stage.setScene(new Scene(root, 300, 300));
            stage.isAlwaysOnTop();
            stage.setResizable(false);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** //FIXME Убрать задвоение - вызвать универсальный метод открытия модального окна
     * Метод открывает модальное окно с формой изменения пароля.
     */
    private void openChangingPasswordWindow() {
        //выводим сообщение в нижнюю метку GUI
        noticeLabel.setText("Fill a Changing Password Form please.");
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/changePassword.fxml"));
            Parent root = loader.load();
            changePasswordController = loader.getController();
            //сохраняем ссылку на контроллер открываемого окна авторизации/регистрации
            changePasswordController.setBackController(this);

            //определяем действия по событию закрыть окно по крестику через лямбда
            stage.setOnCloseRequest(event -> {
                //вызываем разрыв соединения, если выйти по крестику
                GUIController.this.setAuthorizedMode(true);
            });

            stage.setTitle("Changing Password Form to the Cloud Storage by LYS");
            stage.setScene(new Scene(root, 300, 200));
            stage.isAlwaysOnTop();
            stage.setResizable(false);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** //FIXME Убрать задвоение - вызвать перегруженный метод openNewNameWindow(Item origin)
     * Перегруженный метод открывает модальное окно для ввода нового имени элемента списка.
     */
    private void openNewNameWindow() {
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/rename.fxml"));
            Parent root = loader.load();
            RenameController renameController = loader.getController();

            //определяем действия по событию закрыть окно по крестику через лямбда
            stage.setOnCloseRequest(event -> {
                writeToLog("GUIController.menuItemRename() - " +
                        "the newNameWindow was closed forcibly!");
                //сбрасываем текстовое поле имени
                GUIController.this.newName = "";
            });
            //запоминаем текущий контроллер для возврата
            renameController.setBackController(this);

            stage.setTitle("insert a new name");
            stage.setScene(new Scene(root, 200, 50));
            stage.isAlwaysOnTop();
            stage.setResizable(false);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Перегруженный метод открывает модальное окно для ввода нового имени элемента списка.
     * @param origin - объект элемента - оригинал
     * @return false - если закрыть окно принудительно, true - при штатном вводе
     */
    private boolean openNewNameWindow(Item origin) {
        AtomicBoolean flag = new AtomicBoolean(false);
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/rename.fxml"));
            Parent root = loader.load();
            RenameController renameController = loader.getController();

            //определяем действия по событию закрыть окно по крестику через лямбда
            stage.setOnCloseRequest(event -> {
                writeToLog("GUIController.menuItemRename() - " +
                        "the newNameWindow was closed forcibly!");
                //сбрасываем флаг
                flag.set(false);
                //сбрасываем текстовое поле имени
                GUIController.this.newName = "";
            });
            //записываем текущее имя в текстовое поле
            renameController.setNewNameString(origin.getItemName());
            //запоминаем текущий контроллер для возврата
            renameController.setBackController(this);

            stage.setTitle("insert a new name");
            stage.setScene(new Scene(root, 200, 50));
            stage.isAlwaysOnTop();
            stage.setResizable(false);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Метод открывает сцену с информацией о программе.
     */
    private void openAboutScene() {
        //получаем объект стадии приложения
        stage = (Stage) noticeLabel.getScene().getWindow();
        //сохраням объект гланой сцены
        Scene primaryScene = noticeLabel.getScene();
        //инициируем элементы сцены
        VBox pane = new VBox(10);
        pane.setAlignment(Pos.TOP_CENTER);
        Label label = new Label(new AboutText().getText());
        label.setStyle("-fx-font-size: 14");
        label.setWrapText(true);
        Button backBtn = new Button("Return");
        //устанавливаем лисенер на кнопку, чтобы вернуться с основной сцене
        backBtn.setOnAction(e -> stage.setScene(primaryScene));
        pane.getChildren().addAll(label, backBtn);
        Scene aboutScene = new Scene(pane, 410, 220);
        stage.setScene(aboutScene);
    }

    /**
     * Метод-прокладка, чтобы открывать окно GUI в других потоках
     */
    public void openAuthWindowInGUI() {
        //в отдельном потоке запускаем обновление интерфейса
        //открываем окно авторизации
        Platform.runLater(this::openAuthorisationWindow);
    }

    /**
     * Метод устанавливает режим отображения GUI "Отсоединен" или "Подсоединен".
     * @param isDisconnectedMode - если true - "Отсоединен"
     */
    public void setDisconnectedMode(boolean isDisconnectedMode) {
        //показываем и активируем(если isDisconnectedMode = true)
        // панель с кнопкой подключения к серверу
        connectToCloudStorageStackPane.setManaged(isDisconnectedMode);
        connectToCloudStorageStackPane.setVisible(isDisconnectedMode);
        //активируем кнопку connectToCloudStorageButton
        connectToCloudStorageButton.setDisable(!isDisconnectedMode);

        //скрываем и деактивируем (если isDisconnectedMode = true)
        //деактивируем кнопки сетевого хранилища
        storageHomeButton.setDisable(isDisconnectedMode);
        storageGoUpButton.setDisable(isDisconnectedMode);
        storageRefreshButton.setDisable(isDisconnectedMode);
        storageNewFolderButton.setDisable(isDisconnectedMode);
        // список объектов в сетевом хранилище
        storageItemListView.setManaged(!isDisconnectedMode);
        storageItemListView.setVisible(!isDisconnectedMode);

        //деактивируем пункт меню Disconnect
        disconnectMenuItem.setDisable(isDisconnectedMode);
        //деактивируем пункт меню ChangePassword
        changePasswordMenuItem.setDisable(isDisconnectedMode);
    }

    /**
     * Метод устанавливает GUI в режим авторизован или нет, в зависимости от параметра
     * @param isAuthMode - true - сервер авторизовал пользователя
     */
    public void setAuthorizedMode(boolean isAuthMode) {
        //активируем/деактивируем кнопки сетевого хранилища
        storageHomeButton.setDisable(!isAuthMode);
        storageGoUpButton.setDisable(!isAuthMode);
        storageRefreshButton.setDisable(!isAuthMode);
        storageNewFolderButton.setDisable(!isAuthMode);
        //скрываем и деактивируем(если isAuthMode = true) кнопку подключения к серверу
        connectToCloudStorageStackPane.setManaged(!isAuthMode);
        connectToCloudStorageStackPane.setVisible(!isAuthMode);
        //показываем и активируем(если isAuthMode = true) список объектов в сетевом хранилище
        storageItemListView.setManaged(isAuthMode);
        storageItemListView.setVisible(isAuthMode);
        //если авторизация получена
        if(isAuthMode){
            //если объект контроллера регистрации не нулевой
            if(registrationController != null){
                //закрываем окно формы в потоке JavaFX
                Platform.runLater(() -> registrationController.hideWindow());
            }
            //если объект контроллера авторизации не нулевой
            if(authorisationController != null){
                //закрываем окно формы в потоке JavaFX
                Platform.runLater(() -> authorisationController.hideWindow());
            }
            //если объект контроллера изменения пароля пользователя не нулевой
            if(changePasswordController != null){
                //закрываем окно формы в потоке JavaFX
                Platform.runLater(() -> changePasswordController.hideWindow());
            }
        }
    }

    /**
     * Метод открывает окно регистрации.
     */
    public void setRegistrationFormMode(){
        //если объект контроллера регистрации не нулевой
        if(authorisationController != null){
            //закрываем окно формы в потоке JavaFX
            Platform.runLater(() -> authorisationController.hideWindow());
        }
        //открываем окно регистрации с пустыми полями
        openRegistrationWindow();
    }

    /**
     * Метод открывает окно авторизации.
     */
    public void setAuthorizationFormMode() {
        //если объект контроллера регистрации не нулевой
        if(registrationController != null){
            //закрываем окно формы в потоке JavaFX
            Platform.runLater(() -> registrationController.hideWindow());
        }
        //открываем окно авторизации с пустыми логином и паролем
        openAuthorisationWindow();
    }

    /**
     * Метод устанавливает режим "Зарегистрирован, но не авторизован" - скрывает
     * окно регистрации и открывает окно авторизации с логином и паролем,
     * сохраненными из регистрационной формы.
     */
    public void setRegisteredAndUnauthorisedMode() {
        //инициируем переменные межпотоковые переменные для логин аи пароля
        AtomicReference<String>  loginAtomic = new AtomicReference<>();
        AtomicReference<String>  passwordAtomic = new AtomicReference<>();
        //если объект контроллера регистрации не нулевой
        if(registrationController != null){
            //закрываем окно формы в потоке JavaFX
            Platform.runLater(() -> {
                //сохраняем логин и пароль из регистрационной формы
                assert false;
                loginAtomic.set(registrationController.getLoginString());
                passwordAtomic.set(registrationController.getPasswordString());
                //закрываем окно регистрации
                registrationController.hideWindow();
            });
        }
        //делаем паузу мин. 100 ms, чтобы процесс успел завершиться до закрытия окна
        //без паузы мин 100 ms - значение loginAtomic.get() = null
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //открываем окно формы в потоке JavaFX //WORKS!
        Platform.runLater(() -> openAuthorisationWindow(loginAtomic.get(), passwordAtomic.get()));
    }

    /**
     * МЕтод проверяет не пустые ли поля логин и пароля, чтобы отлавливать закрытие окна
     * авторизации силой и не отправлять пустую форму на сервер.
     * @param login - логин пользователя
     * @param password - пароль пользователя
     * @return - true, оба не пустые
     */
    private boolean isLoginPasswordNotEmpty(String login, String password){
        return !login.isEmpty() && !password.isEmpty();
    }

    /**
     * Метод выводит в отдельном потоке(не javaFX) переданное сообщение в метку уведомлений.
     * @param text - строка сообщения
     */
    public void showTextInGUI(String text){
        //в отдельном потоке запускаем обновление интерфейса
        Platform.runLater(() -> {
            //выводим сообщение в нижнюю метку GUI
            noticeLabel.setText(text);
        });
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    /**
     * Метод записывает новое значение абсолютного пути к корневой папке клиента.
     * @param newPathname - новое значение абсолютного пути
     */
    public void setNewRootPathname(String newPathname) {
        //если введенное имя пути пустое, то выходим
        if(newPathname.trim().isEmpty()){
            return;
        }
        Path newPath = Paths.get(newPathname);
        //если новая директория существует и это действительно директория
        if(newPath.toFile().exists() && newPath.toFile().isDirectory()){
            //записываем новое значение в переменную корневой директории
            CloudStorageClient.CLIENT_ROOT_PATH = newPath;
            //сохраняем новое значение абсолютного пути
            storageClient.saveClientRootPathProperty(newPath.toString());
            //устанавливаем текущей директорией директорию по умолчанию
            updateClientItemListInGUI(clientDefaultDirItem);
            //устанавливаем текст в метку уведомлений
            noticeLabel.setText("The Client Root path has been changed!");
        } else {
            noticeLabel.setText("Something wrong with a new Client Root path!");
        }
    }

    public Label getNoticeLabel() {
        return noticeLabel;
    }

    //Метод отправки запроса об отключении на сервер
    public void dispose() {
        //запускаем процесс отправки запроса серверу на разрыв соединения
        storageClient.demandDisconnect();
        //делаем паузу 2 сек, чтобы процесс успел завершиться до закрытия окна
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void writeToLog(String msg){
        storageClient.writeToLog(msg);
    }

}
