package com.example.offlinetranslator.ui;

import com.example.offlinetranslator.core.Language;
import com.example.offlinetranslator.core.TranslationRequest;
import com.example.offlinetranslator.core.TranslatorLLM;
import com.example.offlinetranslator.db.HistoryDao;

import javax.swing.*;
import java.awt.*;

public class TranslatorFrame extends JFrame {
    private final TranslatorLLM llm;
    private final HistoryDao historyDao;

    private final JComboBox<Language> srcCombo = new JComboBox<>(Language.values());
    private final JComboBox<Language> tgtCombo = new JComboBox<>(new Language[]{
            Language.EN, Language.HI, Language.AR, Language.UR, Language.FR, Language.ES, Language.DE
    });

    private final JTextArea input = new JTextArea(6, 60);
    private final JTextArea output = new JTextArea(6, 60);
    private final JTextArea history = new JTextArea(6, 60);

    public TranslatorFrame(TranslatorLLM llm, HistoryDao historyDao) {
        super("Offline Language Translator (Swing)");
        this.llm = llm;
        this.historyDao = historyDao;
        buildUi();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    private void buildUi() {
        var panel = new JPanel(new BorderLayout(8,8));
        var north = new JPanel(new FlowLayout(FlowLayout.LEFT));

        srcCombo.setSelectedItem(Language.AUTO);
        tgtCombo.setSelectedItem(Language.EN);

        var swapBtn = new JButton("↔");
        swapBtn.addActionListener(a -> {
            var s = (Language) srcCombo.getSelectedItem();
            var t = (Language) tgtCombo.getSelectedItem();
            if (s != null && t != null && s != Language.AUTO) {
                srcCombo.setSelectedItem(t);
                tgtCombo.setSelectedItem(s);
            }
        });

        var translateBtn = new JButton("Translate");
        translateBtn.addActionListener(a -> doTranslate());

        north.add(new JLabel("From:")); north.add(srcCombo);
        north.add(swapBtn);
        north.add(new JLabel("To:")); north.add(tgtCombo);
        north.add(translateBtn);

        input.setLineWrap(true); input.setWrapStyleWord(true);
        output.setLineWrap(true); output.setWrapStyleWord(true);
        output.setEditable(false);
        history.setEditable(false);
        history.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        var center = new JPanel(new GridLayout(2,1,8,8));
        center.add(wrap("Input", new JScrollPane(input)));
        center.add(wrap("Output", new JScrollPane(output)));

        var south = wrap("Recent History", new JScrollPane(history));

        panel.add(north, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);
        setContentPane(panel);

        refreshHistory();
    }

    private JPanel wrap(String title, JComponent c) {
        var p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private void doTranslate() {
        var src = (Language) srcCombo.getSelectedItem();
        var tgt = (Language) tgtCombo.getSelectedItem();
        var text = input.getText().trim();
        if (tgt == null || text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter text and select target language.");
            return;
        }
        var req = new TranslationRequest(src == null ? Language.AUTO : src, tgt, text);

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        output.setText("Translating offline...");
        new Thread(() -> {
            try {
                var res = llm.translate(req);
                SwingUtilities.invokeLater(() -> {
                    output.setText(res.translated());
                    historyDao.insert(res.detectedSourceLang(), tgt.name(), text, res.translated());
                    refreshHistory();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    output.setText("Error: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this, ex.toString(), "Translation Error", JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                SwingUtilities.invokeLater(() -> setCursor(Cursor.getDefaultCursor()));
            }
        }).start();
    }

    private void refreshHistory() {
        var rows = historyDao.lastN(10);
        var sb = new StringBuilder();
        for (var r : rows) {
            sb.append("[%s] %s → %s%n".formatted(r.createdAt(), r.src(), r.tgt()));
            sb.append("IN: ").append(trim(r.input())).append('\n');
            sb.append("OUT: ").append(trim(r.output())).append("\n\n");
        }
        history.setText(sb.toString());
        history.setCaretPosition(0);
    }

    private String trim(String s) {
        s = s.replaceAll("\\R+", " ").trim();
        return s.length() > 140 ? s.substring(0,137) + "..." : s;
    }
}
