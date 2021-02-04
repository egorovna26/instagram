package su.egorovna.instagram.controller;

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import io.datafx.controller.ViewController;
import io.datafx.controller.ViewNode;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.context.ActionHandler;
import io.datafx.controller.flow.context.FlowActionHandler;
import io.datafx.controller.util.VetoException;
import io.datafx.core.concurrent.ProcessChain;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import org.controlsfx.control.textfield.CustomTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.egorovna.instagram.live.InstagramApi;

import javax.annotation.PostConstruct;

@ViewController(value = "/fxml/broadcast.fxml", title = "Instagram Live")
public class BroadcastController extends Controller {

    private static final Logger LOG = LoggerFactory.getLogger(BroadcastController.class);

    @ActionHandler
    private FlowActionHandler handler;

    @ViewNode
    private StackPane root;

    @ViewNode
    private Circle pic;

    @ViewNode
    private Label fullName;

    @ViewNode
    private CustomTextField url;

    @ViewNode
    private CustomTextField key;

    @PostConstruct
    private void init() {
        pic.setFill(new ImagePattern(new Image(InstagramApi.getClient().getSelfProfile().getProfile_pic_url())));
        fullName.setText(InstagramApi.getClient().getSelfProfile().getFull_name());
        url.setText(InstagramApi.getBroadcastUrl());
        key.setText(InstagramApi.getBroadcastKey());
        addClipboardButton(url);
        addClipboardButton(key);
    }

    private void addClipboardButton(CustomTextField textField) {
        MaterialDesignIconView iconView = new MaterialDesignIconView(MaterialDesignIcon.CLIPBOARD_TEXT, "24.0");
        iconView.setCursor(Cursor.HAND);
        iconView.setTranslateX(-5);
        iconView.setOnMouseClicked(event -> {
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(textField.getText());
            Clipboard clipboard = Clipboard.getSystemClipboard();
            clipboard.setContent(clipboardContent);
            textField.selectAll();
            textField.requestFocus();
        });
        textField.setRight(iconView);
        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                textField.deselect();
            }
        });
    }

    @FXML
    void onCancel(ActionEvent event) {
        ProcessChain.create()
                .addRunnableInPlatformThread(this::lock)
                .addRunnableInPlatformThread(() -> startLoading(root, "Cancel broadcast"))
                .addSupplierInExecutor(() -> {
                    try {
                        InstagramApi.stopBroadcast();
                    } catch (Exception e) {
                        LOG.debug("Ошибка остановки трансляции", e);
                        return false;
                    }
                    return true;
                })
                .addConsumerInPlatformThread(aBoolean -> {
                    if (aBoolean) {
                        try {
                            handler.navigate(CreateController.class);
                        } catch (VetoException | FlowException e) {
                            LOG.error("Системная ошибка", e);
                            unlock();
                            stopLoading();
                        }
                    }
                })
                .run();
    }

    @FXML
    void onLive(ActionEvent event) {
        ProcessChain.create()
                .addRunnableInPlatformThread(this::lock)
                .addRunnableInPlatformThread(() -> startLoading(root, "Starting live stream"))
                .addSupplierInExecutor(() -> {
                    try {
                        InstagramApi.startBroadcast(true);
                    } catch (Exception e) {
                        LOG.debug("Ошибка старта трансляции", e);
                        return false;
                    }
                    return true;
                })
                .addConsumerInPlatformThread(aBoolean -> {
                    if (aBoolean) {
                        try {
                            handler.navigate(StreamController.class);
                        } catch (VetoException | FlowException e) {
                            LOG.error("Системная ошибка", e);
                            unlock();
                            stopLoading();
                        }
                    }
                })
                .run();
    }

    @Override
    protected void lock() {

    }

    @Override
    protected void unlock() {

    }
}
