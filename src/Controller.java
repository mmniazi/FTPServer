import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfigurationFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    private UserManager userManager;
    private FtpServer server;
    private Path rootDirPath;
    private Path userManagerFilePath;
    private Path keystorePath;
    private Stage stage;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField passwordField;

    @FXML
    private TextArea progressArea;

    @FXML
    private TextField rootLocationField;

    @FXML
    private CheckBox ftpCheckBox;

    @FXML
    private CheckBox ftpsCheckBox;

    @FXML
    void CreateUser(ActionEvent event) {
        //Creating a user and adding it to user manager
        BaseUser user = new BaseUser();
        user.setName(usernameField.getText());
        user.setPassword(passwordField.getText());
        Path userHomeDirectory;
        try {
            userHomeDirectory = Files.createDirectory(Paths.get(rootDirPath.toString() + "/" + user.getName()));
            user.setHomeDirectory(userHomeDirectory.toString());
            List<Authority> authorities = new ArrayList<>();
            authorities.add(new WritePermission());
            user.setAuthorities(authorities);
            //Save the user to the user list on the filesystem
            userManager.save(user);
            progressArea.appendText(usernameField.getText() + " user created.\n");
        } catch (IOException | FtpException exception) {
            exception.printStackTrace(System.err);
            progressArea.appendText(usernameField.getText() + " user creation failed.\n");
        }
    }

    @FXML
    void StartServer(ActionEvent event) {
        if (server == null || server.isStopped()) {
            createRequiredFiles();
            FtpServerFactory serverFactory = new FtpServerFactory();

            if (ftpCheckBox.isSelected()) {
                ListenerFactory defaultFactory = new ListenerFactory();
                // Listen on 29745 for FTP
                defaultFactory.setPort(29745);
                // Creating a default listener
                serverFactory.addListener("default", defaultFactory.createListener());

            }

            if (ftpsCheckBox.isSelected()) {
                ListenerFactory secureFactory = new ListenerFactory();
                // Listen on 29746 for FTPS
                secureFactory.setPort(29746);
                // define SSL configuration
                SslConfigurationFactory ssl = new SslConfigurationFactory();
                ssl.setKeystoreFile(keystorePath.toFile());
                ssl.setKeystorePassword("password");
                // set the SSL configuration for the listener
                secureFactory.setSslConfiguration(ssl.createSslConfiguration());
                secureFactory.setImplicitSsl(true);
                // Creating a secure listener
                serverFactory.addListener("secure", secureFactory.createListener());
            }

            if (ftpCheckBox.isSelected() || ftpsCheckBox.isSelected()) {
                PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
                // Setting user properties file to userManagerFactory
                userManagerFactory.setFile(userManagerFilePath.toFile());
                // Creating userManger using above settings
                userManager = userManagerFactory.createUserManager();
                serverFactory.setUserManager(userManager);
                server = serverFactory.createServer();

                // Start server if not running
                try {
                    server.start();
                    progressArea.appendText("Server started.\n");
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                    progressArea.appendText("Server could not be started." +
                            " Make sure another server is not running on same port no.\n");
                }
            }
        }
    }

    @FXML
    void StopServer(ActionEvent event) {
        // Stopping ftp server
        try {
            server.stop();
            progressArea.appendText("Server stopped.\n");
        } catch (Exception e) {
            progressArea.appendText("Server could not be stopped.\n");
            e.printStackTrace();
        }
    }

    @FXML
    void browseLocation(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Root Directory");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File selectedDirectory = chooser.showDialog(stage);
        if (!(selectedDirectory == null)) {
            rootLocationField.setText(selectedDirectory.toString());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        rootLocationField.setText(System.getProperty("user.home") + File.separator + "ftpHome");
    }

    private void createRequiredFiles() {
        // Creating FTP home directory if it does not exist
        // Home directory is created inside default user directory
        // In windows it corresponds to "C:\Users\{Your UserName}\"
        rootDirPath = Paths.get(rootLocationField.getText());
        try {
            if (Files.notExists(rootDirPath)) {
                Files.createDirectories(rootDirPath);
            }
        } catch (IOException exception) {
            exception.printStackTrace(System.err);
        }

        // Copy keystore file server.jks from inside jar to user home directory
        keystorePath = Paths.get(rootDirPath.toString() + "/server.jks");
        if (Files.notExists(keystorePath)) {
            try {
                Files.copy(getClass().getResourceAsStream("server.jks"), keystorePath);
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }

        // Setting user properties file location
        userManagerFilePath = Paths.get(rootDirPath.toString() + "/users.properties");
        // Creating user properties File if it does not exist
        try {
            if (Files.notExists(userManagerFilePath)) {
                Files.createFile(userManagerFilePath);
            }
        } catch (IOException exception) {
            exception.printStackTrace(System.err);
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }
}
