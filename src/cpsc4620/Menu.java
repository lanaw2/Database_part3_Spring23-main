package cpsc4620;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import init.DBIniter;
import java.text.*;

/*
 * This file is where the front end magic happens.
 * 
 * You will have to write the functionality of each of these menu options' respective functions.
 * 
 * This file should need to access your DB at all, it should make calls to the DBNinja that will do all the connections.
 * 
 * You can add and remove functions as you see necessary. But you MUST have all 8 menu functions (9 including exit)
 * 
 * Simply removing menu functions because you don't know how to implement it will result in a major error penalty (akin to your program crashing)
 * 
 * Speaking of crashing. Your program shouldn't do it. Use exceptions, or if statements, or whatever it is you need to do to keep your program from breaking.
 * 
 * 
 */

public class Menu {
	public static void main(String[] args) throws SQLException, IOException, ParseException {
		System.out.println("Welcome to Taylor's Pizzeria!");
		
		int menu_option = 0;
		DBConnector.make_connection();
		// present a menu of options and take their selection
		
		PrintMenu();
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		DBIniter.init();
		String option = reader.readLine();
		menu_option = Integer.parseInt(option);

		while (menu_option != 9) {
			switch (menu_option) {
			case 1:// enter order
				EnterOrder();
				break;
			case 2:// view customers
				viewCustomers();
				break;
			case 3:// enter customer
				EnterCustomer();
				break;
			case 4:// view order
				// open/closed/date
				ViewOrders();
				break;
			case 5:// mark order as complete
				MarkOrderAsComplete();
				break;
			case 6:// view inventory levels
				ViewInventoryLevels();
				break;
			case 7:// add to inventory
				AddInventory();
				break;
			case 8:// view reports
				PrintReports();
				break;
			}
			PrintMenu();
			option = reader.readLine();
			menu_option = Integer.parseInt(option);
		}

	}

	public static void PrintMenu() {
		System.out.println("\n\nPlease enter a menu option:");
		System.out.println("1. Enter a new order");
		System.out.println("2. View Customers ");
		System.out.println("3. Enter a new Customer ");
		System.out.println("4. View orders");
		System.out.println("5. Mark an order as completed");
		System.out.println("6. View Inventory Levels");
		System.out.println("7. Add Inventory");
		System.out.println("8. View Reports");
		System.out.println("9. Exit\n\n");
		System.out.println("Enter your option: ");
	}

