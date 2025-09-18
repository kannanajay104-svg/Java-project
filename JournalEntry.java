 import java.io.*;
 import java.text.SimpleDateFormat;
 import java.util.*;
// ===== Class: JournalEntry =====
 class JournalEntry {
    private Date date;
    private String mood;
    private String note;
    public JournalEntry(Date date, String mood, String note) {
        this.date = date;
        this.mood = mood;
        this.note = note;
    }
    public Date getDate() { return date; }
    public String getMood() { return mood; }
    public String getNote() { return note; }
    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return "[" + sdf.format(date) + "] Mood: " + mood + " | Note: " + note;
    }
 }