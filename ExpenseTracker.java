import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;

class InvalidAmountException extends Exception {
    InvalidAmountException(String message) {
        super(message);
    }
}

interface Reportable {
    String getDetails();
}

abstract class Expense implements Reportable {
    protected String title;
    protected double amount;

    Expense(String title, double amount) {      
        this.title = title;                      
        this.amount = amount;
    }

    abstract String getCategory();               

    final double getAmount() {                   
        return amount;
    }

    public String getDetails() {
        return getCategory() + " | " + title + " | Rs." + amount;
    }
}

class FoodExpense extends Expense {
    FoodExpense(String t, double a) { super(t, a); }   
    String getCategory() { return "Food"; }            
}

class TravelExpense extends Expense {
    TravelExpense(String t, double a) { super(t, a); }
    String getCategory() { return "Travel"; }
}

class OtherExpense extends Expense {
    OtherExpense(String t, double a) { super(t, a); }
    String getCategory() { return "Other"; }
}


class ExpenseManager {
    private ArrayList<Expense> list = new ArrayList<Expense>();
    private final String FILE_NAME = "expenses.txt";

    void add(Expense e) {
        list.add(e);
    }

    void add(String title, String amountText, String category) throws InvalidAmountException {
        double amt;
        try {
            amt = Double.parseDouble(amountText.trim());
        } catch (NumberFormatException ex) {
            throw new InvalidAmountException("Amount must be a number!");
        }
        if (amt <= 0) {
            throw new InvalidAmountException("Amount must be greater than 0!");
        }
        if (title.trim().length() == 0) {
            throw new InvalidAmountException("Title cannot be empty!");
        }
        title = title.replace(",", " ");

        Expense e;
        if (category.equals("Food")) {
            e = new FoodExpense(title, amt);
        } else if (category.equals("Travel")) {
            e = new TravelExpense(title, amt);
        } else {
            e = new OtherExpense(title, amt);
        }
        add(e);
    }

    double total() {
        double sum = 0;
        for (Expense e : list) {
            sum += e.getAmount();
        }
        return Math.round(sum * 100) / 100.0;           
    }

    double highest() {
        double max = 0;
        for (Expense e : list) {
            max = Math.max(max, e.getAmount());
        }
        return max;
    }

    double categoryTotal(String category) {
        double sum = 0;
        for (Expense e : list) {
            if (e.getCategory().equals(category)) {
                sum += e.getAmount();
            }
        }
        return Math.round(sum * 100) / 100.0;
    }

    void deleteLast() {
        if (list.size() > 0) {
            list.remove(list.size() - 1);
        }
    }

    ArrayList<Expense> getAll() { return list; }

    void clear() { list.clear(); }

    synchronized void save() throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(FILE_NAME));
        for (Expense e : list) {
            pw.println(e.getCategory() + "," + e.title + "," + e.amount);
        }
        pw.close();
    }

    void load() {
        File f = new File(FILE_NAME);                    
        if (!f.exists()) return;
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");            
                add(p[1], p[2], p[0]);
            }
            br.close();
        } catch (Exception ex) {
            System.out.println("Could not load file: " + ex.getMessage());
        }
    }
}


class SaveThread extends Thread {
    private ExpenseManager manager;
    private JLabel status;

    SaveThread(ExpenseManager m, JLabel s) {
        manager = m;
        status = s;
    }

    public void run() {
        try {
            status.setText("Saving...");
            Thread.sleep(700);                           
            manager.save();
            status.setText("Saved to expenses.txt");
        } catch (Exception e) {
            status.setText("Save failed!");
        }
    }
}

public class ExpenseTracker extends JFrame {
    ExpenseManager manager = new ExpenseManager();

    Color DARK  = new Color(44, 62, 80);
    Color LIGHT = new Color(236, 240, 241);
    Color GREEN = new Color(39, 174, 96);
    Color BLUE  = new Color(41, 128, 185);
    Color RED   = new Color(192, 57, 43);