	// allow for a new order to be placed
	public static void EnterOrder() throws SQLException, IOException 
	{
		/*
		 * EnterOrder should do the following:
		 * Ask if the order is for an existing customer -> If yes, select the customer. If no -> create the customer (as if the menu option 2 was selected).
		 * Ask if the order is delivery, pickup, or dinein (ask for orderType specific information when needed)
		 * Build the pizza (there's a function for this)
		 * ask if more pizzas should be be created. if yes, go back to building your pizza.
		 * Apply order discounts as needed (including to the DB)
		 * apply the pizza to the order (including to the DB)
		 * return to menu
		 */

		Integer CustID;
		// Get next order number
		int orderNumber;
		if(DBNinja.getCurrentOrders().size()==0){
			orderNumber=1;
		}else{
			orderNumber = DBNinja.getNextOrderID();
		}
		// Get customer information
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Is this order for an existing customer? Y or N");
		String isCustomer = reader.readLine();
		if(isCustomer.toUpperCase().equals("Y")){
			viewCustomers();
			System.out.println("Enter the customer's ID Number:");
			CustID = Integer.parseInt(reader.readLine());
		}else{
			EnterCustomer();
			// Get newly added customerID
			Integer num_customers = DBNinja.getCustomerList().size();
			CustID = num_customers;
		}
		// Order Type
		System.out.println("Is this order for delivery(1), pickup(2), or dine-in(3)?");
		int orderType = Integer.parseInt(reader.readLine());
		String address = null;
		Integer table_num = null;
		if(orderType==1){
			System.out.println("Enter the delivery address for this order:");
			address= reader.readLine();
		}else if(orderType==3){
			System.out.println("Enter the table number for this order:");
			table_num = Integer.parseInt(reader.readLine());
		}

		// Add Pizza
		boolean add_more_pizza = true;
		double orderBusPrice = 0;
		double orderCustPrice = 0;
		ArrayList<Pizza> pizzaList = new ArrayList<>();
		while(add_more_pizza){
			Pizza new_pizza = buildPizza(orderNumber);
			pizzaList.add(new_pizza);
			orderBusPrice += new_pizza.getBusPrice();
			orderCustPrice += new_pizza.getCustPrice();
			System.out.println("Enter -1 to stop adding pizzas... Enter anything else to continue adding pizzas:");
			Integer more_pizza = Integer.parseInt(reader.readLine());
			if(more_pizza==-1){
				add_more_pizza=false;
			}
		}

		// Add discounts to order
		System.out.println("Would you like to add any discounts to this order? Y/N");
		String add_discount = reader.readLine();
		ArrayList<Discount> discountList = new ArrayList<>();
		if(add_discount.equals("Y")){
			boolean add_more_discount = true;
			while(add_more_discount){
				ArrayList<Discount> dList = DBNinja.getDiscountList();
				for(Discount d:dList){
					System.out.println(d.toString());
				}
				System.out.println("Which order discount would you like to add? Enter the DiscountID. Enter -1 to stop adding discounts.");
				Integer discount_id = Integer.parseInt(reader.readLine());
				if(discount_id==-1){
					break;
				}
				// Add selected discount to discount list
				for(Discount d:dList){
					if(d.getDiscountID()==discount_id){
						discountList.add(d);
					}
				}

			}
		}

		// Timestamp for order
		SimpleDateFormat formatter = new SimpleDateFormat("YYYY-MM-DD HH:mm:ss");
		Date date = new Date();

		// Create new order
		Order o = null;
		if(orderType==1){
			o = new DeliveryOrder(orderNumber,CustID,formatter.format(date),orderCustPrice,orderBusPrice,0,address);
			o.setOrderType(DBNinja.delivery);
		}else if(orderType==2){
			o = new PickupOrder(orderNumber,CustID, formatter.format(date), orderCustPrice,orderBusPrice,0,0);
			o.setOrderType(DBNinja.pickup);
		}else{
			o = new DineinOrder(orderNumber,CustID,formatter.format(date),orderCustPrice,orderBusPrice,0,table_num);
			o.setOrderType(DBNinja.dine_in);
		}
		// Add pizzas and discounts to order
		o.setPizzaList(pizzaList);
		o.setDiscountList(discountList);

		// Add Orders/Discounts to database
		// Add Discounts to orders
		for(Discount d: discountList){
			DBNinja.useOrderDiscount(o,d);
			o.addDiscount(d);
		}
		// Add order to database
		DBNinja.addOrder(o);
		System.out.println("Finished adding order...Returning to menu...");
	}
	
	
	public static void viewCustomers() throws SQLException, IOException {
		/*
		 * Simply print out all the customers from the database.
		 */
		ArrayList<Customer> customerList = DBNinja.getCustomerList();
		for(Customer cus:customerList){
			System.out.println(cus.toString());
		}
	}

	// Enter a new customer in the database
	public static void EnterCustomer() throws SQLException, IOException 
	{
		/*
		 * Ask what the name of the customer is. YOU MUST TELL ME (the grader) HOW TO FORMAT THE FIRST NAME, LAST NAME, AND PHONE NUMBER.
		 * If you ask for first and last name one at a time, tell me to insert First name <enter> Last Name (or separate them by different print statements)
		 * If you want them in the same line, tell me (First Name <space> Last Name).
		 * 
		 * same with phone number. If there's hyphens, tell me XXX-XXX-XXXX. For spaces, XXX XXX XXXX. For nothing XXXXXXXXXXXX.
		 * 
		 * I don't care what the format is as long as you tell me what it is, but if I have to guess what your input is I will not be a happy grader
		 * 
		 * Once you get the name and phone number (and anything else your design might have) add it to the DB
		 */
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		int cus_id = DBNinja.getCustomerList().size()+1;
		System.out.println("Please enter the customer's first name:");
		String f_name = reader.readLine();
		System.out.println("Please enter the customer's last name:");
		String l_name = reader.readLine();
		System.out.println("Please enter the customer's phone number (Formatted XXX-XXX-XXXX):");
		String phone_number = reader.readLine();
		Customer c = new Customer(cus_id,f_name,l_name,phone_number);
		DBNinja.addCustomer(c);
	}

