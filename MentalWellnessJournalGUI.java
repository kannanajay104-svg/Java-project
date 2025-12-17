import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.table.*;

// ================= ENUM for Moods =================
enum Mood {
    HAPPY, SAD, ANGRY, TIRED, STRESSED, HUNGRY, CALM, CONFUSED, NEUTRAL;

    @Override
    public String toString() {
        return name().substring(0,1)+name().substring(1).toLowerCase();
    }
}

// ================= Encryption Utility =================
class SimpleCipher {
    private static final int KEY=42;
    public static String encrypt(String data){
        StringBuilder sb=new StringBuilder();
        for(char c:data.toCharArray()) sb.append((char)(c^KEY));
        return sb.toString();
    }
    public static String decrypt(String data){ return encrypt(data); }
}

// ================= Journal Entry =================
class JournalEntry {
    private Date startTime;  // suggestion time
    private Date endTime;    // feedback end time
    private Mood mood;
    private String note;     // feedback
    private List<String> tags;
    private long durationMinutes;

    public JournalEntry(Date startTime, Date endTime, Mood mood, String note, List<String> tags){
        this.startTime=startTime; this.endTime=endTime; this.mood=mood; this.note=note; this.tags=tags;
        calculateDuration();
    }

    public void setFeedback(String note, Date endTime){
        this.note=note; this.endTime=endTime; calculateDuration();
    }

    private void calculateDuration(){
        if(startTime!=null && endTime!=null)
            this.durationMinutes=(endTime.getTime()-startTime.getTime())/(1000*60);
        else this.durationMinutes=0;
    }

    public Date getStartTime(){ return startTime; }
    public Date getEndTime(){ return endTime; }
    public Mood getMood(){ return mood; }
    public String getNote(){ return note; }
    public long getDurationMinutes(){ return durationMinutes; }
    public List<String> getTags(){ return tags; }
    public boolean needsFeedback(){ return note==null || note.isEmpty(); }

    public String toString(int index){
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String startStr=startTime!=null?sdf.format(startTime):"N/A";
        String endStr=endTime!=null?sdf.format(endTime):"Pending";
        String tagsStr=tags.isEmpty()?"":" Tags: "+String.join(", ",tags);
        String durationStr=MentalWellnessJournalGUI.getDurationString(durationMinutes*60*1000);
        String feedbackStr=note!=null?note:"❗ Feedback pending";
        return String.format("\n--- Entry %d ---\n[Start: %s]\nEnd: %s\nMood: %s%s\nDuration: %s\nFeedback: %s\n--------------",
                index,startStr,endStr,mood.toString(),tagsStr,durationStr,feedbackStr);
    }

    public String serialize(){
        String tagsStr=String.join(",",tags);
        String noteStr=note!=null?note.replace(";",","):"";
        long end=(endTime!=null)?endTime.getTime():0;
        return startTime.getTime()+";"+end+";"+mood.name()+";"+noteStr+";"+tagsStr;
    }

    public static JournalEntry deserialize(String line){
        String[] parts=SimpleCipher.decrypt(line).split(";",5);
        if(parts.length<5) return null;
        Date start=new Date(Long.parseLong(parts[0]));
        Date end=(Long.parseLong(parts[1])==0)?null:new Date(Long.parseLong(parts[1]));
        Mood mood=Mood.valueOf(parts[2]);
        String note=parts[3].isEmpty()?null:parts[3];
        List<String> tags=parts[4].isEmpty()?new ArrayList<>():Arrays.asList(parts[4].split(","));
        return new JournalEntry(start,end,mood,note,tags);
    }
}

// ================= Journal Manager =================
class JournalManager {
    private List<JournalEntry> entries=new ArrayList<>();

    public void addEntry(JournalEntry entry){ entries.add(entry); }
    public List<JournalEntry> getEntries(){ return entries; }
    public List<JournalEntry> getPendingFeedbacks(){
        List<JournalEntry> pending=new ArrayList<>();
        for(JournalEntry e:entries) if(e.needsFeedback()) pending.add(e);
        return pending;
    }

