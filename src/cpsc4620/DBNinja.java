package cpsc4620;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.Date;

/*
 * This file is where most of your code changes will occur You will write the code to retrieve
 * information from the database, or save information to the database
 * 
 * The class has several hard coded static variables used for the connection, you will need to
 * change those to your connection information
 * 
 * This class also has static string variables for pickup, delivery and dine-in. If your database
 * stores the strings differently (i.e "pick-up" vs "pickup") changing these static variables will
 * ensure that the comparison is checking for the right string in other places in the program. You
 * will also need to use these strings if you store this as boolean fields or an integer.
 * 
 * 
 */

/**
 * A utility class to help add and retrieve information from the database
 */

public final class DBNinja {
	private static Connection conn;

	// Change these variables to however you record dine-in, pick-up and delivery,
	// and sizes and crusts
	public final static String pickup = "pickup";
	public final static String delivery = "delivery";
	public final static String dine_in = "dinein";

	public final static String size_s = "small";
	public final static String size_m = "medium";
	public final static String size_l = "Large";
	public final static String size_xl = "XLarge";

	public final static String crust_thin = "Thin";
	public final static String crust_orig = "Original";
	public final static String crust_pan = "Pan";
	public final static String crust_gf = "Gluten-Free";

	private static boolean connect_to_db() throws SQLException, IOException {

		try {
			conn = DBConnector.make_connection();
			return true;
		} catch (SQLException e) {
			return false;
		} catch (IOException e) {
			return false;
		}

	}

	public static void addOrder(Order o) throws SQLException, IOException {
		/*
		 * add code to add the order to the DB. Remember that we're not just
		 * adding the order to the order DB table, but we're also recording
		 * the necessary data for the delivery, dinein, and pickup tables
		 */
		try {
			connect_to_db();
			Statement stmt = (Statement) conn.createStatement();

			String query1 = "INSERT INTO ordersummary(OrderID,OrderCustomerID, OrderType, OrderCost, OrderPrice, OrderTime)"
					+ "VALUES (" + o.getOrderID() + "," + o.getCustID() + ",'" + o.getOrderType() + "',"
					+ o.getBusPrice() + "," + o.getCustPrice() + ",'" + o.getDate() + "')";
			stmt.executeUpdate(query1);

			if (o.getOrderType() == dine_in) {
				String query2 = "INSERT INTO dinein VALUES(" + o.getOrderID() + "," + ((DineinOrder) o).getTableNum()
						+ ")";
				stmt.executeUpdate(query2);
			} else if (o.getOrderType() == delivery) {
				String query3 = "INSERT INTO delivery VALUES(" + o.getOrderID() + ",'"
						+ ((DeliveryOrder) o).getAddress() + "')";
				stmt.executeUpdate(query3);
			} else {
				String query4 = "INSERT INTO pickup VALUES(" + o.getOrderID() + "," + ((PickupOrder) o).getIsPickedUp()
						+ ")";
				stmt.executeUpdate(query4);
			}
			conn.close();
		} catch (Exception e) {
			System.err.println("Got an exception!");
			e.printStackTrace();
			System.out.println(e);
		}
	}