	// View any orders that are not marked as completed
	public static void ViewOrders() throws SQLException, IOException, ParseException {
	/*
	 * This should be subdivided into two options: print all orders (using simplified view) and print all orders (using simplified view) since a specific date.
	 * 
	 * Once you print the orders (using either sub option) you should then ask which order I want to see in detail
	 * 
	 * When I enter the order, print out all the information about that order, not just the simplified view.
	 * 
	 */
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Would you like to...\n(1) Display all orders\n(2) Display all orders since a specific date");
		int option = Integer.parseInt(reader.readLine());
		ArrayList<Order> order_list = DBNinja.getCurrentOrders();
		if(option==1){
			for(Order o: order_list){
				System.out.println(o.toSimplePrint());
			}
			System.out.println("Which order would you like to see in detail? Enter a number.");
			int order_num = Integer.parseInt(reader.readLine());
			for(Order o: order_list){
				if(o.getOrderID()==order_num){
					System.out.println(o.toString());
				}
			}
		}else if(option==2){
			//object of SimpleDateFormat class
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			System.out.println("What date would you like to restrict by? (format YYYY-MM-DD)");
			Date input_date = sdf.parse(reader.readLine());
			for(Order o: order_list){
				String order_date = o.getDate();
				Date o_date = sdf.parse(order_date.substring(0,10));
				// if order date is the same or after input date
				if(o_date.compareTo(input_date)>=0){
					System.out.println(o.toSimplePrint());
				}
			}
			System.out.println("Which order would you like to see in detail? Enter a number.");
			int order_num = Integer.parseInt(reader.readLine());
			for(Order o: order_list){
				if(o.getOrderID()==order_num){
					System.out.println(o.toString());
				}
			}
		}
	}

	
	// When an order is completed, we need to make sure it is marked as complete
	public static void MarkOrderAsComplete() throws SQLException, IOException 
	{
		/*All orders that are created through java (part 3, not the 7 orders from part 2) should start as incomplete
		 * 
		 * When this function is called, you should print all of the orders marked as complete 
		 * and allow the user to choose which of the incomplete orders they wish to mark as complete
		 * 
		 */
		ArrayList<Order> order_list = DBNinja.getCurrentOrders();
		for(Order o: order_list){
			if(o.getIsComplete()==0){
				System.out.println(o.toSimplePrint());
			}
		}
		System.out.println("Which order would you like to mark as complete? Enter the Order ID");
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		Integer order_num = Integer.parseInt(reader.readLine());
		for(Order o: order_list){
			if(o.getOrderID()==order_num){
				o.setIsComplete(1);
				DBNinja.CompleteOrder(o);
			}
		}
		System.out.println("Order "+order_num+" has been marked as complete");
	}

	// See the list of inventory and it's current level
	public static void ViewInventoryLevels() throws SQLException, IOException 
	{
		//print the inventory. I am really just concerned with the ID, the name, and the current inventory
		DBNinja.printInventory();
	}

	// Select an inventory item and add more to the inventory level to re-stock the
	// inventory
	public static void AddInventory() throws SQLException, IOException 
	{
		/*
		 * This should print the current inventory and then ask the user which topping they want to add more to and how much to add
		 */
		DBNinja.printInventory();
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("Which topping would you like to add more to? Enter Topping ID Number:\n");
		Integer top_id = Integer.parseInt(reader.readLine());
		System.out.println("How much more would you like to add?");
		Integer top_amt = Integer.parseInt(reader.readLine());
		ArrayList<Topping> topping_list = DBNinja.getInventory();
		for(Topping t: topping_list){
			if(t.getTopID()==top_id){
				DBNinja.AddToInventory(t,top_amt);
			}
		}
	}

