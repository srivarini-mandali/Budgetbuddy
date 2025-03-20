package Secondjavafx;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ExpenseTable {
    private final StringProperty first;
    private final StringProperty sec;
    private final StringProperty date;

    public ExpenseTable() {
        this.first = new SimpleStringProperty();
        this.sec = new SimpleStringProperty();
        this.date = new SimpleStringProperty();
    }

    public ExpenseTable(String first, String sec, String date) {
        this.first = new SimpleStringProperty(first);
        this.sec = new SimpleStringProperty(sec);
        this.date = new SimpleStringProperty(date);
    }

    public String getFirst() {
        return first.get();
    }

    public void setFirst(String first) {
        this.first.set(first);
    }

    public StringProperty firstProperty() {
        return first;
    }

    public String getSec() {
        return sec.get();
    }

    public void setSec(String sec) {
        this.sec.set(sec);
    }

    public StringProperty secProperty() {
        return sec;
    }

    public String getDate() {
        return date.get();
    }

    public void setDate(String date) {
        this.date.set(date);
    }

    public StringProperty dateProperty() {
        return date;
    }
}
