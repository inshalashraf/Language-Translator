package com.example.offlinetranslator.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HistoryDao {
    private final Db db;
    public record Row(long id, String createdAt, String src, String tgt, String input, String output) {}

    public HistoryDao(Db db) { this.db = db; }

    public void insert(String src, String tgt, String input, String output) {
        try (var conn = db.get();
             var ps = conn.prepareStatement(
                     "INSERT INTO history(source_lang,target_lang,input_text,output_text) VALUES(?,?,?,?)")) {
            ps.setString(1, src);
            ps.setString(2, tgt);
            ps.setString(3, input);
            ps.setString(4, output);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Row> lastN(int n) {
        try (var conn = db.get();
             var ps = conn.prepareStatement(
                     "SELECT id,created_at,source_lang,target_lang,input_text,output_text FROM history ORDER BY id DESC LIMIT ?")) {
            ps.setInt(1, n);
            try (var rs = ps.executeQuery()) {
                var out = new ArrayList<Row>();
                while (rs.next()) {
                    out.add(new Row(
                            rs.getLong(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getString(5), rs.getString(6)
                    ));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