	// A function that builds a pizza. Used in our add new order function
	public static Pizza buildPizza(int orderID) throws SQLException, IOException
	{
		/*
		 * This is a helper function for first menu option.
		 * It should ask which size pizza the user wants and the crustType.
		 * Once the pizza is created, it should be added to the DB.
		 * We also need to add toppings to the pizza. (Which means we not only need to add toppings here, but also our bridge table)
		 * We then need to add pizza discounts (again, to here and to the database)
		 * Once the discounts are added, we can return the pizza
		 */
		System.out.println("Let's print a pizza!");
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("What size is this pizza?\n(1) Small\n(2) Medium\n(3) Large\n(4) X-Large\nEnter the corresponding number:");
		Integer size = Integer.parseInt(reader.readLine());
		System.out.println("What crust for this pizza?\n(1) Thin\n(2) Original\n(3) Pan\n(4) Gluten-Free\nEnter the corresponding number:");
		Integer crust = Integer.parseInt(reader.readLine());
		String size_str=null;
		switch(size){
			case 1:
				size_str = DBNinja.size_s;
			case 2:
				size_str = DBNinja.size_m;
			case 3:
				size_str = DBNinja.size_l;
			case 4:
				size_str = DBNinja.size_xl;
		}

		String crust_str=null;
		switch(crust){
			case 1:
				crust_str = DBNinja.crust_thin;
			case 2:
				crust_str = DBNinja.crust_orig;
			case 3:
				crust_str = DBNinja.crust_pan;
			case 4:
				crust_str = DBNinja.crust_gf;
		}
		double BusPrice = DBNinja.getBaseBusPrice(size_str,crust_str);
		double CustPrice = DBNinja.getBaseCustPrice(size_str,crust_str);
		SimpleDateFormat formatter = new SimpleDateFormat("YYYY-MM-DD HH:mm:ss");
		Date date = new Date();
		Pizza p = new Pizza(DBNinja.getMaxPizzaID()+1,size_str,crust_str,orderID,"Completed",date.toString(),BusPrice,CustPrice);
		boolean add_topp = true;
		ArrayList<Topping> all_toppings = DBNinja.getInventory();
		int count=0;
		while(add_topp) {
			System.out.println("Printing current topping list...");
			DBNinja.printInventory();
			System.out.println("Which topping would you like to add? Enter Topping ID Number, Enter -1 to stop adding toppings:");
			Integer top_ID = Integer.parseInt(reader.readLine());
			if (top_ID == -1) {
				add_topp = false;
				break;
			}
			System.out.println("Would you like to add double? Y/N");
			String isDouble = reader.readLine();
			boolean doubleAmt;
			if (isDouble.toUpperCase().equals("Y")) {
				doubleAmt = true;
			}else{
				doubleAmt = false;
			}
			for(Topping t: all_toppings){
				if(t.getTopID()==top_ID){
					p.addToppings(t,doubleAmt);
					p.modifyDoubledArray(count,doubleAmt);
				}
			}
			count++;
		}
		System.out.println("Would you like to add any discounts to this pizza? Y/N");
		String add_discount = reader.readLine();
		if(add_discount.equals("Y")){
			boolean add_more_discount = true;
			while(add_more_discount){
				ArrayList<Discount> dList = DBNinja.getDiscountList();
				for(Discount d:dList){
					System.out.println(d.toString());
				}
				System.out.println("Which order discount would you like to add? Enter the DiscountID. Enter -1 to stop adding discounts.");
				Integer discount_id = Integer.parseInt(reader.readLine());
				if(discount_id==-1){
					break;
				}
				// Add selected discount to discount list
				for(Discount d:dList){
					if(d.getDiscountID()==discount_id){
						p.addDiscounts(d);

					}
				}

			}
		}
		Pizza ret = p;
		DBNinja.addPizza(ret);
		for(Discount d: ret.getDiscounts()){
			DBNinja.usePizzaDiscount(p,d);
		}
		count=0;
		for(Topping t: ret.getToppings()){
			DBNinja.useTopping(p,t,ret.getIsDoubleArray()[count]);
			count++;
		}

		return ret;
	}
	
