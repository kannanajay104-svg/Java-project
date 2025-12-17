import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// ================= ENUM for Moods =================
enum Mood {
    HAPPY, SAD, ANGRY, TIRED, STRESSED, HUNGRY, CALM, CONFUSED, NEUTRAL;

    @Override
    public String toString() {
        return name().substring(0, 1) + name().substring(1).toLowerCase();
    }
}

// ================= Encryption Utility =================
class SimpleCipher {
    private static final int KEY = 42;
    public static String encrypt(String data) {
        StringBuilder encrypted = new StringBuilder();
        for (char c : data.toCharArray()) encrypted.append((char) (c ^ KEY));
        return encrypted.toString();
    }
    public static String decrypt(String data) { return encrypt(data); }
}

// ================= Journal Entry =================
class JournalEntry {
    private Date startTime;
    private Date endTime;
    private Mood mood;
    private String note;
    private List<String> tags;
    private long durationMinutes;

    public JournalEntry(Date startTime, Date endTime, Mood mood, String note, List<String> tags) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.mood = mood;
        this.note = note;
        this.tags = tags;
        calculateDuration();
    }

    public void setFeedback(String note, Date endTime) {
        this.note = note;
        this.endTime = endTime;
        calculateDuration();
    }

    private void calculateDuration() {
        if (startTime != null && endTime != null)
            this.durationMinutes = (endTime.getTime() - startTime.getTime()) / (1000 * 60);
        else
            this.durationMinutes = 0;
    }

    public Date getStartTime() { return startTime; }
    public Date getEndTime() { return endTime; }
    public Mood getMood() { return mood; }
    public String getNote() { return note; }
    public long getDurationMinutes() { return durationMinutes; }
    public List<String> getTags() { return tags; }
    public boolean needsFeedback() { return note == null || note.isEmpty(); }

    public String toString(int index) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String startStr = (startTime != null) ? sdf.format(startTime) : "N/A";
        String tagsStr = tags.isEmpty() ? "" : " Tags: " + String.join(", ", tags);
        String durationStr = MentalWellnessJournal3.getDurationString(durationMinutes * 60 * 1000);
        String feedbackStr = (note != null) ? note : "❗ Feedback pending";

        return String.format("\n--- Entry %d ---\n", index)
             + "[Start: " + startStr + "]\n"
             + "Mood: " + mood.toString() + tagsStr + "\n"
             + "Duration: " + durationStr + "\n"
             + "Feedback: " + feedbackStr + "\n"
             + "--------------";
    }

    public String serialize() {
        String tagsStr = String.join(",", tags);
        String noteStr = (note != null) ? note.replace(";", ",") : "";
        long end = (endTime != null) ? endTime.getTime() : 0;
        return startTime.getTime() + ";" + end + ";" + mood.name() + ";" + noteStr + ";" + tagsStr;
    }

    public static JournalEntry deserialize(String line) {
        String[] parts = SimpleCipher.decrypt(line).split(";",5);
        if (parts.length < 5) return null;
        Date start = new Date(Long.parseLong(parts[0]));
        Date end = (Long.parseLong(parts[1]) == 0) ? null : new Date(Long.parseLong(parts[1]));
        Mood mood = Mood.valueOf(parts[2]);
        String note = parts[3].isEmpty() ? null : parts[3];
        List<String> tags = parts[4].isEmpty() ? new ArrayList<>() : Arrays.asList(parts[4].split(","));
        return new JournalEntry(start,end,mood,note,tags);
    }
}

// ================= Journal Manager =================
class JournalManager {
    private List<JournalEntry> entries = new ArrayList<>();

    public void addEntry(JournalEntry entry) { entries.add(entry); }
    public void viewAll() {
        if (entries.isEmpty()) System.out.println("No journal entries yet.");
        else for (int i = 0; i < entries.size(); i++) System.out.println(entries.get(i).toString(i+1));
    }

