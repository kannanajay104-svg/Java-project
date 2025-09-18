 import java.io.*;
 import java.text.SimpleDateFormat;
 import java.util.*; 
// ===== Class: JournalManager =====
 class JournalManager {
    private List<JournalEntry> entries = new ArrayList<>();
    // Add entry
    public void addEntry(JournalEntry entry) {
        entries.add(entry);
    }
    // View all entries
    public void viewAll() {
        if (entries.isEmpty()) {
            System.out.println("No journal entries yet.");
        } else {
            for (JournalEntry e : entries) {
                System.out.println(e);
            }
        }
    }
    // Sort by date
    public void sortByDate() {
        entries.sort(Comparator.comparing(JournalEntry::getDate));
    }
    // Sort by mood
    public void sortByMood() {
        entries.sort(Comparator.comparing(JournalEntry::getMood));
 }
    // Search by keyword
    public void searchByKeyword(String keyword) {
        boolean found = false;
        for (JournalEntry e : entries) {
            if (e.getNote().toLowerCase().contains(keyword.toLowerCase())) {
                System.out.println(e);
                found = true;
            }
        }
        if (!found) {
            System.out.println("No matching entries found.");
        }
    }
    // Mood Statistics
    public void generateMoodStatistics() {
        if (entries.isEmpty()) {
            System.out.println("No entries to analyze.");
            return;
        }
        Map<String, Integer> moodCount = new HashMap<>();
        for (JournalEntry e : entries) {
            moodCount.put(e.getMood(), moodCount.getOrDefault(e.getMood(), 0) + 1);
        }
        System.out.println("\n--- Mood Statistics ---");
        for (String mood : moodCount.keySet()) {
            System.out.println(mood + ": " + moodCount.get(mood));
        }
    }
    // Generate Report (entries + statistics)
    public void generateReport() {
        System.out.println("\n===== Journal Report =====");
        viewAll();
        generateMoodStatistics();
        System.out.println("==========================");
    }
    // Save to file
    public void saveToFile(String filename) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (JournalEntry e : entries) {
                writer.write(e.getDate().getTime() + ";" + e.getMood() + ";" + e.getNote());
                writer.newLine();
            }
        }
    }
    // Load from file
    public void loadFromFile(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                Date date = new Date(Long.parseLong(parts[0]));
                String mood = parts[1];
                String note = parts[2];
                entries.add(new JournalEntry(date, mood, note));
            }
        }
    }
 }