	public static void addPizza(Pizza p) throws SQLException, IOException {
		connect_to_db();
		/*
		 * Add the code needed to insert the pizza into into the database.
		 * Keep in mind adding pizza discounts to that bridge table and
		 * instance of topping usage to that bridge table if you have't accounted
		 * for that somewhere else.
		 */

		// DO NOT FORGET TO CLOSE YOUR CONNECTION

		// Create the SQL statement for inserting a new pizza
		try {
			String query1 = "INSERT INTO pizza (PizzaID,PizzaOrderIDmPizzaCrust, PizzaSize, PizzaCost, PizzaPrice, PizzaState) "
					+
					"VALUES ('"+p.getPizzaID()+ "', " + p.getOrderID() +"','"+ p.getSize() + "', '" + p.getCrustType() + ", '"
					+ p.getBusPrice() + "', " + p.getCustPrice() + ", " + p.getPizzaState()+ ")";

			// Execute the SQL statement to insert the new pizza
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(query1);
			stmt.close();

			// Retrieve the ID of the newly inserted pizza
			String getLastInsertIDSQL = "SELECT LAST_INSERT_ID()";
			Statement getLastInsertIDStatement = conn.createStatement();
			ResultSet rs = getLastInsertIDStatement.executeQuery(getLastInsertIDSQL);
			if (rs.next()) {
				int pizzaID = rs.getInt(1);
				p.setPizzaID(pizzaID);
			}
			rs.close();
			getLastInsertIDStatement.close();

			// Insert the toppings for the pizza into the database
			ArrayList<Topping> toppings = p.getToppings();
			for (int i = 0; i < toppings.size(); i++) {
				Topping t = toppings.get(i);
				String insertToppingSQL = "INSERT INTO pizza_toppings (pizza_id, topping_id, is_doubled) " +
						"VALUES (" + p.getPizzaID() + ", " + t.getTopName() + ", " + p.getIsDoubleArray()[i] + ")";
				Statement insertToppingStatement = conn.createStatement();
				insertToppingStatement.executeUpdate(insertToppingSQL);
				insertToppingStatement.close();
			}

			// Insert the discounts for the pizza into the database
			ArrayList<Discount> discounts = p.getDiscounts();
			for (int i = 0; i < discounts.size(); i++) {
				Discount d = discounts.get(i);
				String insertDiscountSQL = "INSERT INTO pizza_discounts (pizza_id, discount_id) " +
						"VALUES (" + p.getPizzaID() + ", " + d.getDiscountID() + ")";
				Statement insertDiscountStatement = conn.createStatement();
				insertDiscountStatement.executeUpdate(insertDiscountSQL);
				insertDiscountStatement.close();
			}
			// Close the database connection
			conn.close();
		} catch (Exception e) {
			System.err.println("Got an exception!");
			e.printStackTrace();
			System.out.println(e);
		}

	}

	public static int getMaxPizzaID() throws SQLException, IOException {
		connect_to_db();
		/*
		 * A function I needed because I forgot to make my pizzas auto increment in my
		 * DB.
		 * It goes and fetches the largest PizzaID in the pizza table.
		 * You wont need to implement this function if you didn't forget to do that
		 */
		int max_id = 0;
		try {
			String query = "SELECT COALESCE(MAX(PizzaID), 1) FROM pizza";
			Statement stmt = conn.prepareStatement(query);
			ResultSet rs = stmt.executeQuery(query);
			if(rs.next()){
				max_id = rs.getInt(1);
			}
		}catch(Exception e){
			System.err.println("Got an exception!");
			e.printStackTrace();
			System.out.println(e);
		}finally{
			conn.close();
		}
		// DO NOT FORGET TO CLOSE YOUR CONNECTION
		return max_id;
	}

	public static void useTopping(Pizza p, Topping t, boolean isDoubled) throws SQLException, IOException
	/*
	 * this function will update toppings inventory in SQL and add entities to the
	 * Pizzatops table. Pass in the p pizza that is using t topping
	 */
	{
		connect_to_db();
		/*
		 * This function should 2 two things.
		 * We need to update the topping inventory every time we use t topping
		 * (accounting for extra toppings as well)
		 * and we need to add that instance of topping usage to the pizza-topping bridge
		 * if we haven't done that elsewhere
		 * Ideally, you should't let toppings go negative. If someone tries to use
		 * toppings that you don't have, just print
		 * that you've run out of that topping.
		 */

		// DO NOT FORGET TO CLOSE YOUR CONNECTION
		try {
			// Retrieve the current inventory of the topping
			Statement stmt = conn.createStatement();
			String query1 = "SELECT ToppingInventory FROM topping WHERE ToppingName = '" + t.getTopName()+"'";
			ResultSet rs1 = stmt.executeQuery(query1);
			int curINVT = 0;
			if (rs1.next()) {
				curINVT = rs1.getInt("ToppingInventory");
			}

			// Calculate the amount of topping needed based on pizza size and isDoubled flag
			double amount = 0.0;
			switch (p.getSize()) {
				case "Personal":
					amount = t.getPerAMT();
					break;
				case "Medium":
					amount = t.getMedAMT();
					break;
				case "Large":
					amount = t.getLgAMT();
					break;
				case "Extra Large":
					amount = t.getXLAMT();
					break;
			}
			if (isDoubled) {
				amount *= 2.0;
			}

			// Check if there is enough inventory to use the topping
			if (curINVT < amount) {
				System.out.println("Sorry, we ran out of " + t.getTopName() + ".");
				return;
			}

			// Update the inventory of the topping
			curINVT -= amount;
			String query2 = "UPDATE topping SET ToppingInventory = " + curINVT + " WHERE ToppingName = '" + t.getTopName()+"'";
			int rowsAffected = stmt.executeUpdate(query2);
			if (rowsAffected != 1) {
				System.out.println("Failed to update the inventory of " + t.getTopName() + ".");
				return;
			}

			// Add an entry to the PizzaTops table
			String query3 = "INSERT INTO pizzatopping (PizzaToppingPizzaID, PizzaToppingToppingName, PizzaToppingExtra) VALUES ("
					+ p.getPizzaID() + ", '" + t.getTopName() + "', " + (isDoubled ? 1 : 0) + ")";
			rowsAffected = stmt.executeUpdate(query3);
			if (rowsAffected != 1) {
				System.out.println("Failed to add topping usage to PizzaTops table.");
			}

			conn.close();
		} catch (Exception e) {
			System.err.println("Got an exception!");
			e.printStackTrace();
			System.out.println(e);
		}

	}