    public void deleteEntryByIndex(Scanner sc) {
        if (entries.isEmpty()) { System.out.println("No entries to delete."); return; }
        viewAll();
        System.out.print("\nEnter entry number to delete: ");
        try {
            int idx = Integer.parseInt(sc.nextLine()) - 1;
            if (idx >= 0 && idx < entries.size()) {
                System.out.print("Confirm delete? (Y/N): ");
                String conf = sc.nextLine().trim().toLowerCase();
                if (conf.equals("y")) { entries.remove(idx); System.out.println("Deleted."); }
                else System.out.println("Cancelled.");
            } else System.out.println("Invalid number.");
        } catch(Exception e){ System.out.println("Invalid input."); }
    }

    public void filterByMood(Mood mood) {
        System.out.println("\n--- Entries with Mood: " + mood + " ---");
        List<JournalEntry> filtered = new ArrayList<>();
        for (JournalEntry e : entries) if (e.getMood() == mood) filtered.add(e);
        if (filtered.isEmpty()) System.out.println("No entries found.");
        else for (int i=0;i<filtered.size();i++) System.out.println(filtered.get(i).toString(i+1));
    }

    public void generateMoodStatistics() {
        if (entries.isEmpty()) { System.out.println("No entries to analyze."); return; }
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, List<JournalEntry>> dayGroups = new TreeMap<>();
        for (JournalEntry e : entries) {
            String day = (e.getStartTime() != null) ? dayFormat.format(e.getStartTime()) : "Unknown";
            dayGroups.computeIfAbsent(day, k -> new ArrayList<>()).add(e);
        }
        System.out.println("\n--- Mood Stats Day by Day ---");
        for (String day : dayGroups.keySet()) {
            List<JournalEntry> dayEntries = dayGroups.get(day);
            Map<Mood, Integer> moodCount = new HashMap<>();
            for (JournalEntry e : dayEntries) moodCount.put(e.getMood(), moodCount.getOrDefault(e.getMood(),0)+1);
            Mood common=null; int max=0;
            System.out.println("Date: "+day);
            for (Mood m : moodCount.keySet()) {
                int count = moodCount.get(m);
                double percent = (count*100.0)/dayEntries.size();
                System.out.printf("%s: %d (%.1f%%)\n", m, count, percent);
                if(count>max){ max=count; common=m; }
            }
            System.out.println("Most common mood: "+common+"\n");
        }
        Map<Mood, Integer> overallMap = new HashMap<>();
        for(JournalEntry e:entries) overallMap.put(e.getMood(), overallMap.getOrDefault(e.getMood(),0)+1);
        Mood overallCommon=null; int overallMax=0;
        System.out.println("--- Overall Mood Stats ---");
        for(Mood m: overallMap.keySet()){
            int count=overallMap.get(m); double percent=(count*100.0)/entries.size();
            System.out.printf("%s: %d (%.1f%%)\n", m,count,percent);
            if(count>overallMax){ overallMax=count; overallCommon=m; }
        }
        System.out.println("Overall most common mood: "+overallCommon);
    }

    public void generateReport() { viewAll(); generateMoodStatistics(); }

    // FIXED: Always overwrite full details (proper saving)
    public void saveToFile(String filename) throws IOException {
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(filename,false))){
            for(JournalEntry e: entries) bw.write(SimpleCipher.encrypt(e.serialize())+"\n");
        }
    }

    public void loadFromFile(String filename) {
        File f = new File(filename);
        if(!f.exists()) return;
        try(BufferedReader br = new BufferedReader(new FileReader(f))){
            String line;
            while((line=br.readLine())!=null){
                if(line.trim().isEmpty()) continue;
                JournalEntry e = JournalEntry.deserialize(line);
                if(e!=null) entries.add(e);
            }
        } catch(Exception e){ System.out.println("Error loading journal."); entries.clear(); }
    }

    public List<JournalEntry> getPendingFeedbacks() {
        List<JournalEntry> pending = new ArrayList<>();
        for(JournalEntry e:entries) if(e.needsFeedback()) pending.add(e);
        return pending;
    }
}

