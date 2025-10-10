package com.example.offlinetranslator;

import java.io.InputStream;
import java.util.Properties;

import javax.swing.SwingUtilities;

import com.example.offlinetranslator.db.Db;
import com.example.offlinetranslator.db.HistoryDao;
import com.example.offlinetranslator.llm.Gpt4AllLLM;
import com.example.offlinetranslator.llm.OnlineTranslator;
import com.example.offlinetranslator.ui.TranslatorFrame;

public class Main {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");

        var props = new Properties();
        try (InputStream in = Main.class.getResourceAsStream("/app.properties")) {
            props.load(in);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load app.properties", e);
        }

        var db = new Db(props.getProperty("db.path", "jdbc:sqlite:translator.db"));
        db.init();
        var dao = new HistoryDao(db);

        String mode = props.getProperty("translator.mode", "online").trim().toLowerCase();
        try (var llm = switch (mode) {
            case "online" -> new OnlineTranslator(props);
            default -> new Gpt4AllLLM(props);
        }) {
            SwingUtilities.invokeLater(() -> {
                var frame = new TranslatorFrame(llm, dao);
                frame.setVisible(true);
            });
            // Keep main thread alive while UI is open (LLM closed on exit)
            // In this design, the try-with`-resources closes when the process exits.
            // If you need explicit shutdown hooks, add Runtime.getRuntime().addShutdownHook(...)
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