	public static void usePizzaDiscount(Pizza p, Discount d) throws SQLException, IOException {
		connect_to_db();
		/*
		 * Helper function I used to update the pizza-discount bridge table.
		 * You might use this, you might not depending on where / how to want to update
		 * this table
		 */

		// DO NOT FORGET TO CLOSE YOUR CONNECTION
	}

	public static void useOrderDiscount(Order o, Discount d) throws SQLException, IOException {
		connect_to_db();
		/*
		 * Helper function I used to update the pizza-discount bridge table.
		 * You might use this, you might not depending on where / how to want to update
		 * this table
		 */

		// Update the order in the database
		String sql = "UPDATE orders SET DiscountOrderName = '" + d.getDiscountName() + "', DiscountOrderID  = "
				+ o.getOrderID();
		Statement stmt = conn.createStatement();
		stmt.executeUpdate(sql);

		conn.close();

	}

	public static void addCustomer(Customer c) throws SQLException, IOException {
		connect_to_db();
		try (Statement stmt = (Statement) conn.createStatement()) {
			String query = "INSERT INTO customer (CustomerID, CustomerFName, CustomerLName, CustomerPhoneNumber) VALUES ("
					+ c.getCustID() + ", '"
					+ c.getFName() + "', '" + c.getLName() + "', '" + c.getPhone() + "')";
			stmt.executeUpdate(query);
		}

	}

	public static void CompleteOrder(Order o) throws SQLException, IOException {
		connect_to_db();
		/*
		 * add code to mark an order as complete in the DB. You may have a boolean field
		 * for this, or maybe a completed time timestamp. However you have it.
		 */

		// DO NOT FORGET TO CLOSE YOUR CONNECTION

		// Update the order to mark it as complete
		String sql = "UPDATE orders SET isComplete = 1 WHERE OrderID = " + o.getOrderID();
		Statement stmt = conn.createStatement();
		stmt.executeUpdate(sql);

		conn.close();
	}

	public static void AddToInventory(Topping t, double toAdd) throws SQLException, IOException {
		connect_to_db();
		/*
		 * Adds toAdd amount of topping to topping t.
		 */

		// DO NOT FORGET TO CLOSE YOUR CONNECTION
		try {
			String sql = "UPDATE topping SET ToppingInventory = ToppingInventory + " + toAdd + " WHERE ToppingName = '" + t.getTopName()+"'";
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			System.out.println("Added " + toAdd + " to " + t.getTopName() + " inventory.");
		} catch (SQLException e) {
			System.out.println("Failed to add to topping inventory: " + e.getMessage());
		} finally {
			conn.close();
		}
	}

	public static void printInventory() throws SQLException, IOException {
		connect_to_db();

		/*
		 * I used this function to PRINT (not return) the inventory list.
		 * When you print the inventory (either here or somewhere else)
		 * be sure that you print it in a way that is readable.
		 * 
		 * 
		 * 
		 * The topping list should also print in alphabetical order
		 */

		// DO NOT FORGET TO CLOSE YOUR CONNECTION
		String sql = "SELECT ToppingName, ToppingInventory FROM topping ORDER BY ToppingName";
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);

