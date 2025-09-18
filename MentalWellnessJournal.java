// ===== Main Class =====
 public class MentalWellnessJournal{
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter your username: ");
        User user = new User(sc.nextLine());
        String filename = user.getUsername() + "_journal.txt";
        JournalManager manager = new JournalManager();
        manager.loadFromFile(filename);
        int choice;
        do {
            System.out.println("\n--- Mental Wellness Journal ---");
            System.out.println("1. Add entry");
            System.out.println("2. View all entries");
            System.out.println("3. Sort by date");
            System.out.println("4. Sort by mood");
            System.out.println("5. Search by keyword");
            System.out.println("6. Mood statistics");
            System.out.println("7. Generate report");
            System.out.println("8. Save and exit");
            System.out.print("Choose an option: ");
            choice = Integer.parseInt(sc.nextLine());
            switch (choice) {
                case 1:
                    System.out.print("Mood: ");
                    String mood = sc.nextLine();
                    System.out.print("Note: ");
                    String note = sc.nextLine();
                    manager.addEntry(new JournalEntry(new Date(), mood, note));
                    break;
                case 2:
                    manager.viewAll();
                    break;
                case 3:
                    manager.sortByDate();
                    System.out.println("Sorted by date:");
                    manager.viewAll();
                    break;
                case 4:
                    manager.sortByMood();
                    System.out.println("Sorted by mood:");
                    manager.viewAll();
                    break;
                case 5:
                    System.out.print("Enter keyword: ");
                    String keyword = sc.nextLine();
                    manager.searchByKeyword(keyword);
                    break;
                case 6:
                    manager.generateMoodStatistics();
                    break;
                case 7:
                    manager.generateReport();
                    break;
                case 8:
                    manager.saveToFile(filename);
                    System.out.println("Entries saved. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        } while (choice != 8);
        sc.close();
    }
 }