    JTextField txtTitle = new JTextField();
    JTextField txtAmount = new JTextField();
    JComboBox<String> cmbCategory = new JComboBox<String>(new String[]{"Food", "Travel", "Other"});

    JTextArea area = new JTextArea();
    JLabel lblTotal = new JLabel();
    JLabel lblFood = new JLabel();
    JLabel lblTravel = new JLabel();
    JLabel lblOther = new JLabel();
    JLabel lblStatus = new JLabel("Ready");

    JButton btnAdd = makeButton("Add", GREEN);
    JButton btnSave = makeButton("Save", BLUE);
    JButton btnDelete = makeButton("Delete Last", RED);
    JButton btnClear = makeButton("Clear All", DARK);

    ExpenseTracker() {
        setTitle("Expense Tracker");
        setSize(480, 600);
        setLocationRelativeTo(null);                     
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout()); 
        JLabel header = new JLabel("EXPENSE TRACKER", JLabel.CENTER);
        header.setFont(new Font("Arial", Font.BOLD, 24));
        header.setForeground(Color.WHITE);
        header.setOpaque(true);
        header.setBackground(DARK);
        header.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.setBackground(LIGHT);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Add New Expense"),
                BorderFactory.createEmptyBorder(5, 10, 10, 10)));
        form.add(new JLabel("Title:"));
        form.add(txtTitle);
        form.add(new JLabel("Amount (Rs.):"));
        form.add(txtAmount);
        form.add(new JLabel("Category:"));
        form.add(cmbCategory);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createTitledBorder("Expense List"));
        JPanel center = new JPanel(new BorderLayout(0, 5));
        center.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        center.add(form, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        JPanel summary = new JPanel(new GridLayout(2, 2, 5, 5));
        summary.setBorder(BorderFactory.createTitledBorder("Summary (click Total for highest)"));
        summary.add(lblTotal);
        summary.add(lblFood);
        summary.add(lblTravel);
        summary.add(lblOther);
        lblTotal.setFont(new Font("Arial", Font.BOLD, 14));
        lblTotal.setForeground(RED);
        lblTotal.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        buttons.add(btnAdd);
        buttons.add(btnSave);
        buttons.add(btnDelete);
        buttons.add(btnClear);
        lblStatus.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        JPanel south = new JPanel(new BorderLayout());
        south.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        south.add(summary, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.CENTER);
        south.add(lblStatus, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        btnAdd.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addExpense();
            }
        });
        btnSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new SaveThread(manager, lblStatus).start();
            }
        });
        btnDelete.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                manager.deleteLast();
                refresh();
            }
        });
        btnClear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                manager.clear();
                refresh();
            }
        });
        txtAmount.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    addExpense();
                }
            }
        });
        lblTotal.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(null, "Highest expense: Rs." + manager.highest());
            }
        });

        manager.load();
        refresh();
        setVisible(true);
    }
    JButton makeButton(String text, Color color) {
        JButton b = new JButton(text);
        b.setBackground(color);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Arial", Font.BOLD, 12));
        b.setFocusPainted(false);
        return b;
    }
    void addExpense() {
        try {                                           
            manager.add(txtTitle.getText(), txtAmount.getText(), (String) cmbCategory.getSelectedItem());
            txtTitle.setText("");
            txtAmount.setText("");
            lblStatus.setText("Expense added");
            refresh();
        } catch (InvalidAmountException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            txtTitle.requestFocus();
        }
    }
    void refresh() {
        StringBuffer sb = new StringBuffer();           
        int no = 1;
        for (Expense e : manager.getAll()) {
            sb.append(no++).append(". ").append(e.getDetails()).append("\n");
        }
        area.setText(sb.toString());
        lblTotal.setText("Total: Rs." + manager.total());
        lblFood.setText("Food: Rs." + manager.categoryTotal("Food"));
        lblTravel.setText("Travel: Rs." + manager.categoryTotal("Travel"));
        lblOther.setText("Other: Rs." + manager.categoryTotal("Other"));
    }
    public static void main(String[] args) {
        try {   // cross-platform look so button colors show on every OS
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Using default look");
        }
        new ExpenseTracker();
    }
}