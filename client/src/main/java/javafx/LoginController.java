package javafx;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginController {

    @FXML
    private VBox globParent;

    @FXML
    private VBox authorizationVBox, registrationVBox;

    @FXML
    private TextField login;

    @FXML
    private PasswordField password, passwordConfirm;

    @FXML
    private Button registrationButton, authorizationButton;

    //главный контроллер GUI
    private GUIController backController;

    /**
     * Метод отрабатывает клик линка "Registration" в авторизационной форме.
     * Открывает Регистрационную форму.
     * @param actionEvent - событие клик мыши
     */
    @FXML
    public void onRegistrationLinkClick(ActionEvent actionEvent) {
        //очищаем все поля формы авторизации/регистрации
        clearRegAuthFields();
        //выводим сообщение в метку оповещения в GUI
        backController.showTextInGUI("Insert your new registration data.");
        //устанавливаем режим отображения "Регистрационная форма"
        setRegistrationMode(true);
    }

    /**
     * Метод отрабатывает клик кнопки на кнопку "Registration".
     * Открывает Авторизационную форму и запускает процесс отправки запроса на сервер
     * для регистрации в сетевом хранилище.
     * @param actionEvent - событие клик мыши
     */
    @FXML
    public void onRegistrationBtnClick(ActionEvent actionEvent) {
        //если введенные регистрационные данные корректны
        if(isRegistrationDataCorrect(login.getText(), password.getText(), passwordConfirm.getText())){
            //выводим сообщение в метку оповещения в GUI
            backController.showTextInGUI("Your registration data has been sent. Wait please...");
            //запускаем процесс регистрации в сетевом хранилище
            backController.demandRegistration(login.getText(), password.getText());
        }
    }

    /**
     * Метод отрабатывает клик линка "Authorization" в регистрационной форме.
     * Открывает Авторизационную форму.
     * @param actionEvent - событие клик мыши
     */
    @FXML
    public void onAuthorizationLinkClick(ActionEvent actionEvent) {
        //выводим сообщение в метку уведомлений
        backController.getNoticeLabel().setText("Insert your login and password");
        //очищаем все поля формы авторизации/регистрации
        clearRegAuthFields();
        //возвращаемся в режим авторизации
        setRegistrationMode(false);
    }

    /**
     * Метод обрабатывает клик мыши по кнопке "Authorization" в диалоговом окне.
     * Запускает процесс отправки данных на сервер для автторизации.
     * @param actionEvent - клик мыши по кнопке "Authorization"
     */
    @FXML
    public void onAuthorizationBtnClick(ActionEvent actionEvent) {
        //если введенные логин и пароль корректны
        if(isLoginPasswordCorrect(login.getText(), password.getText())){
            //запускаем процесс авторизации
            backController.demandAuthorisation(login.getText(), password.getText());
        }
    }

    /**
     * Метод проверяет корректность введенной пары - логин и пароль
     * @param login - введенные логин
     * @param password - введенные пароль
     * @return - результат проверки корректности введенной пары - логин и пароль
     */
    private boolean isLoginPasswordCorrect(String login, String password){

        System.out.println("LoginController.isLoginPasswordCorrect() - login: " + login
                + ", password: " + password);
        //FIXME усилить проверку
        return !login.trim().isEmpty() && !password.trim().isEmpty();
    }

    /**
     * Метод проверяет корректность введенных данных в регистрационной форме.
     * @param login - введенный логин
     * @param password - введенный пароль
     * @param passwordConfirm - введенный второй раз пароль
     * @return - результат проверки корректности введенных данных в регистрационной форме
     */
    private boolean isRegistrationDataCorrect(String login, String password, String passwordConfirm){

        System.out.println("LoginController.isLoginPasswordCorrect() - login: " + login
                + ", password: " + password + ", passwordConfirm: " + passwordConfirm);
        //FIXME усилить проверку
        return !login.trim().isEmpty() && !password.trim().isEmpty() && !passwordConfirm.trim().isEmpty() &&
                password.equals(passwordConfirm);
    }

    /**
     * Метод устанавливает GUI в режим авторизован или нет, в зависимости от параметра
     * @param isRegMode - true - сервер авторизовал пользователя
     */
    public void setRegistrationMode(boolean isRegMode) {
        //скрываем и деактивируем(если isRegMode = true) кнопку подключения к серверу
        authorizationVBox.setManaged(!isRegMode);
        authorizationVBox.setVisible(!isRegMode);
        //активируем/деактивируем обработку нажатия кнопки Enter на клавиатуре
        authorizationButton.setDefaultButton(!isRegMode);

        //показываем и активируем(если isRegMode = true) список объектов в сетевом хранилище
        registrationVBox.setManaged(isRegMode);
        registrationVBox.setVisible(isRegMode);
        //активируем/деактивируем обработку нажатия кнопки Enter на клавиатуре
        registrationButton.setDefaultButton(isRegMode);
    }

    /**
     * Метод очистки полей в регистрационной/авторизационной форме.
     */
    private void clearRegAuthFields(){
        login.setText("");
        password.setText("");
        passwordConfirm.setText("");
    }

    /**
     * Метод закрывает окно.
     */
    public void hideWindow(){
        //если окно показывается
        if(globParent.getScene().getWindow().isShowing()){
            //закрываем окно
            globParent.getScene().getWindow().hide();
        }
    }

    public PasswordField getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setBackController(GUIController backController) {
        this.backController = backController;
    }
}
