package Secondjavafx;

import java.util.ArrayList;
import java.util.List;

public class CategoryNode {
    String name;
    double totalExpense;
    List<CategoryNode> subCategories;

    public CategoryNode(String name) {
        this.name = name;
        this.subCategories = new ArrayList<>();
        this.totalExpense = 0.0;
    }

    public void addSubCategory(CategoryNode subCategory) {
        subCategories.add(subCategory);
    }

    public double calculateTotalExpense() {
        double total = this.totalExpense; // This node's total
        for (CategoryNode subCategory : subCategories) {
            total += subCategory.calculateTotalExpense();
        }
        return total;
    }
}