	private static int getTopIndexFromList(int TopID, ArrayList<Topping> tops)
	{
		/*
		 * This is a helper function I used to get a topping index from a list of toppings
		 * It's very possible you never need to use a function like this
		 * 
		 */
		int ret = -1;
		
		
		
		return ret;
	}
	
	
	public static void PrintReports() throws SQLException, NumberFormatException, IOException
	{
		/*
		 * This function calls the DBNinja functions to print the three reports.
		 * 
		 * You should ask the user which report to print
		 */
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Which report would you like to print? Enter a number: \n(1) ToppingPopularity\n(2) ProfitByPizza\n(3) ProfitByOrderType");
		Integer report_choice = Integer.parseInt(reader.readLine());
		if(report_choice==1){
			DBNinja.printToppingPopReport();
		}else if(report_choice==2){
			DBNinja.printProfitByPizzaReport();
		}else if(report_choice==3){
			DBNinja.printProfitByOrderType();
		}
	}

}


//Prompt - NO CODE SHOULD TAKE PLACE BELOW THIS LINE
//DO NOT EDIT ANYTHING BELOW HERE, I NEED IT FOR MY TESTING DIRECTORY. IF YOU EDIT SOMETHING BELOW, IT BREAKS MY TESTER WHICH MEANS YOU DO NOT GET GRADED (0)