// ================= Mood Analyzer =================
class MoodAnalyzer {
    public static Mood analyzeMood(String text) {
        text=text.toLowerCase();
        if(text.contains("happy")||text.contains("excited")||text.contains("joyful")) return Mood.HAPPY;
        if(text.contains("sad")||text.contains("lonely")||text.contains("upset")) return Mood.SAD;
        if(text.contains("angry")||text.contains("frustrated")||text.contains("mad")) return Mood.ANGRY;
        if(text.contains("tired")||text.contains("exhausted")||text.contains("sleepy")) return Mood.TIRED;
        if(text.contains("stressed")||text.contains("worried")||text.contains("anxious")) return Mood.STRESSED;
        if(text.contains("hungry")||text.contains("starving")) return Mood.HUNGRY;
        if(text.contains("calm")||text.contains("relaxed")) return Mood.CALM;
        if(text.contains("confused")||text.contains("uncertain")) return Mood.CONFUSED;
        return Mood.NEUTRAL;
    }
    public static String getSuggestion(Mood mood) {
        switch(mood){
            case HAPPY: return "Share your joy with others.";
            case SAD: return "Talk to someone you trust.";
            case ANGRY: return "Take deep breaths and calm yourself.";
            case TIRED: return "Rest or take a nap.";
            case STRESSED: return "Pause and focus on one task.";
            case HUNGRY: return "Eat something healthy.";
            case CALM: return "Enjoy the stillness.";
            case CONFUSED: return "Break problem into smaller parts.";
            default: return "Stay mindful and keep expressing your thoughts.";
        }
    }
}

// ================= User =================
class User {
    private String username;
    private String password;
    public User(String username){ this.username=username.trim().isEmpty()?"default_user":username.trim(); }
    public boolean authenticate(String pw){ return password!=null && password.equals(pw);}
    public void setPassword(String pw){ this.password=pw;}
    public String getPassword(){return password;}
    public String getUsername(){ return username;}
}

// ================= Main =================
public class MentalWellnessJournal3 {
    private static final String USER_FILE="jsample.txt";

    public static String getDurationString(long ms){
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        if(minutes<1) return "few seconds";
        if(minutes<60) return minutes+" minutes";
        long hours=minutes/60; minutes%=60;
        return hours+" hours "+minutes+" minutes";
    }

    // FIXED LOGIN BEHAVIOR
    private static User login(Scanner sc) throws IOException {
        System.out.print("Enter username: ");
        String username=sc.nextLine().trim();
        Map<String,String> map=new HashMap<>();
        File f = new File(USER_FILE);

        // Load all users
        if(f.exists()){
            try(BufferedReader br=new BufferedReader(new FileReader(f))){
                String line;
                while((line=br.readLine())!=null){
                    String[] arr=line.split(":");
                    if(arr.length==2) map.put(arr[0],arr[1]);
                }
            }
        }

        User user=new User(username);
        // Existing user
        if(map.containsKey(username)){
            String pw=SimpleCipher.decrypt(map.get(username));
            user.setPassword(pw);
            int attempts=3;
            while(attempts>0){
                System.out.print("Enter password: ");
                if(user.authenticate(sc.nextLine())) { 
                    System.out.println("Login successful.\n");
                    return user; 
                } else { 
                    attempts--; 
                    System.out.println("Incorrect password. Attempts left: "+attempts);
                }
            }
            System.out.println("Too many failed attempts. Exiting.");
            return null;
        } 
        // New user creation
        else {
            System.out.print("New user! Set password: ");
            String pw=sc.nextLine();
            user.setPassword(pw);
            try(BufferedWriter bw=new BufferedWriter(new FileWriter(f,true))){
                bw.write(username+":"+SimpleCipher.encrypt(pw)); bw.newLine();
            }
            System.out.println("User created successfully.\n");
        }
        return user;
    }

