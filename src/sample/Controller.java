package sample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
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

    @FXML
    private TextField usernameField;

    @FXML
    private TextField passwordField;

    @FXML
    private TextField keyField;

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
        } catch (IOException | FtpException exception) {
            exception.printStackTrace(System.err);
        }
    }

    @FXML
    void StartServer(ActionEvent event) {
        // Start server if not running
        try {
            if (server.isStopped()) {
                server.start();
            }
        } catch (FtpException exception) {
            exception.printStackTrace(System.err);
        }
    }

    @FXML
    void StopServer(ActionEvent event) {
        // Stopping ftp server
        server.stop();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        createRequiredFiles();
        FtpServerFactory serverFactory = new FtpServerFactory();
        ListenerFactory defaultFactory = new ListenerFactory();
        ListenerFactory secureFactory = new ListenerFactory();
        // Listen on 29745 for FTP and 29746 for FTPS
        defaultFactory.setPort(29745);
        secureFactory.setPort(29746);

        // define SSL configuration
        SslConfigurationFactory ssl = new SslConfigurationFactory();
        ssl.setKeystoreFile(keystorePath.toFile());
        ssl.setKeystorePassword("password");
        // set the SSL configuration for the listener
        secureFactory.setSslConfiguration(ssl.createSslConfiguration());
        secureFactory.setImplicitSsl(true);

        // Creating a default and secure listener
        serverFactory.addListener("default", defaultFactory.createListener());
        serverFactory.addListener("secure", secureFactory.createListener());
        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();

        // Setting user properties file to userManagerFactory
        userManagerFactory.setFile(userManagerFilePath.toFile());
        // Creating userManger using above settings
        userManager = userManagerFactory.createUserManager();
        serverFactory.setUserManager(userManager);
        server = serverFactory.createServer();
    }

    private void createRequiredFiles() {
        // Creating FTP home directory if it does not exist
        // Home directory is created inside default user directory
        // In windows it corresponds to "C:\Users\{Your UserName}\"
        rootDirPath = Paths.get(System.getProperty("user.home") + "/ftpHome");
        try {
            if (Files.notExists(rootDirPath)) {
                Files.createDirectories(rootDirPath);
            }
        } catch (IOException exception) {
            exception.printStackTrace(System.err);
        }

        // Copy keystore file server.jks from inside jar to user home directory
        keystorePath = Paths.get(rootDirPath.toString() + "/server.jks");
        try {
            Files.copy(getClass().getResourceAsStream("server.jks"), keystorePath);
        } catch (IOException e) {
            e.printStackTrace(System.err);
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
}