    public void deleteEntry(int idx){ if(idx>=0 && idx<entries.size()) entries.remove(idx); }

    public void saveToFile(String filename) throws IOException{
        try(BufferedWriter bw=new BufferedWriter(new FileWriter(filename))){
            for(JournalEntry e:entries) bw.write(SimpleCipher.encrypt(e.serialize())+"\n");
        }
    }

    public void loadFromFile(String filename){
        File f=new File(filename);
        if(!f.exists()) return;
        try(BufferedReader br=new BufferedReader(new FileReader(f))){
            String line;
            while((line=br.readLine())!=null){
                if(line.trim().isEmpty()) continue;
                JournalEntry e=JournalEntry.deserialize(line);
                if(e!=null) entries.add(e);
            }
        }catch(Exception e){ entries.clear(); }
    }
}

// ================= Mood Analyzer =================
class MoodAnalyzer {
    public static Mood analyzeMood(String text){
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
    public static String getSuggestion(Mood mood){
        switch(mood){
            case HAPPY:return "Share your joy with others.";
            case SAD:return "Talk to someone you trust.";
            case ANGRY:return "Take deep breaths and calm yourself.";
            case TIRED:return "Rest or take a nap.";
            case STRESSED:return "Pause and focus on one task.";
            case HUNGRY:return "Eat something healthy.";
            case CALM:return "Enjoy the stillness.";
            case CONFUSED:return "Break problem into smaller parts.";
            default:return "Stay mindful and keep expressing your thoughts.";
        }
    }
}

// ================= User =================
class User {
    private String username;
    private String password;
    public User(String username){ this.username=username.trim().isEmpty()?"default_user":username.trim(); }
    public void setPassword(String pw){ this.password=pw; }
    public boolean authenticate(String pw){ return password!=null && password.equals(pw); }
    public String getUsername(){ return username; }
}

// ================= Main GUI =================
public class MentalWellnessJournalGUI {
    private static final String USER_FILE="users.txt";
    private JFrame frame;
    private JTable table;
    private DefaultTableModel tableModel;
    private JournalManager manager;
    private User user;
    private String journalFile;

    public static void main(String[] args){ SwingUtilities.invokeLater(()->new MentalWellnessJournalGUI().start()); }

    private void start(){
        frame=new JFrame("Mental Wellness Journal"); frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800,600);
        loginUsernameDialog();
    }

    // ================= Two-step login =================
    private void loginUsernameDialog(){
        String username=JOptionPane.showInputDialog(frame,"Enter username:");
        if(username==null) System.exit(0);
        user=new User(username.trim());
        Map<String,String> map=new HashMap<>();
        File f=new File(USER_FILE);
        if(f.exists()){
            try(BufferedReader br=new BufferedReader(new FileReader(f))){
                String line; while((line=br.readLine())!=null){
                    String[] arr=line.split(":"); if(arr.length==2) map.put(arr[0],arr[1]);
                }
            }catch(Exception e){}
        }
        if(map.containsKey(username)){
            loginPasswordDialog(SimpleCipher.decrypt(map.get(username)));
        }else{
            String pw=JOptionPane.showInputDialog(frame,"New user! Set password:");
            if(pw==null) System.exit(0);
            user.setPassword(pw);
            try(BufferedWriter bw=new BufferedWriter(new FileWriter(f,true))){
                bw.write(username+":"+SimpleCipher.encrypt(pw)); bw.newLine();
            }catch(Exception e){}
            JOptionPane.showMessageDialog(frame,"User created.");
            loginSuccess();
        }
    }

    private void loginPasswordDialog(String correctPw){
        int attempts=3;
        while(attempts>0){
            String pw=JOptionPane.showInputDialog(frame,"Enter password (Attempts left "+attempts+"):"); 
            if(pw==null) System.exit(0);
            if(pw.equals(correctPw)){
                user.setPassword(pw); loginSuccess(); return;
            }else attempts--;
        }
        JOptionPane.showMessageDialog(frame,"Too many incorrect attempts. Exiting."); System.exit(0);
    }

