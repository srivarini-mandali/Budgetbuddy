package Secondjavafx;


import java.time.LocalDate;

class TransactionNode {
    LocalDate date;
    double amount;
    String category;
    TransactionNode left, right;

    public TransactionNode(LocalDate date, double amount, String category) {
        this.date = date;
        this.amount = amount;
        this.category = category;
        this.left = this.right = null;
    }
}

public class TransactionBST {
    private TransactionNode root;

    public void insert(LocalDate date, double amount, String category) {
        root = insertRec(root, date, amount, category);
    }

    private TransactionNode insertRec(TransactionNode root, LocalDate date, double amount, String category) {
        if (root == null) {
            root = new TransactionNode(date, amount, category);
            return root;
        }
        if (date.isBefore(root.date)) {
            root.left = insertRec(root.left, date, amount, category);
        } else if (date.isAfter(root.date)) {
            root.right = insertRec(root.right, date, amount, category);
        }
        return root;
    }

    public TransactionNode search(LocalDate date) {
        return searchRec(root, date);
    }

    private TransactionNode searchRec(TransactionNode root, LocalDate date) {
        if (root == null || root.date.equals(date)) {
            return root;
        }
        if (date.isBefore(root.date)) {
            return searchRec(root.left, date);
        } else {
            return searchRec(root.right, date);
        }
    }
}
