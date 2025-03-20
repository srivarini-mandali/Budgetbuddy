package Secondjavafx;


import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private void handleLoginButtonAction() {
        if (authenticate(usernameField.getText(), passwordField.getText())) {
            // Handle successful login
            System.out.println("Login successful");
        } else {
            // Handle failed login
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Login Failed");
            alert.setHeaderText(null);
            alert.setContentText("Incorrect username or password.");
            alert.showAndWait();
        }
    }

    private boolean authenticate(String username, String password) {
        // Replace with actual authentication logic
        return "admin".equals(username) && "admin".equals(password);
    }
}
