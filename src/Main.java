import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("sample.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("FTP Server");
        primaryStage.setScene(new Scene(root, 510, 420));
        primaryStage.setResizable(false);
        primaryStage.show();
        final Controller controller = loader.getController();
        controller.setStage(primaryStage);
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                controller.StopServer(new ActionEvent());
            }
        });
    }


    public static void main(String[] args) {
        launch(args);
    }
}