    private void loginSuccess(){
        journalFile=user.getUsername()+"_journal.txt";
        manager=new JournalManager(); manager.loadFromFile(journalFile);
        handlePendingFeedbacks();
        showMainMenu();
    }

    // ================= Pending Feedback Handling =================
    private void handlePendingFeedbacks(){
        List<JournalEntry> pending=manager.getPendingFeedbacks();
        if(pending.isEmpty()) return;
        for(JournalEntry e:pending){
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm");
            JOptionPane.showMessageDialog(frame,"You have pending feedback for suggestion: "+MoodAnalyzer.getSuggestion(e.getMood()));
            String fb;
            while(true){
                fb=JOptionPane.showInputDialog(frame,"Please provide feedback for the suggestion:");
                if(fb!=null && !fb.trim().isEmpty()) break;
            }
            Date endTime=null;
            while(endTime==null){
                String s=JOptionPane.showInputDialog(frame,"Enter end time of mood (yyyy-MM-dd HH:mm):");
                if(s==null) continue;
                try{ endTime=new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(s.trim()); }catch(Exception ex){ endTime=null; }
            }
            e.setFeedback(fb,endTime);
        }
    }

    // ================= Main Menu =================
    private void showMainMenu(){
        String[] options={"Add Entry","View Entries","Mood Statistics","Filter by Mood","Full Report","Delete Entry","Save & Logout"};
        while(true){
            int choice=JOptionPane.showOptionDialog(frame,"Choose an option","Main Menu",
                    JOptionPane.DEFAULT_OPTION,JOptionPane.INFORMATION_MESSAGE,null,options,options[0]);
            if(choice==-1) System.exit(0);
            switch(choice){
                case 0: addEntryDialog(); break;
                case 1: viewEntriesDialog(); break;
                case 2: moodStatsDialog(); break;
                case 3: filterMoodDialog(); break;
                case 4: generateReportDialog(); break;
                case 5: deleteEntryDialog(); break;
                case 6: saveAndLogout(); return;
            }
        }
    }

    // ================= Add Entry =================
    private void addEntryDialog(){
        String feel=JOptionPane.showInputDialog(frame,"How do you feel right now?");
        if(feel==null) return;
        Date startTime=new Date();
        Mood moodDetected=MoodAnalyzer.analyzeMood(feel);
        String suggestion=MoodAnalyzer.getSuggestion(moodDetected);
        JOptionPane.showMessageDialog(frame,"Detected Mood: "+moodDetected+"\nSuggestion: "+suggestion);
        String tagsText=JOptionPane.showInputDialog(frame,"Add optional tags (space separated):");
        List<String> tags=new ArrayList<>();
        if(tagsText!=null) tags=Arrays.stream(tagsText.split("\\s+")).filter(t->!t.isEmpty()).collect(Collectors.toList());
        JournalEntry newEntry=new JournalEntry(startTime,null,moodDetected,null,tags);
        manager.addEntry(newEntry);

        // ================= Feedback for suggestion =================
        long diff2Hours=TimeUnit.HOURS.toMillis(2);
        long start=startTime.getTime();
        long now=System.currentTimeMillis();
        if(now-start>diff2Hours){
            JOptionPane.showMessageDialog(frame,"2 hours passed. Feedback missed. Auto-saving and logout.");
            try{ manager.saveToFile(journalFile);}catch(Exception e){}
            System.exit(0);
        }

        String fb;
        while(true){
            fb=JOptionPane.showInputDialog(frame,"After suggestion, how do you feel now? (Feedback must be given within 2 hours)");
            if(fb!=null && !fb.trim().isEmpty()) break;
        }
        Date endTime=new Date();
        newEntry.setFeedback(fb,endTime);
        JOptionPane.showMessageDialog(frame,"Entry saved. Duration: "+getDurationString(newEntry.getDurationMinutes()*60*1000));
    }