/*
CPSC 4620 Project: Part 3 â€“ Java Application Due: Thursday 11/30 @ 11:59 pm 125 pts

For this part of the project you will complete an application that will interact with your database. Much of the code is already completed, you will just need to handle the functionality to retrieve information from your database and save information to the database.
Note, this program does little to no verification of the input that is entered in the interface or passed to the objects in constructors or setters. This means that any junk data you enter will not be caught and will propagate to your database, if it does not cause an exception. Be careful with the data you enter! In the real world, this program would be much more robust and much more complex.

Program Requirements:

Add a new order to the database: You must be able to add a new order with pizzas to the database. The user interface is there to handle all of the input, and it creates the Order object in the code. It then calls DBNinja.addOrder(order) to save the order to the database. You will need to complete addOrder. Remember addOrder will include adding the order as well as the pizzas and their toppings. Since you are adding a new order, the inventory level for any toppings used will need to be updated. You need to check to see if there is inventory available for each topping as it is added to the pizza. You can not let the inventory level go negative for this project. To complete this operation, DBNinja must also be able to return a list of the available toppings and the list of known customers, both of which must be ordered appropropriately.

View Customers: This option will display each customer and their associated information. The customer information must be ordered by last name, first name and phone number. The user interface exists for this, it just needs the functionality in DBNinja

Enter a new customer: The program must be able to add the information for a new customer in the database. Again, the user interface for this exists, and it creates the Customer object and passes it to DBNinja to be saved to the database. You need to write the code to add this customer to the database. You do need to edit the prompt for the user interface in Menu.java to specify the format for the phone number, to make sure it matches the format in your database.

View orders: The program must be able to display orders and be sorted by order date/time from most recent to oldest. The program should be able to display open orders, all the completed orders or just the completed order since a specific date (inclusive) The user interface exists for this, it just needs the functionality in DBNinja

Mark an order as completed: Once the kitchen has finished prepping an order, they need to be able to mark it as completed. When an order is marked as completed, all of the pizzas should be marked as completed in the database. Open orders should be sorted as described above for option #4. Again, the user interface exists for this, it just needs the functionality in DBNinja

View Inventory Levels: This option will display each topping and its current inventory level. The toppings should be sorted in alphabetical order. Again, the user interface exists for this, it just needs the functionality in DBNinja

Add Inventory: When the inventory level of an item runs low, the restaurant will restock that item. When they do so, they need to enter into the inventory how much of that item was added. They will select a topping and then say how many units were added. Note: this is not creating a new topping, just updating the inventory level. Make sure that the inventory list is sorted as described in option #6. Again, the user interface exists for this, it just needs the functionality in DBNinja

View Reports: The program must be able to run the 3 profitability reports using the views you created in Part 2. Again, the user interface exists for this, it just needs the functionality in DBNinja

Modify the package DBConnector to contain your database connection information, this is the same information you use to connect to the database via MySQL Workbench. You will use DBNinja.connect_to_db to open a connection to the database. Be aware of how many open database connections you make and make sure the database is properly closed!
Your code needs to be secure, so any time you are adding any sort of parameter to your query that is a String, you need to use PreparedStatements to prevent against SQL injections attacks. If your query does not involve any parameters, or if your queries parameters are not coming from a String variable, then you can use a regular Statement instead.

The Files: Start by downloading the starter code files from Canvas. You will see that the user interface and the java interfaces and classes that you need for the assignment are already completed. Review all these files to familiarize yourself with them. They contain comments with instructions for what to complete. You should not need to change the user interface except to change prompts to the user to specify data formats (i.e. dashes in phone number) so it matches your database. You also should not need to change the entity object code, unless you want to remove any ID fields that you did not add to your database.

You could also leave the ID fields in place and just ignore them. If you have any data types that donâ€™t match (i.e. string size options as integers instead of strings), make the conversion when you pull the information from the database or add it to the database. You need to handle data type differences at that time anyway, so it makes sense to do it then instead of making changes to all of the files to handle the different data type or format.

The Menu.java class contains the actual user interface. This code will present the user with a menu of options, gather the necessary inputs, create the objects, and call the necessary functions in DBNinja. Again, you will not need to make changes to this file except to change the prompt to tell me what format you expect the phone number in (with or without dashes).

There is also a static class called DBNinja. This will be the actual class that connects to the database. This is where most of the work will be done. You will need to complete the methods to accomplish the tasks specified.

Also in DBNinja, there are several public static strings for different crusts, sizes and order types. By defining these in one place and always using those strings we can ensure consistency in our data and in our comparisons. You donâ€™t want to have â€œSMALLâ€� â€œsmallâ€� â€œSmallâ€� and â€œPersonalâ€� in your database so it is important to stay consistent. These strings will help with that. You can change what these strings say in DBNinja to match your database, as all other code refers to these public static strings.

Start by changing the class attributes in DBConnector that contain the data to connect to the database. You will need to provide your database name, username and password. All of this is available is available in the Chapter 15 lecture materials. Once you have that done, you can begin to build the functions that will interact with the database.

The methods you need to complete are already defined in the DBNinja class and are called by Menu.java, they just need the code. Two functions are completed (getInventory and getTopping), although for a different database design, and are included to show an example of connecting and using a database. You will need to make changes to these methods to get them to work for your database.

Several extra functions are suggested in the DBNinja class. Their functionality will be needed in other methods. By separating them out you can keep your code modular and reduce repeated code. I recommend completing your code with these small individual methods and queries. There are also additional methods suggested in the comments, but without the method template that could be helpful for your program. HINT, make sure you test your SQL queries in MySQL Workbench BEFORE implementing them in codeâ€¦it will save you a lot of debugging time!

If the code in the DBNinja class is completed correctly, then the program should function as intended. Make sure to TEST, to ensure your code works! Remember that you will need to include the MySQL JDBC libraries when building this application. Otherwise you will NOT be able to connect to your database.

Compiling and running your code: The starter code that will compile and â€œrunâ€�, but it will not do anything useful without your additions. Because so much code is being provided, there is no excuse for submitting code that does not compile. Code that does not compile and run will receive a 0, even if the issue is minor and easy to correct.

Help: Use MS Teams to ask questions. Do not wait until the last day to ask questions or get started!

Submission You will submit your assignment on Canvas. Your submission must include: â€¢ Updated DB scripts from Part 2 (all 5 scripts, in a folder, even if some of them are unchanged). â€¢ All of the class code files along with a README file identifying which class files in the starter code you changed. Include the README even if it says â€œI have no special instructions to shareâ€�. â€¢ Zip the DB Scripts, the class files (i.e. the application), and the README file(s) into one compressed ZIP file. No other formats will be accepted. Do not submit the lib directory or an IntellJ or other IDE project, just the code.

Testing your submission Your project will be tested by replacing your DBconnector class with one that connects to a special test server. Then your final SQL files will be run to recreate your database and populate the tables with data. The Java application will then be built with the new DBconnector class and tested.

No late submissions will be accepted for this assignment.*/

