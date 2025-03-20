package Secondjavafx;
import javafx.scene.control.ButtonBar;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class ExpenseTracKer extends Application {
    private Stage primaryStage; // Class-level field to hold the primary stage

	private CategoryNode rootCategory;
	private TransactionBST transactionTree;
    private String loggedInUsername; // Store the logged in user's username globally

	public ExpenseTracKer() {
		rootCategory = new CategoryNode("All Expenses");
		transactionTree = new TransactionBST();
		setupCategories();
	}

	private void setupCategories() {
		// Setup your categories here, e.g., "Food", "Utilities"
		CategoryNode food = new CategoryNode("Food");
		rootCategory.addSubCategory(food);
		food.addSubCategory(new CategoryNode("Restaurants"));
		food.addSubCategory(new CategoryNode("Groceries"));
	}

	public static void main(String[] args) {
		launch(args);
	}

	// Data structures for managing notifications and transactions
	Queue<String> notificationQueue = new ArrayDeque<>();
	ArrayDeque<String> transactionUndoDeque = new ArrayDeque<>();
	PriorityQueue<Double> transactionPriorityQueue = new PriorityQueue<>((x, y) -> y.compareTo(x));
	HashMap<String, Double> categoryTotalsCache = new HashMap<>();

	TableView<ExpenseTable> tableview;
	TableView<ExpenseCategory> tableview1;
	private PieChart pieChart;
	String semaphore = "";
	private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";
	String database = "jdbc:mysql://localhost/test?useUnicode=true&serverTimezone=UTC";
	String username = "root";
	String password = "ramprasad9";

	public String Formate(double value) {
		DecimalFormat myformat = new DecimalFormat("$###,###.0##");
		return myformat.format(value);
	}

	public double calculateTotalForCategory(String category) {

		if (categoryTotalsCache.containsKey(category)) {
			return categoryTotalsCache.get(category);
		}

		double total = 0.0;
		try (Connection conn = DriverManager.getConnection(database, username, password)) {
			String sql = "SELECT SUM(trtrtr) AS sum FROM tdtb WHERE Category = ?";
			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
				pstmt.setString(1, category);
				ResultSet rs = pstmt.executeQuery();
				if (rs.next()) {
					total += rs.getDouble("sum");
				}
			}
			sql = "SELECT DISTINCT Category FROM tdtb WHERE ParentCategory = ?";
			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
				pstmt.setString(1, category);
				ResultSet rs = pstmt.executeQuery();
				while (rs.next()) {
					total += calculateTotalForCategory(rs.getString("Category"));
				}
			}
		} catch (SQLException e) {
			System.out.println("Database error: " + e.getMessage());
		}

		categoryTotalsCache.put(category, total);
		return total;
	}

	public ObservableList<PieChart.Data> getPie() {
		 ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
		    String sql = "SELECT DISTINCT Category FROM tdtb WHERE username = ?";
		    try (Connection conn = DriverManager.getConnection(database, username, password);
		         PreparedStatement stmt = conn.prepareStatement(sql)) {
		        stmt.setString(1, loggedInUsername);
		        ResultSet rs = stmt.executeQuery();
		        while (rs.next()) {
		            String category = rs.getString("Category");
		            double total = calculateTotalForCategory(category);
		            pieChartData.add(new PieChart.Data(category, total));
		        }
		    } catch (SQLException e) {
		        System.out.println("Database error: " + e.getMessage());
		    }
		    return pieChartData;
	}

	public ObservableList<ExpenseTable> getExpense() {
		ObservableList<ExpenseTable> expense = FXCollections.observableArrayList();
		try (Connection conn = DriverManager.getConnection(database, username, password);
				Statement stt = conn.createStatement();
		   PreparedStatement stmt = conn.prepareStatement("SELECT * FROM tdtb WHERE username = ? ORDER BY date DESC")) {
	        stmt.setString(1, loggedInUsername);
	        ResultSet res = stmt.executeQuery();
				
			while (res.next()) {
				expense.add(new ExpenseTable(res.getString("Category"), Formate(res.getDouble("trtrtr")),
						res.getDate("date").toString()));
			}
		} catch (SQLException e) {
			System.out.print("Do not connect to DB - Error:" + e);
		}
		return expense;
	}

	public ObservableList<ExpenseCategory> getCategory() {
		ObservableList<ExpenseCategory> category = FXCollections.observableArrayList();
		double total = 0;
		String sqlCategory = "SELECT DISTINCT Category FROM tdtb WHERE username = ?";
		String sqlSalary = "SELECT SUM(trtrtr) AS 'sum' FROM tdtb WHERE Category = 'Salary' AND username = ?";

		try (Connection conn = DriverManager.getConnection(database, username, password);
		         PreparedStatement stmtCategory = conn.prepareStatement(sqlCategory);
		         PreparedStatement stmtSalary = conn.prepareStatement(sqlSalary)) {
		        
		        // Set username for category query
		        stmtCategory.setString(1, loggedInUsername);
		        ResultSet res = stmtCategory.executeQuery();
		        while (res.next()) {
		            String cat = res.getString("Category");
		            if (!"Salary".equals(cat)) {
		                double price = calculateTotalForCategory(cat);
		                category.add(new ExpenseCategory(cat, Formate(price)));
		                total += price;
		            }
		        }

		        // Set username for salary query
		        stmtSalary.setString(1, loggedInUsername);
		        ResultSet res2 = stmtSalary.executeQuery();
		        if (res2.next()) {
		            double salary = res2.getDouble("sum");
		            category.add(new ExpenseCategory("Total Expense --------->", Formate(total)));
		            category.add(new ExpenseCategory("Balance", Formate(salary)));
		            category.add(new ExpenseCategory("Total Savings --------->", Formate(salary - total)));
		        }
		    } catch (SQLException e) {
		        System.out.print("Do not connect to DB - Error:" + e);
		    }

		    return category;
	}
	public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage; // Initialize the field
        primaryStage.setTitle("Budget Buddy  - Login");
        primaryStage.setScene(createLoginScene(primaryStage));
        primaryStage.show();
    }
	private Scene createLoginScene(Stage stage) {
	    // Create a BorderPane as the root layout
	    BorderPane rootPane = new BorderPane();

	    // Left side with the image
	    ImageView imageView = new ImageView(new Image("background1.gif"));
	    imageView.setFitHeight(550);
	    imageView.setFitWidth(400);
	    imageView.setStyle("-fx-background-size: cover;");
	    rootPane.setLeft(imageView);

	    // Right side for the login form
	    GridPane grid = new GridPane();
	    grid.setAlignment(Pos.TOP_CENTER);
	    grid.setHgap(10);
	    grid.setVgap(20);
	    grid.setPadding(new Insets(20, 50, 20, 20)); // Add padding

	    // Logo and label
	    ImageView logoView = new ImageView(new Image("logo.png"));
	    logoView.setFitHeight(100);
	    logoView.setFitWidth(100);

	    Label budgetBuddyLabel = new Label("BudgetBuddy");
	    budgetBuddyLabel.setFont(Font.font("Gabriola", FontWeight.BOLD, 44));
	    budgetBuddyLabel.setStyle("-fx-text-fill: black;");

	    // Use VBox to stack logo and label vertically
	    VBox logoAndLabel = new VBox(5); // 5px spacing
	    logoAndLabel.setAlignment(Pos.CENTER);
	    logoAndLabel.getChildren().addAll(logoView, budgetBuddyLabel);

	    grid.add(logoAndLabel, 0, 0, 2, 1);
	    GridPane.setHalignment(logoAndLabel, HPos.CENTER);
	    GridPane.setValignment(logoAndLabel, VPos.TOP);
	    GridPane.setMargin(logoAndLabel, new Insets(10, 0, 20, 0)); // Adjust top margin

	    // Username and Password fields with labels
	    Label usernameLabel = new Label("Username:");
	    usernameLabel.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
	    TextField usernameField = new TextField();
	    usernameField.setPrefWidth(200);

	    Label passwordLabel = new Label("Password:");
	    passwordLabel.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
	    PasswordField passwordField = new PasswordField();
	    passwordField.setPrefWidth(200);

	    grid.add(usernameLabel, 0, 1);
	    grid.add(usernameField, 1, 1);
	    grid.add(passwordLabel, 0, 2);
	    grid.add(passwordField, 1, 2);

	    // Buttons for login and registration
	    Button loginButton = new Button("Login");
	    loginButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
	    loginButton.setPrefWidth(200);
	    Button registerButton = new Button("Register");
	    registerButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
	    registerButton.setPrefWidth(200);

	    loginButton.setOnAction(event -> {
	        if (authenticate(usernameField.getText(), passwordField.getText())) {
	            loggedInUsername = usernameField.getText();
	            stage.setTitle("Budget Buddy");
	            setupMainScene(stage);
	        } else {
	            Alert alert = new Alert(Alert.AlertType.ERROR);
	            alert.setTitle("Authentication Failed");
	            alert.setHeaderText(null);
	            alert.setContentText("Incorrect username or password.");
	            alert.showAndWait();
	        }
	    });
	    registerButton.setOnAction(event -> openRegistrationForm(stage));
	    Label newUserLabel = new Label("New User? Register Here:");
	    newUserLabel.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
	    grid.add(newUserLabel, 1, 4, 2, 1); 
	    
	    grid.add(loginButton, 1, 3, 2, 1);
	    grid.add(registerButton, 1, 5, 2, 1);

	    GridPane.setMargin(loginButton, new Insets(10, 0, 0, 0));
	    GridPane.setMargin(registerButton, new Insets(10, 0, 0, 0));

	    // Place grid in the right half of the BorderPane
	    rootPane.setRight(grid);

	    return new Scene(rootPane, 800, 550); // Adjusted total scene width to accommodate both sides
	}


	private void openRegistrationForm(Stage stage) {
	    Stage registerStage = new Stage();
	    registerStage.setTitle("Register New Account");

	    // BorderPane to manage layout with background on the left
	    BorderPane rootPane = new BorderPane();

	    // Left side with the background image
	    ImageView imageView = new ImageView(new Image("register.png"));
	    imageView.setFitHeight(550);
	    imageView.setFitWidth(400); // Adjust the width as necessary
	    imageView.setStyle("-fx-background-size: cover;");
	    rootPane.setLeft(imageView);

	    // GridPane for the form on the right
	    GridPane grid = new GridPane();
	    grid.setAlignment(Pos.CENTER);
	    grid.setHgap(10);
	    grid.setVgap(10);
	    grid.setPadding(new Insets(20, 50, 20, 20));

	    // Logo and "Registration" label with a VBox for vertical arrangement
	    ImageView logoView = new ImageView(new Image("logo.png"));
	    logoView.setFitHeight(100);
	    logoView.setFitWidth(100);
	    Label registrationLabel = new Label("Welcome to Registration");
	    registrationLabel.setFont(Font.font("Gabriola", FontWeight.BOLD, 24));
	    registrationLabel.setStyle("-fx-text-fill: black;"); // Text color set to black
	    VBox logoLabelBox = new VBox(10);
	    logoLabelBox.setAlignment(Pos.CENTER);
	    logoLabelBox.getChildren().addAll(logoView, registrationLabel);
	    grid.add(logoLabelBox, 0, 0, 2, 1);
	    GridPane.setHalignment(logoLabelBox, HPos.CENTER);

	    // Username and Password fields with labels
	    Label usernameLabel = new Label("Username:");
	    usernameLabel.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
	    TextField newUserField = new TextField();
	    newUserField.setPrefWidth(200);
	    Label passwordLabel = new Label("Password:");
	    passwordLabel.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
	    PasswordField newPasswordField = new PasswordField();
	    newPasswordField.setPrefWidth(200);

	    Label confirmPasswordLabel = new Label("Confirm Password:");
	    confirmPasswordLabel.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
	    PasswordField confirmPasswordField = new PasswordField();
	    confirmPasswordField.setPrefWidth(200);

	    Button submitButton = new Button("Submit");
	    submitButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
	    submitButton.setPrefWidth(200);

	    Label errorLabel = new Label();
	    errorLabel.setTextFill(Color.RED);

	    submitButton.setOnAction(e -> {
	        if (!newPasswordField.getText().equals(confirmPasswordField.getText())) {
	            errorLabel.setText("Error: Passwords do not match!");
	        } else if (newUserField.getText().isEmpty() || newPasswordField.getText().isEmpty()) {
	            errorLabel.setText("Error: All fields must be filled!");
	        } else {
	            registerNewUser(newUserField.getText(), newPasswordField.getText(), stage, () -> {
	                stage.setScene(createLoginScene(stage));  // Navigate back to the login scene after successful registration
	                registerStage.close();  // Close the registration window
	            });
	        }
	    });

	    grid.add(usernameLabel, 0, 2);
	    grid.add(newUserField, 1, 2);
	    grid.add(passwordLabel, 0, 3);
	    grid.add(newPasswordField, 1, 3);
	    grid.add(confirmPasswordLabel, 0, 4);
	    grid.add(confirmPasswordField, 1, 4);
	    grid.add(submitButton, 1, 5);
	    grid.add(errorLabel, 1, 6);

	    rootPane.setCenter(grid);

	    // Setting the scene size identical to the login scene
	    Scene scene = new Scene(rootPane, 800, 550);
	    registerStage.setScene(scene);
	    registerStage.show();
	}


	private void registerNewUser(String username, String password, Stage stage, Runnable onSuccessfulRegistration) {
		String databaseUrl = "jdbc:mysql://localhost/test?useUnicode=true&serverTimezone=UTC";
	    String dbUsername = "root";
	    String dbPassword = "ramprasad9";

	    Connection conn = null;
	    PreparedStatement pstmt = null;

	    try {
	        conn = DriverManager.getConnection(databaseUrl, dbUsername, dbPassword);
	        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
	        pstmt = conn.prepareStatement(sql);
	        pstmt.setString(1, username);
	        pstmt.setString(2, password);  // Hash the password if implementing properly
	        int affectedRows = pstmt.executeUpdate();

	        if (affectedRows > 0) {
	            Platform.runLater(() -> {
	                Alert alert = new Alert(Alert.AlertType.INFORMATION);
	                alert.setTitle("Registration Successful");
	                alert.setHeaderText(null);
	                alert.setContentText("User registered successfully! You can now log in.");
	                alert.showAndWait();
	                onSuccessfulRegistration.run();  // Executes the provided Runnable on successful registration
	            });
	        } else {
	            Platform.runLater(() -> {
	                Alert alert = new Alert(Alert.AlertType.ERROR);
	                alert.setTitle("Registration Failed");
	                alert.setHeaderText(null);
	                alert.setContentText("No user was registered. Please check your data!");
	                alert.showAndWait();
	            });
	        }
	    } catch (SQLException e) {
	        Platform.runLater(() -> {
	            Alert alert = new Alert(Alert.AlertType.ERROR);
	            alert.setTitle("Database Error");
	            alert.setHeaderText(null);
	            alert.setContentText("Registration failed: " + e.getMessage());
	            alert.showAndWait();
	        });
	    } finally {
	        try {
	            if (pstmt != null) pstmt.close();
	            if (conn != null) conn.close();
	        } catch (SQLException ex) {
	            ex.printStackTrace();
	        }
	    }
	}


	// Checks if the username and password are correct
	private boolean authenticate(String username, String password) {
	    Connection conn = null;
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    String databaseUrl = "jdbc:mysql://localhost/test?useUnicode=true&serverTimezone=UTC";
	    String dbUsername = "root";
	    String dbPassword = "ramprasad9";

	    try {
	        conn = DriverManager.getConnection(databaseUrl, dbUsername, dbPassword);
	        String sql = "SELECT password FROM users WHERE username = ?";
	        pstmt = conn.prepareStatement(sql);
	        pstmt.setString(1, username);
	        rs = pstmt.executeQuery();

	        if (rs.next()) {
	            String storedPassword = rs.getString("password");
	            // If hashing was used during registration, use the same hash function to compare
	            return storedPassword.equals(password);
	        }
	    } catch (SQLException ex) {
	        ex.printStackTrace();
	        return false;
	    } finally {
	        try {
	            if (rs != null) rs.close();
	            if (pstmt != null) pstmt.close();
	            if (conn != null) conn.close();
	        } catch (SQLException ex) {
	            ex.printStackTrace();
	        }
	    }
	    return false;
	}


	public String hashPassword(String password) throws NoSuchAlgorithmException {
	    MessageDigest md = MessageDigest.getInstance("SHA-256");
	    byte[] hashedPassword = md.digest(password.getBytes());

	    StringBuilder sb = new StringBuilder();
	    for (byte b : hashedPassword) {
	        sb.append(String.format("%02x", b));
	    }
	    return sb.toString();
	}

    private void setupMainScene(Stage basestage) {
		Scene Basescene, ExpenseType, IncomeType, Entryscene;

        basestage.setTitle("Budget Buddy");
		basestage.setScene(createLoginScene(basestage));

        basestage.show();
        
		// TABPANE TABS
		Image[] Tabimage = { new Image("Spending.png"), new Image("Transaction.png"), new Image("Graph.png"),
				new Image("logout.png") };
		ImageView[] Tabview = { new ImageView(Tabimage[0]), new ImageView(Tabimage[1]), new ImageView(Tabimage[2]),
				new ImageView(Tabimage[3]) };

		for (int i = 0; i < 4; i++) {
			Tabview[i].setFitHeight(25);
			Tabview[i].setFitWidth(25);
		}

		TabPane tabpane = new TabPane();
	    tabpane.setTabMinWidth(187);
	    tabpane.setTabMaxWidth(187);
	    tabpane.setTabMinHeight(30);
		tabpane.getStyleClass().add("tab-pane");
	    tabpane.setStyle("-fx-background-color: #ffffff;");  // Set the background color to white

		Tab[] tab = { new Tab("Spending"), new Tab("Transaction"), new Tab("Graph"), new Tab("Logout") };
		for (int i = 0; i < 4; i++) {
			tab[i].setClosable(false);
			tab[i].setGraphic(Tabview[i]);
			tab[i].getStyleClass().add("tab-sett");
		    tab[3].setClosable(false);  // Important to prevent the tab from being closed

		}
		tabpane.getTabs().addAll(tab[0], tab[1], tab[2], tab[3]);
		tab[3].setOnSelectionChanged(event -> {
		        if (tab[3].isSelected()) {
		            setupLogoutTabContent(tab[3]);  // Setup content for logout tab when it is selected
		        }
		    });
		Insets RBinset = new Insets(22, 60, 20, 0);
		Insets LBinset = new Insets(22, 0, 20, 60);

		Button Expense = new Button("+ Expense");
		Expense.getStyleClass().add("Basescene-button");
	    Expense.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
		Expense.requestFocus();
		Button Income = new Button("+ Income");
		Income.getStyleClass().add("Basescene-button");
	    Income.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

		// TABLE VIEW 1
		tableview1 = new TableView<>();
		TableViewSelectionModel<ExpenseCategory> selectionmodel = tableview1.getSelectionModel();
		TableColumn<ExpenseCategory, String> column3 = new TableColumn<>();
		column3.setCellValueFactory(new PropertyValueFactory<>("category"));
		column3.setSortable(true); // Allow sorting
		column3.setMinWidth(200);
		column3.setText("Category"); // Setting title for the column
		column3.prefWidthProperty().bind(tableview1.widthProperty().multiply(0.5)); // 50% of table width


		TableColumn<ExpenseCategory, String> column4 = new TableColumn<>();
		column4.setCellValueFactory(new PropertyValueFactory<>("amount"));
		column4.setSortable(true); // Allow sorting
		column4.setMinWidth(200);
		column4.setText("Amount");
		column4.prefWidthProperty().bind(tableview1.widthProperty().multiply(0.5)); // 50% of table width

		
		tableview1.setItems(getCategory());
		tableview1.getColumns().addAll(column3, column4);
		tableview1.setMaxHeight(340);
		tableview1.setMaxWidth(790);
		tableview1.getStyleClass().add("table-vie");
		selectionmodel.clearSelection();
		VBox table_container1 = new VBox(tableview1);
		table_container1.getStylesheets().add("tableview1.css");
	    table_container1.setStyle("-fx-background-color: #ffffff;");

		BorderPane gridpane = new BorderPane();
		gridpane.setStyle("-fx-background-color: #ffffff");

		gridpane.setRight(Expense);
		BorderPane.setAlignment(Expense, Pos.BOTTOM_RIGHT);
		BorderPane.setMargin(Expense, RBinset);
		gridpane.setLeft(Income);
		BorderPane.setAlignment(Income, Pos.BOTTOM_LEFT);
		BorderPane.setMargin(Income, LBinset);

		VBox Spending_pane = new VBox();
		Spending_pane.getChildren().addAll(table_container1, gridpane);
		Spending_pane.setStyle("-fx-background-color: #ffffff;");

		Insets deleteset = new Insets(0, 0, 0, 335);
		Button Delete = new Button("Delete");
		Delete.getStyleClass().add("Basescene-button");
		PieChart Piechart = new PieChart();

		// TABLE VIEW
		tableview = new TableView<>();
		TableViewSelectionModel<ExpenseTable> selectionmodel1 = tableview.getSelectionModel();
		tableview.setPlaceholder(new Label("NO Transaction Done"));
		TableColumn<ExpenseTable, String> column1 = new TableColumn<>("Category");
		column1.setCellValueFactory(new PropertyValueFactory<>("first"));
		column1.setSortable(true); // Allow sorting
		column1.setMinWidth(130);
		column1.prefWidthProperty().bind(tableview.widthProperty().divide(3)); // 33.33% of table width

		TableColumn<ExpenseTable, String> column2 = new TableColumn<>("Amount");
		column2.setCellValueFactory(new PropertyValueFactory<>("sec"));
		column2.setSortable(true); // Allow sorting
		column2.setMinWidth(138);
		column2.prefWidthProperty().bind(tableview.widthProperty().divide(3)); // 33.33% of table width

		TableColumn<ExpenseTable, String> column5 = new TableColumn<>("Date");
		column5.setCellValueFactory(new PropertyValueFactory<>("date"));
		column5.setSortable(true); // Allow sorting
		column5.setMinWidth(130);
		column5.prefWidthProperty().bind(tableview.widthProperty().divide(3)); // 33.33% of table width


		tableview.setItems(getExpense());
		tableview.getColumns().addAll(column1, column5, column2);
		tableview.setMinHeight(350);
		tableview.getStyleClass().add("table-view");
		VBox table_container = new VBox();
		VBox.setMargin(Delete, deleteset);
		table_container.getChildren().addAll(tableview, Delete);

		table_container.getStylesheets().add("tableview.css");
		table_container.setStyle("-fx-background-color:#ffffff");

		Delete.setOnAction(e -> {
			ExpenseTable select = selectionmodel1.getSelectedItem();
			if (select != null) {
				String deleteamount = select.getSec().toString();
				double deleted = Double.parseDouble(deleteamount.replaceAll("[,$]*", ""));
				try {
					Connection conn = DriverManager.getConnection(database, username, password);
					Statement stt = conn.createStatement();
					String query = "DELETE FROM tdtb where date = '" + select.getDate() + "' AND" + " Category = '"
							+ select.getFirst() + "' AND" + " trtrtr = '" + deleted + "'";
					int o = stt.executeUpdate(query);
					System.out.println(o + "row deleted");
					conn.close();
					refreshTableViews();
					Piechart.setData(getPie());
				} catch (SQLException ex) {
					System.out.print("Do not connect to DB - Error:" + ex);
				}
			}
		});
		selectionmodel1.clearSelection();

		// PIE CHART
		Piechart.setData(getPie());
		Piechart.getStyleClass().add("Pie-chart");
		Piechart.getStylesheets().add("Pie.css");
		Piechart.setTitle("Financial Distribution Chart");
		Piechart.setLabelLineLength(20);

		Pane PiePane = new Pane();
		PiePane.getChildren().add(Piechart);

		PiePane.setStyle("-fx-background-color:#ffffff");
		tab[0].setContent(Spending_pane);
		tab[1].setContent(table_container);
		tab[2].setContent(PiePane);
		tabpane.setStyle("-fx-cursor:default");
		Basescene = new Scene(tabpane, 800, 550);
		Basescene.getStylesheets().add("JavaFx.css");

		basestage.setTitle("Budget Buddy");
		basestage.setScene(Basescene);

		// THIS IS --> Entryscene <-- >>>>>>>>>>>>>>>>>>>>>>
		Button Backbutton = new Button("Back");
		Backbutton.setPrefSize(150, 50);
		Backbutton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

		Backbutton.getStyleClass().add("back-button");
		Backbutton.setOnAction(e -> {
			basestage.setScene(Basescene);
		});
		Button Done = new Button("Done");
		Done.setPrefSize(150, 50);
		Done.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

		Done.getStyleClass().add("back-button");

//		Insets doneset = new Insets(0, 0, 0, 277);
		HBox button_container = new HBox(550);  // Set spacing between buttons
//		HBox.setMargin(Done, doneset);
		button_container.getChildren().addAll(Backbutton, Done);
		button_container.setStyle("-fx-cursor:default;-fx-background-color: #ffffff;");
		button_container.getStylesheets().add("JavaFx.css");

		GridPane entrypane = new GridPane();
		entrypane.setStyle("-fx-cursor:default;-fx-background-color: #ffffff;");
		entrypane.setHgap(10);
		entrypane.setVgap(10);
		entrypane.setAlignment(Pos.CENTER);
        entrypane.setPrefSize(800, 550);  // Increased size (width x height)

		// DatePicker
		Label Date = new Label("Date          :");
		Date.getStyleClass().add("label-entry");
		DatePicker datepicker = new DatePicker();
		datepicker.setConverter(new StringConverter<LocalDate>() {
			String pattern = "dd-MM-yyyy";
			DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(pattern);

			{
				datepicker.setPromptText(pattern.toLowerCase());
			}

			@Override
			public String toString(LocalDate date) {
				if (date != null) {
					return dateFormatter.format(date);
				} else {
					return "";
				}
			}

			@Override
			public LocalDate fromString(String string) {
				if (string != null && !string.isEmpty()) {
					return LocalDate.parse(string, dateFormatter);
				} else {
					return null;
				}
			}
		});
		datepicker.setValue(LocalDate.now());
		entrypane.add(Date, 0, 3);
		entrypane.add(datepicker, 1, 3);

		// Error Message
		Label Error = new Label();
		Error.setStyle("-fx-alignment:CENTER;-fx-text-fill:Red;");

		Label Category = new Label("Category  :");
		Category.getStyleClass().add("label-entry");
		TextField textfield = new TextField();
		entrypane.add(Category, 0, 4);
		entrypane.add(textfield, 1, 4);

		Label Amount = new Label("Amount    :");
		Amount.getStyleClass().add("label-entry");
		TextField Amountfield = new TextField();
		entrypane.add(Amount, 0, 5);
		entrypane.add(Amountfield, 1, 5);
		Backbutton.setOnAction(e -> {
			basestage.setScene(Basescene);
			textfield.clear();
			Amountfield.clear();
		});
		Done.setOnAction(e -> {
			LocalDate d = datepicker.getValue();
			String c = textfield.getText();
			String ddd = Amountfield.getText();
			double D = 0;
			if (!ddd.isEmpty())
				D = Double.parseDouble(ddd);

			if (!c.isEmpty() && !ddd.isEmpty()) {
				try {
					Connection conn = DriverManager.getConnection(database, username, password);
					PreparedStatement pre = conn
							.prepareStatement("INSERT INTO tdtb (Category, Date, trtrtr, username) VALUES (?, ?, ?, ?)");
					pre.setString(1, c);
					pre.setDate(2, java.sql.Date.valueOf(d));
					pre.setDouble(3, D);
					pre.setString(4, loggedInUsername); // Make sure to get the logged-in user's username correctly

					int o = pre.executeUpdate();
					System.out.println(o + "row affected");
					

					// Check if the transaction exceeds $500 and add to notification queue
					if (D > 500) {
						notificationQueue.add("Alert: High Transaction Recorded : $" + D + " on " + d.toString());
						displayNotifications();
					}
					// Query for total expenses and income
					double totalExpenses = 0.0;
					double totalIncome = 0.0;

					// Query for total expenses
					ResultSet resExp = conn.createStatement()
							.executeQuery("SELECT SUM(trtrtr) AS total FROM tdtb WHERE Category <> 'Salary'");
					if (resExp.next()) {
						totalExpenses = resExp.getDouble("total");
					}

					// Query for total income
					ResultSet resInc = conn.createStatement()
							.executeQuery("SELECT SUM(trtrtr) AS total FROM tdtb WHERE Category = 'Salary'");
					if (resInc.next()) {
						totalIncome = resInc.getDouble("total");
					}

					double newSavings = totalIncome - totalExpenses;

					// Check if savings are less than expenses
					if (newSavings < totalExpenses) {
						String notificationMessage = "Warning: Your expenses (" + Formate(totalExpenses)
								+ ") are now greater than your savings (" + Formate(newSavings)
								+ "). Consider reviewing your spending.";
						notificationQueue.add(notificationMessage);
						displayNotifications();
					}
					conn.close();
					refreshTableViews();
					Piechart.setData(getPie());

				} catch (SQLException ex) {
					System.out.print("Do not connect to DB - Error:" + ex);
				}
				textfield.clear();
				Amountfield.clear();
				basestage.setScene(Basescene);
			} else {
				if (c.isEmpty())
					Error.setText("Please Select Cetegory");
				if (ddd.isEmpty())
					Error.setText("Please Enter Amount");
				if (c.isEmpty() && ddd.isEmpty())
					Error.setText("Please Enter Amount & Select Cetegory");
			}
		});

		Label TDetail = new Label("  Transaction Details  ");
		TDetail.setMinWidth(800);
		TDetail.getStyleClass().add("transaction-label");

		Text TText = new Text();
		TText.setText("These details are required to be entered:" + "\n\nDate:\nThe date the Transaction occurred."
				+ "\n\nCategory:\nThe Category the Transaction falls "
				+ "into e.g. Clothes, Salary.\n\nAmount:\nThe monetary amount.");
		TText.setFont(new Font(14));
		TText.setFill(Color.WHITE);
		TText.setFontSmoothingType(FontSmoothingType.LCD);
		VBox Text_container = new VBox(TText);
		Text_container.setStyle(
				"-fx-background-color:#262626;-fx-padding:10px;-fx-border-color:#ffffff;-fx-border-width:5px;-fx-border-radius:20px");

		VBox entry_pane = new VBox();
		entry_pane.setStyle("-fx-background-color: #ffffff");
		Insets inset = new Insets(20, 5, 20, 5);
		VBox.setMargin(Text_container, inset);

		entry_pane.getChildren().addAll(button_container, TDetail, entrypane, Error, Text_container);
		Entryscene = new Scene(entry_pane, 800, 550);
		Entryscene.getStylesheets().add("JavaFx.css");

		// THIS IS --> ExpenseType <-- >>>>>>>>>>>>>>>>>>>>>>
		Button backbutton = new Button(" Back ");
		backbutton.setPrefWidth(400);
		backbutton.getStyleClass().add("back-button");
		backbutton.setPrefHeight(43);
		backbutton.setOnAction(e -> {
			basestage.setScene(Entryscene);
		});

		Image[] Fuelimage = { new Image("Fuel.png"), new Image("Gifts.png"), new Image("Shopping.png"),
				new Image("Clothes.png"), new Image("Eating Out.png"), new Image("Entertainment.png"),
				new Image("General.png"), new Image("Holidays.png"), new Image("Kids.png"), new Image("Sports.png"),
				new Image("Travel.png") };

		ImageView[] imageview = new ImageView[11];
		for (int i = 0; i < 11; i++) {
			imageview[i] = new ImageView(Fuelimage[i]);
			imageview[i].setFitHeight(25);
			imageview[i].setFitWidth(25);
		}
		imageview[0].setFitHeight(23);
		imageview[0].setFitWidth(23);
		imageview[10].setFitHeight(30);
		imageview[10].setFitWidth(30);
		Button[] ExpenseCategory = { new Button("  Fuel", imageview[0]), new Button("  Gifts", imageview[1]),
				new Button("  Shopping", imageview[2]), new Button("  Clothes", imageview[3]),
				new Button("  Eating Out", imageview[4]), new Button("  Entertainment", imageview[5]),
				new Button("  General", imageview[6]), new Button("  Holidays", imageview[7]),
				new Button("  Kids", imageview[8]), new Button("  Sports", imageview[9]),
				new Button("  Travel", imageview[10]) };
		ExpenseCategory[0].setStyle("-fx-padding:2px 10px 2px 12px");
		for (Button Ex : ExpenseCategory) {
			Ex.setPrefWidth(400);
			Ex.setAlignment(Pos.BASELINE_LEFT);
			Ex.getStyleClass().add("Expensescene-button");
			// 11 buttons
		}
		VBox Expensetype = new VBox();
		Expensetype.setStyle("-fx-background-color:#ffffff;-fx-cursor:default");
		Expensetype.getChildren().add(backbutton);
		for (Button Ex : ExpenseCategory) {
			Expensetype.getChildren().add(Ex);
		}

		ExpenseType = new Scene(Expensetype, 400, 550);
		ExpenseType.getStylesheets().add("JavaFx.css");
		// Expense Button event open ExspenseType
		Expense.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				semaphore = "Expense";
				basestage.setScene(Entryscene);
				System.out.println("Expense category");
			}
		});
		// ALL BUTTON EVENT
		ExpenseCategory[0].setOnAction(e -> {
			textfield.setText("Fuel");
			basestage.setScene(Entryscene);
		});
		ExpenseCategory[1].setOnAction(e -> {
			textfield.setText("Gifts");
			basestage.setScene(Entryscene);
		});
		ExpenseCategory[2].setOnAction(e -> {
			textfield.setText("Shopping");
			basestage.setScene(Entryscene);
		});
		ExpenseCategory[3].setOnAction(e -> {
			textfield.setText("Clothes");
			basestage.setScene(Entryscene);
		});
		ExpenseCategory[4].setOnAction(e -> {
			textfield.setText("Eating Out");
			basestage.setScene(Entryscene);
		});
		ExpenseCategory[5].setOnAction(e -> {
			textfield.setText("Entertainment");
			basestage.setScene(Entryscene);
		});
		ExpenseCategory[6].setOnAction(e -> {
			textfield.setText("General");
			basestage.setScene(Entryscene);
		});
		ExpenseCategory[7].setOnAction(e -> {
			textfield.setText("Holidays");
			basestage.setScene(Entryscene);
		});
		ExpenseCategory[8].setOnAction(e -> {
			textfield.setText("Kids");
			basestage.setScene(Entryscene);
		});
		ExpenseCategory[9].setOnAction(e -> {
			textfield.setText("Sports");
			basestage.setScene(Entryscene);
		});
		ExpenseCategory[10].setOnAction(e -> {
			textfield.setText("Travel");
			basestage.setScene(Entryscene);
		});

		// THIS IS --> IncomeType <-- >>>>>>>>>>>>>>>>>>>>>>>>>>>>>
		Button bacKbutton = new Button(" Back ");
		bacKbutton.setPrefWidth(400);
		bacKbutton.setPrefHeight(43);
		bacKbutton.getStyleClass().add("back-button");
		bacKbutton.setOnAction(e -> {
			basestage.setScene(Entryscene);
		});

		Button Salary = new Button("Salary");
		Salary.setAlignment(Pos.BASELINE_LEFT);
		Salary.setPrefWidth(400);
		Salary.getStyleClass().add("Expensescene-button");

		VBox Incometype = new VBox();
		Incometype.getChildren().addAll(bacKbutton, Salary);
		Incometype.setStyle("-fx-background-color:#ffffff;-fx-cursor:default");

		IncomeType = new Scene(Incometype, 400, 550);
		IncomeType.getStylesheets().add("JavaFx.css");
		// Income Button event open IncomeType
		Income.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				semaphore = "Income";
				basestage.setScene(Entryscene);
				System.out.println("Income category");
			}
		});
		Salary.setOnAction(e -> {
			textfield.setText("Salary");
			basestage.setScene(Entryscene);
		});
		// IncomeType Over <<<<<<<<<<<<<<<<<
		textfield.setOnMouseClicked(e -> {
			if (semaphore.equals("Expense"))
				basestage.setScene(ExpenseType);
			if (semaphore.equals("Income"))
				basestage.setScene(IncomeType);
		});
		basestage.setResizable(false);
		basestage.show();
		Button addTransactionBtn = new Button("Add Transaction");
		addTransactionBtn.setOnAction(e -> {
			// Example data
			transactionTree.insert(LocalDate.now(), 200, "Groceries");
			// You might want to add GUI to enter these details
		});

	}



	private void setupLogoutTabContent(Tab tab) {
		// TODO Auto-generated method stub
		// Create the main layout pane for the logout tab
	    BorderPane logoutContent = new BorderPane();
	    logoutContent.setStyle("-fx-background-color: #ffffff;"); // Set white background for clarity

	    // Image on the left side
	    ImageView logoutImage = new ImageView(new Image("logout.gif"));
	    logoutImage.setFitHeight(550);
	    logoutImage.setFitWidth(400);
	    // Wrap the image in a pane to control padding and placement
	    Pane imagePane = new Pane(logoutImage);
	    imagePane.setPadding(new Insets(20));
	    logoutContent.setLeft(imagePane); // Place image pane on the left

	    // VBox for right side to hold text and button
	    VBox textAndButtonBox = new VBox(20);
	    textAndButtonBox.setAlignment(Pos.CENTER_LEFT);
	    textAndButtonBox.setPadding(new Insets(20));

	    // First message
	    Label logoutMessage = new Label("Are you sure you want to logout? ");
	    logoutMessage.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");
	    
	    // Additional message
	    Label enjoyMessage = new Label("Click Yes to confirm or select another tab to cancel.");
	    enjoyMessage.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");

	    // Logout button
	    Button yesButton = new Button("Yes");
	    yesButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;"); // Green button with white text
	    yesButton.setOnAction(e -> {
	        primaryStage.setScene(createLoginScene(primaryStage));  // Log out and switch to login scene
	    });

	    // Add components to the VBox
	    textAndButtonBox.getChildren().addAll(logoutMessage, enjoyMessage, yesButton);
	    logoutContent.setRight(textAndButtonBox); // Place text and button on the right

	    // Set the logout content as the content of the tab
	    tab.setContent(logoutContent);
	}

	private void displayNotifications() {
		while (!notificationQueue.isEmpty()) {
			String notification = notificationQueue.poll();
			// Use JavaFX Alert to display the notification as a popup
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Notification");
			alert.setHeaderText(null); // No header text
			alert.setContentText(notification);
			alert.showAndWait(); // Display the alert and wait for the user to close it
		}
	}

	private void refreshTableViews() {
		tableview1.setItems(getCategory());
		tableview.setItems(getExpense());
		// Reapply sort to ensure the order is maintained after refresh
		tableview1.sort();
		tableview.sort();
	}
}