    // ================= View Entries =================
    private void viewEntriesDialog(){
        List<JournalEntry> entries=manager.getEntries();
        String[] cols={"Start","End","Mood","Feedback","Duration","Tags"};
        Object[][] data=new Object[entries.size()][cols.length];
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for(int i=0;i<entries.size();i++){
            JournalEntry e=entries.get(i);
            data[i][0]=sdf.format(e.getStartTime());
            data[i][1]=e.getEndTime()!=null?sdf.format(e.getEndTime()):"Pending";
            data[i][2]=e.getMood();
            data[i][3]=e.getNote()!=null?e.getNote():"Pending";
            data[i][4]=getDurationString(e.getDurationMinutes()*60*1000);
            data[i][5]=String.join(", ",e.getTags());
        }
        tableModel=new DefaultTableModel(data,cols){ public boolean isCellEditable(int r,int c){return false;}};
        table=new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){
                if(e.getClickCount()==2){
                    int r=table.getSelectedRow();
                    if(r<0) return;
                    JournalEntry entry=entries.get(r);
                    if(entry.needsFeedback()){
                        String fb;
                        while(true){
                            fb=JOptionPane.showInputDialog(frame,"Provide feedback for suggestion: "+MoodAnalyzer.getSuggestion(entry.getMood()));
                            if(fb!=null && !fb.trim().isEmpty()) break;
                        }
                        Date endTime=null;
                        while(endTime==null){
                            String s=JOptionPane.showInputDialog(frame,"Enter end time of mood (yyyy-MM-dd HH:mm):");
                            if(s==null) continue;
                            try{ endTime=new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(s.trim()); }catch(Exception ex){ endTime=null; }
                        }
                        entry.setFeedback(fb,endTime);
                        refreshTable();
                    }
                }
            }
        });
        JScrollPane sp=new JScrollPane(table);
        JOptionPane.showMessageDialog(frame,sp,"View Entries",JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshTable(){
        for(int i=0;i<tableModel.getRowCount();i++){
            JournalEntry e=manager.getEntries().get(i);
            tableModel.setValueAt(e.getEndTime()!=null?new SimpleDateFormat("yyyy-MM-dd HH:mm").format(e.getEndTime()):"Pending",i,1);
            tableModel.setValueAt(e.getNote()!=null?e.getNote():"Pending",i,3);
            tableModel.setValueAt(getDurationString(e.getDurationMinutes()*60*1000),i,4);
        }
    }

    // ================= Mood Statistics =================
    private void moodStatsDialog(){
        List<JournalEntry> entries=manager.getEntries();
        if(entries.isEmpty()){ JOptionPane.showMessageDialog(frame,"No entries to analyze."); return; }
        SimpleDateFormat dayFmt=new SimpleDateFormat("yyyy-MM-dd");
        Map<String,List<JournalEntry>> dayGroups=new TreeMap<>();
        for(JournalEntry e:entries){
            String day=dayFmt.format(e.getStartTime());
            dayGroups.computeIfAbsent(day,k->new ArrayList<>()).add(e);
        }
        StringBuilder sb=new StringBuilder();
        for(String day:dayGroups.keySet()){
            sb.append("Date: ").append(day).append("\n");
            Map<Mood,Integer> moodCount=new HashMap<>();
            for(JournalEntry e:dayGroups.get(day)) moodCount.put(e.getMood(),moodCount.getOrDefault(e.getMood(),0)+1);
            int max=0; Mood common=null;
            for(Mood m:moodCount.keySet()){
                int c=moodCount.get(m);
                double pct=(c*100.0)/dayGroups.get(day).size();
                sb.append(m).append(": ").append(c).append(" (").append(String.format("%.1f%%",pct)).append(")\n");
                if(c>max){ max=c; common=m; }
            }
            sb.append("Most common mood: ").append(common).append("\n\n");
        }
        // Overall
        Map<Mood,Integer> overall=new HashMap<>();
        for(JournalEntry e:entries) overall.put(e.getMood(),overall.getOrDefault(e.getMood(),0)+1);
        int totalOverall=overall.values().stream().mapToInt(i->i).sum();
        sb.append("--- Overall Mood Stats ---\n");
        int maxOverall=0; Mood overallCommon=null;
        for(Mood m:overall.keySet()){
            int c=overall.get(m); double pct=(c*100.0)/totalOverall;
            sb.append(m).append(": ").append(c).append(" (").append(String.format("%.1f%%",pct)).append(")\n");
            if(c>maxOverall){ maxOverall=c; overallCommon=m; }
        }
        sb.append("Overall most common mood: ").append(overallCommon);
        JTextArea ta=new JTextArea(sb.toString()); ta.setEditable(false);
        JScrollPane sp=new JScrollPane(ta);
        JOptionPane.showMessageDialog(frame,sp,"Mood Statistics",JOptionPane.INFORMATION_MESSAGE);
    }

    // ================= Filter Mood =================
    private void filterMoodDialog(){
        String[] moods=Arrays.stream(Mood.values()).map(Mood::toString).toArray(String[]::new);
        String selected=(String)JOptionPane.showInputDialog(frame,"Select mood:","Filter by Mood",
                JOptionPane.QUESTION_MESSAGE,null,moods,moods[0]);
        if(selected==null) return;
        Mood mood=Mood.valueOf(selected.toUpperCase());
        List<JournalEntry> filtered=manager.getEntries().stream().filter(e->e.getMood()==mood).collect(Collectors.toList());
        if(filtered.isEmpty()){ JOptionPane.showMessageDialog(frame,"No entries for mood "+mood); return; }
        String[] cols={"Start","End","Mood","Feedback","Duration","Tags"};
        Object[][] data=new Object[filtered.size()][cols.length];
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for(int i=0;i<filtered.size();i++){
            JournalEntry e=filtered.get(i);
            data[i][0]=sdf.format(e.getStartTime());
            data[i][1]=e.getEndTime()!=null?sdf.format(e.getEndTime()):"Pending";
            data[i][2]=e.getMood();
            data[i][3]=e.getNote()!=null?e.getNote():"Pending";
            data[i][4]=getDurationString(e.getDurationMinutes()*60*1000);
            data[i][5]=String.join(", ",e.getTags());
        }
        JTable t=new JTable(data,cols);
        JScrollPane sp=new JScrollPane(t);
        JOptionPane.showMessageDialog(frame,sp,"Filtered Entries",JOptionPane.INFORMATION_MESSAGE);
    }

    // ================= Full Report =================
    private void generateReportDialog(){
        viewEntriesDialog(); moodStatsDialog();
    }

    // ================= Delete Entry =================
    private void deleteEntryDialog(){
        List<JournalEntry> entries=manager.getEntries();
        if(entries.isEmpty()){ JOptionPane.showMessageDialog(frame,"No entries to delete."); return; }
        String[] options=new String[entries.size()];
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for(int i=0;i<entries.size();i++) options[i]=(i+1)+": "+entries.get(i).getMood()+" "+sdf.format(entries.get(i).getStartTime());
        String sel=(String)JOptionPane.showInputDialog(frame,"Select entry to delete:","Delete Entry",
                JOptionPane.QUESTION_MESSAGE,null,options,options[0]);
        if(sel==null) return;
        int idx=Integer.parseInt(sel.split(":")[0])-1;
        int conf=JOptionPane.showConfirmDialog(frame,"Confirm delete?");
        if(conf==JOptionPane.YES_OPTION) manager.deleteEntry(idx);
    }

    // ================= Save & Logout =================
    private void saveAndLogout(){
        try{ manager.saveToFile(journalFile);}catch(Exception e){}
        JOptionPane.showMessageDialog(frame,"Saved. Logging out.");
        frame.dispose(); start(); // restart login
    }

    // ================= Utility =================
    public static String getDurationString(long ms){
        long minutes=TimeUnit.MILLISECONDS.toMinutes(ms);
        if(minutes<1) return "few seconds";
        if(minutes<60) return minutes+" minutes";
        long hours=minutes/60; minutes%=60;
        return hours+" hours "+minutes+" minutes";
    }
}