    public static void main(String[] args) throws Exception {
        Scanner sc=new Scanner(System.in);
        User user=login(sc);
        if(user==null){ sc.close(); return; }

        String journalFile=user.getUsername()+"_journal.txt";
        JournalManager manager=new JournalManager();
        manager.loadFromFile(journalFile);

        List<JournalEntry> pending = manager.getPendingFeedbacks();
        if(!pending.isEmpty()){
            System.out.println("\nYou have pending feedback entries!");
            for(JournalEntry e:pending){
                System.out.println(e.toString(0));
                System.out.print("Please provide feedback for this entry: ");
                String fb=sc.nextLine().trim();
                while(fb.isEmpty()){
                    System.out.print("Feedback cannot be empty: ");
                    fb=sc.nextLine().trim();
                }
                System.out.print("Enter end time (yyyy-MM-dd HH:mm): ");
                Date endTime=null;
                while(endTime==null){
                    try{
                        String s=sc.nextLine().trim();
                        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm");
                        endTime=sdf.parse(s);
                    } catch(Exception ex){ System.out.print("Invalid. Enter again: "); }
                }
                e.setFeedback(fb,endTime);
            }
        }

        int choice;
        do{
            System.out.println("\n--- Mental Wellness Journal ---");
            System.out.println("1. Add entry (Mood & Tags)");
            System.out.println("2. View all entries");
            System.out.println("3. Mood statistics (daily & overall)");
            System.out.println("4. Filter entries by mood");
            System.out.println("5. Generate full report");
            System.out.println("6. Delete specific entry");
            System.out.println("7. Save and exit");
            System.out.print("Choose an option: ");
            try{ choice=Integer.parseInt(sc.nextLine()); } catch(Exception e){ choice=-1; }

            switch(choice){
                case 1:
                    System.out.print("How do you feel right now? ");
                    String input = sc.nextLine();
                    Date startTime = new Date();
                    Mood moodDetected = MoodAnalyzer.analyzeMood(input);
                    String suggestion = MoodAnalyzer.getSuggestion(moodDetected);
                    System.out.println("Detected Mood: "+moodDetected);
                    System.out.println("Suggestion: "+suggestion);
                    System.out.print("Add optional tags (space separated): ");
                    List<String> tags = Arrays.stream(sc.nextLine().toLowerCase().split(" "))
                            .filter(t->!t.isEmpty()).collect(Collectors.toList());

                    JournalEntry newEntry = new JournalEntry(startTime,null,moodDetected,null,tags);
                    manager.addEntry(newEntry);

                    String fb;
                    while(true){
                        System.out.print("After suggestion, how do you feel now (feedback)? within 2 hours: ");
                        fb=sc.nextLine().trim();
                        if(!fb.isEmpty()) break;
                        System.out.println("Feedback required!");
                    }
                    Date endTimeFb=new Date();
                    newEntry.setFeedback(fb,endTimeFb);
                    System.out.println("Entry saved. Duration: "+getDurationString(newEntry.getDurationMinutes()*60*1000));
                    break;

                case 2: manager.viewAll(); break;
                case 3: manager.generateMoodStatistics(); break;
                case 4:
                    System.out.println("Available moods: "+Arrays.toString(Mood.values()));
                    System.out.print("Enter mood to filter: ");
                    try{ manager.filterByMood(Mood.valueOf(sc.nextLine().trim().toUpperCase())); }
                    catch(Exception e){ System.out.println("Invalid mood."); }
                    break;
                case 5: manager.generateReport(); break;
                case 6: manager.deleteEntryByIndex(sc); break;
                case 7: 
                    manager.saveToFile(journalFile); 
                    System.out.println("All data saved successfully. Goodbye!"); 
                    break;
                default: System.out.println("Invalid choice."); break;
            }
        } while(choice!=7);

        sc.close();
    }
}