		System.out.println("Inventory:\n");
		int count = 1;
		System.out.printf("%-5s %-25s %-5s\n","ID","Name","CurINVT");
		while (rs.next()) {
			String name = rs.getString("ToppingName");
			double curINVT = rs.getDouble("ToppingInventory");
			System.out.printf("%-5d %-25s %5.0f\n",count, name, curINVT);
			count = count+1;
		}
		conn.close();
	}

	public static ArrayList<Topping> getInventory() throws SQLException, IOException {
		connect_to_db();
		ArrayList<Topping> topping_list = new ArrayList<Topping>();
		/*
		 * This function actually returns the toppings. The toppings
		 * should be returned in alphabetical order if you don't
		 * plan on using a printInventory function
		 */
		try {
			String query = "SELECT * FROM topping ORDER BY ToppingName";
			Statement stmt = conn.prepareStatement(query);
			ResultSet rs = stmt.executeQuery(query);
			int count = 1;
			while (rs.next()) {
				int topID = rs.getRow();
				String topName = rs.getString("ToppingName");
				double perAMT = rs.getDouble("ToppingSmallAmount");
				double medAMT = rs.getDouble("ToppingMediumAmount");
				double lgAMT = rs.getDouble("ToppingLargeAmount");
				double xLAMT = rs.getDouble("ToppingXLargeAmount");
				double custPrice = rs.getDouble("ToppingPrice");
				double busPrice = rs.getDouble("ToppingCostPerUnit");
				int minINVT = 0; // You can set this value if needed
				int curINVT = rs.getInt("ToppingInventory");
				Topping topping = new Topping(topID, topName, perAMT, medAMT, lgAMT, xLAMT, custPrice, busPrice, minINVT, curINVT);
				topping_list.add(topping);
			}
			conn.close();
		} catch (Exception e) {
			System.err.println("Got an exception!");
			e.printStackTrace();
			System.out.println(e);
		}
		// DO NOT FORGET TO CLOSE YOUR CONNECTION
		return topping_list;
	}

	public static ArrayList<Order> getCurrentOrders() throws SQLException, IOException {
		connect_to_db();
		/*
		 * This function should return an arraylist of all of the orders.
		 * Remember that in Java, we account for supertypes and subtypes
		 * which means that when we create an arrayList of orders, that really
		 * means we have an arrayList of dineinOrders, deliveryOrders, and pickupOrders.
		 * 
		 * Also, like toppings, whenever we print out the orders using menu function 4
		 * and 5 these orders should print in order from newest to oldest.
		 */

		// DO NOT FORGET TO CLOSE YOUR CONNECTION
		ArrayList<Order> orders = new ArrayList<Order>();

		// Query the database for all orders
		String sql = "SELECT * FROM ordersummary ORDER BY OrderTime DESC";
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);

		// Loop through each row in the result set
		while (rs.next()) {
			int orderID = rs.getInt("OrderID");
			int custID = rs.getInt("OrderCustomerID");
			String orderType = rs.getString("OrderType");
			String date = rs.getString("OrderTime");
			double custPrice = rs.getDouble("OrderPrice");
			double busPrice = rs.getDouble("OrderCost");
			int isComplete = rs.getInt("OrderComplete");

			// Determine the subtype of the order based on the orderType column
			Order order = null;
			Statement stmt2 = conn.createStatement();
			ResultSet rs2;
			if (orderType.equals(DBNinja.dine_in)) {
				String sql2 = "Select DineInTableNumber from dinein where DineInOrderID='"+orderID+"'";
				rs2 = stmt2.executeQuery(sql2);
				while(rs2.next()) {
					int tableNum = rs2.getInt("DineInTableNumber");
					order = new DineinOrder(orderID, custID, date, custPrice, busPrice, isComplete, tableNum);
				}
			} else if (orderType.equals(DBNinja.delivery)) {
				String sql2 = "Select DeliveryAddress from delivery where DeliveryOrderID='"+orderID+"'";
				rs2 = stmt2.executeQuery(sql2);
				while(rs2.next()) {
					String address = rs2.getString("DeliveryAddress");
					order = new DeliveryOrder(orderID, custID, date, custPrice, busPrice, isComplete, address);
				}
			} else {
				String sql2 = "Select isPickedUp from pickup where PickupOrderID='"+orderID+"'";
				rs2 = stmt2.executeQuery(sql2);
				while(rs2.next()) {
					int isPickedUp = rs2.getInt("isPickedUp");
					order = new PickupOrder(orderID, custID, date, custPrice, busPrice, isPickedUp, isComplete);
				}
			}
			orders.add(order);
		}

		// Close the database connection and return the ArrayList of orders
		conn.close();
		return orders;
	}

	public static ArrayList<Order> sortOrders(ArrayList<Order> list) {
		/*
		 * This was a function that I used to sort my arraylist based on date.
		 * You may or may not need this function depending on how you fetch
		 * your orders from the DB in the getCurrentOrders function.
		 */

		// DO NOT FORGET TO CLOSE YOUR CONNECTION
		return null;

	}

	public static boolean checkDate(int year, int month, int day, String dateOfOrder) {
		// Helper function I used to help sort my dates. You likely wont need these

		return false;
	}

	/*
	 * The next 3 private functions help get the individual components of a SQL
	 * datetime object.
	 * You're welcome to keep them or remove them.
	 */
	private static int getYear(String date)// assumes date format 'YYYY-MM-DD HH:mm:ss'
	{
		return Integer.parseInt(date.substring(0, 4));
	}

	private static int getMonth(String date)// assumes date format 'YYYY-MM-DD HH:mm:ss'
	{
		return Integer.parseInt(date.substring(5, 7));
	}

	private static int getDay(String date)// assumes date format 'YYYY-MM-DD HH:mm:ss'
	{
		return Integer.parseInt(date.substring(8, 10));
	}

	public static double getBaseCustPrice(String size, String crust) throws SQLException, IOException {
		connect_to_db();
		double bp = 0.0;
		// add code to get the base price (for the customer) for that size and crust
		// pizza Depending on how
		// you store size & crust in your database, you may have to do a conversion

		// DO NOT FORGET TO CLOSE YOUR CONNECTION
		// Get the base price for the pizza based on its size and crust
		String query = "SELECT BasePrice FROM baseprice WHERE Size = ? AND Crust = ?";
		try (PreparedStatement stmt = conn.prepareStatement(query)) {
			stmt.setString(1, size);
			stmt.setString(2, crust);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				bp = rs.getDouble("BasePrice");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// Close the database connection
		conn.close();

		return bp;
	}

	public static String getCustomerName(int CustID) throws SQLException, IOException {
		/*
		 * This is a helper function I used to fetch the name of a customer
		 * based on a customer ID. It actually gets called in the Order class
		 * so I'll keep the implementation here. You're welcome to change
		 * how the order print statements work so that you don't need this function.
		 */
		connect_to_db();
		String ret = "";
		String query = "Select CustomerFName, CustomerLName From customer WHERE CustomerID=" + CustID + ";";
		Statement stmt = conn.createStatement();
		ResultSet rset = stmt.executeQuery(query);

		while (rset.next()) {
			ret = rset.getString(1) + " " + rset.getString(2);
		}
		conn.close();
		return ret;
	}

	public static double getBaseBusPrice(String size, String crust) throws SQLException, IOException {
		connect_to_db();
		double bp = 0.0;
		// add code to get the base cost (for the business) for that size and crust
		// pizza Depending on how
		// you store size and crust in your database, you may have to do a conversion

		// DO NOT FORGET TO CLOSE YOUR CONNECTION
		String query = "SELECT BaseCost FROM baseprice WHERE Size = ? AND Crust = ?";
		PreparedStatement stmt = conn.prepareStatement(query);
		stmt.setString(1, size);
		stmt.setString(2, crust);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			bp = rs.getDouble("base_cost");
		}
		conn.close();
		return bp;
	}

	public static ArrayList<Discount> getDiscountList() throws SQLException, IOException {
		ArrayList<Discount> discs = new ArrayList<Discount>();
		connect_to_db();
		// returns a list of all the discounts.
		try {
			String query = "SELECT * FROM discount ORDER BY DiscountName ASC";
			Statement stmt = conn.prepareStatement(query);
			ResultSet rs = stmt.executeQuery(query);
			int countID = 1;
			while (rs.next()) {
				String name = rs.getString("DiscountName");
				String type = rs.getString("DiscountType");
				double amt = rs.getDouble("DiscountAmt");
				Boolean isPercent = false;
				if (type.equals("percentage")) {
					isPercent = true;
					amt = amt / 100.0;
				}
				Discount d = new Discount(countID, name, amt, isPercent);
				discs.add(d);
			}
			conn.close();
		} catch (Exception e) {
			System.err.println("Got an exception!");
			e.printStackTrace();
			System.out.println(e);
		}
		// DO NOT FORGET TO CLOSE YOUR CONNECTION
		return discs;
	}

	public static ArrayList<Customer> getCustomerList() throws SQLException, IOException {
		ArrayList<Customer> custs = new ArrayList<Customer>();
		connect_to_db();
		/*
		 * return an arrayList of all the customers. These customers should
		 * print in alphabetical order, so account for that as you see fit.
		 */
		try {
			String query = "SELECT * FROM customer ORDER BY CustomerFName ASC";
			Statement stmt = conn.prepareStatement(query);
			ResultSet rs = stmt.executeQuery(query);

			while (rs.next()) {
				int id = rs.getInt("CustomerID");
				String fname = rs.getString("CustomerFName");
				String lname = rs.getString("CustomerLName");
				String phone = rs.getString("CustomerPhoneNumber");
				Customer cust = new Customer(id, fname, lname, phone);
				custs.add(cust);
			}
			conn.close();
		} catch (Exception e) {
			System.err.println("Got an exception!");
			e.printStackTrace();
			System.out.println(e);
		}

		return custs;
	}

	public static int getNextOrderID() throws SQLException, IOException {
		/*
		 * A helper function I had to use because I forgot to make
		 * my OrderID auto increment...You can remove it if you
		 * did not forget to auto increment your orderID.
		 */
		connect_to_db();
		int max_id = 0;
		try {
			String query = "SELECT COALESCE(MAX(OrderID), 1) FROM ordersummary";
			Statement stmt = conn.prepareStatement(query);
			ResultSet rs = stmt.executeQuery(query);
			if(rs.next()){
				max_id = rs.getInt(1);
			}
		} catch (Exception e) {
			System.err.println("Got an exception!");
			e.printStackTrace();
			System.out.println(e);
		} finally {
			conn.close();
		}
		// DO NOT FORGET TO CLOSE YOUR CONNECTION
		return max_id + 1;
	}

	public static void printToppingPopReport() throws SQLException, IOException {
		connect_to_db();
		/*
		 * Prints the ToppingPopularity view. Remember that these views
		 * need to exist in your DB, so be sure you've run your createViews.sql
		 * files on your testing DB if you haven't already.
		 * 
		 * I'm not picky about how they print (other than that it should
		 * be in alphabetical order by name), just make sure it's readable.
		 */

		// DO NOT FORGET TO CLOSE YOUR CONNECTION
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT * FROM ToppingPopularity ORDER BY Topping ASC");
		System.out.println("Topping Popularity Report:\n");
		System.out.println("Topping\t\t\tTopping Count");
		while (rs.next()) {
			String name = rs.getString("Topping");
			int orders = rs.getInt("ToppingCount");
			System.out.printf("%-20s%d\n", name, orders);
		}
		rs.close();
		stmt.close();
		conn.close();
	}

	public static void printProfitByPizzaReport() throws SQLException, IOException {
		connect_to_db();
		/*
		 * Prints the ProfitByPizza view. Remember that these views
		 * need to exist in your DB, so be sure you've run your createViews.sql
		 * files on your testing DB if you haven't already.
		 * 
		 * I'm not picky about how they print, just make sure it's readable.
		 */

		// DO NOT FORGET TO CLOSE YOUR CONNECTION

		String query = "SELECT * FROM ProfitByPizza";
		try (PreparedStatement stmt = conn.prepareStatement(query)) {
			ResultSet rs = stmt.executeQuery();
			System.out.printf("%-15s %-15s %-15s %-20s\n", "Pizza Crust", "Base Size", "Profit", "Last Order Date");
			while (rs.next()) {
				String crust = rs.getString(1);
				String size = rs.getString(2);
				double profit = rs.getDouble(3);
				String date = rs.getString(4);
				System.out.printf("%-16s %-12s %7.2f \t\t\t %-10s\n",crust,size,profit,date);
			}
		}
		conn.close();
	}

	public static void printProfitByOrderType() throws SQLException, IOException {
		connect_to_db();
		/*
		 * Prints the ProfitByOrderType view. Remember that these views
		 * need to exist in your DB, so be sure you've run your createViews.sql
		 * files on your testing DB if you haven't already.
		 * 
		 * I'm not picky about how they print, just make sure it's readable.
		 */

		// DO NOT FORGET TO CLOSE YOUR CONNECTION
		String query = "SELECT * FROM ProfitByOrderType ORDER BY OrderType ASC;";
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(query);
		System.out.println("Profit by Order Type Report:");
		System.out.printf("%-10s %-10s %-15s %-15s %-10s\n","Order Type |", "Order Month |", "Total Order Price |", "Total Order Cost |", "Profit");
		while (rs.next()) {
			String orderType = rs.getString(1);
			String orderMonth = rs.getString(2);
			double price = rs.getDouble(3);
			double cost= rs.getDouble(4);
			double profit = rs.getDouble(5);
			System.out.printf("%-12s %-20s %-15s %-15s %-10s\n",orderType, orderMonth, price, cost, profit);
		}
		conn.close();
	}

}
