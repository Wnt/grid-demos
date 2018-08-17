package com.example.griddemo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.annotation.WebServlet;

import org.vaadin.patrik.FastNavigation;

import com.github.javafaker.Faker;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.data.Container;
import com.vaadin.data.Container.Indexed;
import com.vaadin.data.Item;
import com.vaadin.data.util.AbstractProperty;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.validator.EmailValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.HtmlRenderer;

import elemental.json.JsonValue;

/**
 * This UI is the application entry point. A UI may either represent a browser
 * window (or tab) or some part of a html page where a Vaadin application is
 * embedded.
 * <p>
 * The UI is initialized using {@link #init(VaadinRequest)}. This method is
 * intended to be overridden to add component to the user interface and
 * initialize non-component functionality.
 */
@Theme("mytheme")
public class MyUI extends UI {

	@Override
	protected void init(VaadinRequest vaadinRequest) {

		TabSheet tabSheet = new TabSheet();
		tabSheet.addComponents(createFastEditDemo(), createIconButtonDemo());
		tabSheet.setSizeFull();
		setContent(tabSheet);
	}

	private Component createIconButtonDemo() {
		Grid grid = createPersonGrid();

		grid.setColumns("department", "firstName", "lastName", "email");

		grid.getColumn("department").setRenderer(new HtmlRenderer() {
			@Override
			public JsonValue encode(String value) {

				return super.encode(
						"<img src=\"https://api.adorable.io/avatars/40/" + value + ".png\" title=\"" + value + "\"/>");
			}
		});

		grid.setEditorBuffered(false);

		grid.setSelectionMode(SelectionMode.MULTI);

		VerticalLayout layout = new VerticalLayout(grid, new Button("Remove", click -> {
			Collection<Object> selectedRows = grid.getSelectedRows();
			for (Object itemId : selectedRows) {
				// when item is removed from grid's container, Grid listens for ItemSetChange
				// events from container and will auto remove the rows from the UI
				grid.getContainerDataSource().removeItem(itemId);
			}
		}));
		layout.setCaption("iconButtonDemo");
		return layout;
	}

	private VerticalLayout createFastEditDemo() {
		Grid grid = createPersonGrid();
		grid.setColumns("orderNumber", "department", "firstName", "lastName", "email");

		((Container.Sortable) grid.getContainerDataSource()).sort(new String[] { "orderNumber" },
				new boolean[] { true });
		// apply the fastnavigation extension to the grid. enables cell editing without
		// double clicking the row
		new FastNavigation(grid, true, true);

		grid.setEditorEnabled(true);

		TextField orderNumberField = new TextField();
		grid.getColumn("orderNumber").setEditorField(orderNumberField);

		for (Column column : grid.getColumns()) {
			grid.getColumn(column.getPropertyId()).setSortable(false);
		}
		reNumberingInProgress = false;
		for (Object itemId : grid.getContainerDataSource().getItemIds()) {
			Item item = grid.getContainerDataSource().getItem(itemId);
			AbstractProperty property = (AbstractProperty) item.getItemProperty("orderNumber");
			property.addValueChangeListener(change -> {
				if (reNumberingInProgress) {
					return;
				}
				reNumberingInProgress = true;
				
				Notification.show("Value changed!");
				grid.sort("orderNumber");
				grid.clearSortOrder();
	
				int i = 1;
				Indexed container = grid.getContainerDataSource();
				for (Object itemId2 : container.getItemIds()) {
					System.out.println(((Person) itemId).getFirstName() + " :: " + ((Person) itemId2).getFirstName());
					container.getItem(itemId2).getItemProperty("orderNumber").setValue(i++);
				}
	
				grid.scrollTo(itemId);
				
				reNumberingInProgress = false;
			});
		}
//		grid.getEditorFieldGroup().addCommitHandler(new CommitHandler() {
//			
//			@Override
//			public void preCommit(CommitEvent commitEvent) throws CommitException {
//				// TODO Auto-generated method stub
//				
//			}
//			
//			@Override
//			public void postCommit(CommitEvent commitEvent) throws CommitException {
//				Notification.show("post commit");
//			}
//		});
//		grid.setEditorBuffered(true);
		// TODO make work without internal NPEs
//		orderNumberField.addValueChangeListener(change -> {
//			
//			Property propertyDataSource = orderNumberField.getPropertyDataSource();
//
//			Notification.show("Value changed!");
//			grid.sort("orderNumber");
//			grid.clearSortOrder();
//
//			Object editedItemId = grid.getEditedItemId();
//
//			if (editedItemId == null) {
//				return;
//			}
//
//			int i = 1;
//			Indexed container = grid.getContainerDataSource();
//			for (Object itemId : container.getItemIds()) {
//				if (editedItemId == itemId) {
//					i++;
//					continue;
//				}
//				container.getItem(itemId).getItemProperty("orderNumber").setValue(i++);
//			}
//
//			grid.scrollTo(editedItemId);
//		});

		ComboBox departmentField = new ComboBox();
		departmentField.addItems(Arrays.asList("Admin", "Services", "Marketing"));
		grid.getColumn("department").setEditorField(departmentField);

		((TextField) grid.getColumn("email").getEditorField()).addValidator(new EmailValidator("invalid email"));

		((TextField) grid.getColumn("firstName").getEditorField()).addValidator(
				new StringLengthValidator("name needs to be at least 3 characters", 3, Integer.MAX_VALUE, false));

		grid.setCellStyleGenerator(cell -> {
			if (cell.getPropertyId().equals("lastName") && (cell.getProperty().getValue() + "").length() > 6) {
				return "highlight";
			}
			return null;
		});
		VerticalLayout fastEditDemo = new VerticalLayout(grid, new Button("foo"));

		fastEditDemo.setCaption("Fast edit demo");
		return fastEditDemo;
	}

	private Grid createPersonGrid() {
		Grid grid = new Grid();

		BeanItemContainer<Person> data = new BeanItemContainer<>(Person.class);
		data.addAll(getPersons());

		grid.setContainerDataSource(data);
		grid.setSizeFull();
		return grid;
	}

	private ArrayList<Person> getPersons() {
		ArrayList<Person> persons = new ArrayList<>();
		for (int i = 1; i < 42; i++) {
			persons.add(new Person(i));
		}
		return persons;
	}

	public static Faker faker = new Faker();
	private boolean reNumberingInProgress;

	public class Person {
		String firstName = faker.name().firstName();
		String lastName = faker.name().lastName();
		String email = faker.internet().emailAddress(firstName.toLowerCase());
		private String department = faker.options().option("Admin", "Services", "Marketing");
		private boolean active = faker.bool().bool();
		private int orderNumber;

		public Person(int orderNumber) {
			this.orderNumber = orderNumber;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getDepartment() {
			return department;
		}

		public void setDepartment(String department) {
			this.department = department;
		}

		public boolean isActive() {
			return active;
		}

		public void setActive(boolean active) {
			this.active = active;
		}

		public int getOrderNumber() {
			return orderNumber;
		}

		public void setOrderNumber(int orderNumber) {
			this.orderNumber = orderNumber;
		}

	}

	@WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
	@VaadinServletConfiguration(ui = MyUI.class, productionMode = false)
	public static class MyUIServlet extends VaadinServlet {
	}
}
