package com.example.griddemo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.servlet.annotation.WebServlet;

import org.vaadin.patrik.FastNavigation;

import com.github.javafaker.Faker;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.contextmenu.GridContextMenu;
import com.vaadin.data.Binder.Binding;
import com.vaadin.data.Binder.BindingBuilder;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.validator.EmailValidator;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.components.grid.GridRowDragger;
import com.vaadin.ui.renderers.ButtonRenderer;
import com.vaadin.ui.renderers.ImageRenderer;
import com.vaadin.ui.renderers.TextRenderer;

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

	private GridRowDragger<Person> gridRowDragger;
	private Binding<Person, Integer> orderBinding;

	@Override
	protected void init(VaadinRequest vaadinRequest) {

		TabSheet tabSheet = new TabSheet();
		tabSheet.addComponents(createFastEditDemo(), createIconButtonDemo());
		tabSheet.setSizeFull();
		setContent(tabSheet);
	}

	private Component createIconButtonDemo() {
		Grid<Person> grid = createPersonGrid();

		grid.setDetailsGenerator(person -> {
			HorizontalLayout editorRoot = new HorizontalLayout();

			ComboBox<String> comboBox = new ComboBox<>();
			comboBox.setItems(Arrays.asList("foo", "bar", "baz"));
			comboBox.addValueChangeListener(change -> {
				String selectedValue = comboBox.getValue();
				editorRoot.addComponent(new Label("# " + selectedValue));
				person.data.add(selectedValue);
				grid.getDataProvider().refreshItem(person);
			});
			editorRoot.addComponent(comboBox);
			return editorRoot;
		});
		grid.addColumn(p -> "Edit", new ButtonRenderer<>(click -> {
			
			grid.setDetailsVisible(click.getItem(), !grid.isDetailsVisible(click.getItem()));
		})).setId("tools");
		grid.addColumn(p -> p.getData().toString()).setId("inline");
		grid.setColumns("firstName", "lastName", "email", "department", "inline", "tools");

		grid.setCaption("Details demo");
		return grid;
	}

	private Component createFastEditDemo() {
		Grid<Person> grid = createPersonGrid();
		grid.getColumns().forEach(column -> column.setSortable(false));
		
		gridRowDragger = new GridRowDragger<>(grid);
		
		UI.getCurrent().setMobileHtml5DndEnabled(true);

		FastNavigation<Person> fastNavigation = new FastNavigation<>(grid, true, true);
		

		VerticalLayout verticalLayout = new VerticalLayout(grid, new Button("remove", click -> {
			ListDataProvider<Person> dp = (ListDataProvider<Person>) grid.getDataProvider();
			Collection<Person> items = dp.getItems();
			if (grid.getSelectedItems() != null && !grid.getSelectedItems().isEmpty()) {
				items.removeAll(grid.getSelectedItems());
			}
			dp.refreshAll();
		}));
		verticalLayout.setExpandRatio(grid, 1);
		verticalLayout.setCaption("fast navigation");
		verticalLayout.setSizeFull();

		grid.setSelectionMode(SelectionMode.MULTI);

		// grid.getEditor().setBuffered(false);
		grid.getEditor().setEnabled(true);

		grid.getColumn("firstName").setEditorComponent(new TextField());

		grid.getColumn("lastName").setEditorComponent(new TextField());

		TextField emailField = new TextField();
		grid.getColumn("email").setEditorComponent(emailField);

		BindingBuilder<Person, String> withValidator = grid.getEditor().getBinder().forField(emailField)
				.withValidator(new EmailValidator("Invalid email"));
		Binding<Person, String> bind = withValidator.bind(Person::getEmail, Person::setEmail);
		grid.getColumn("email").setEditorBinding(bind);

		ComboBox<Object> comboBox = new ComboBox<>();
		comboBox.setItems(Arrays.asList("Admin", "Services", "Marketing"));
		grid.getColumn("department").setEditorComponent(comboBox);

		grid.addColumn(person -> {
			return new ExternalResource("https://api.adorable.io/avatars/40/" + person.getDepartment() + ".png");
		}, new ImageRenderer()).setCaption("Generated column").setId("gc");

		grid.setDescriptionGenerator(p -> {
			return "<strong>" + p.getFirstName() + "</strong>" + " " + p.getLastName();
		}, ContentMode.HTML);

		TextField orderField = new TextField();
		orderField.setValueChangeMode(ValueChangeMode.BLUR);
		orderBinding = grid.getEditor().getBinder()

				.forField(orderField)

				.withConverter(new StringToIntegerConverter("invalid number"))

				.bind(person -> {
					List<Person> items = (List<Person>) ((ListDataProvider) grid.getDataProvider()).getItems();

					// use 1-based indices in the UI
					return items.indexOf(person) + 1;

				}, (person, newValue) -> {
					// use 0-based indices in the array
					newValue--;
					if (newValue < 0) {
						newValue = 0;
					}
					ListDataProvider dataProvider = (ListDataProvider) grid.getDataProvider();
					List<Person> items = (List<Person>) dataProvider.getItems();
					if (newValue > items.size() - 1) {
						newValue = items.size() - 1;
					}

					int originalIndex = items.indexOf(person);
					items.remove(person);
					items.add(newValue, person);
					dataProvider.refreshAll();
					grid.getEditor().cancel();
					grid.scrollTo(newValue);
					// TODO re-order list
				});
		grid.addColumn(person -> {
			List<Person> items = (List<Person>) ((ListDataProvider) grid.getDataProvider()).getItems();

			// use 1-based indices in the UI
			return items.indexOf(person) + 1;

		}, new TextRenderer()).setCaption("#").setId("#").setEditorBinding(orderBinding);

		// Example on how to theme even & odd rows
		// grid.addStyleName("custom-color-grid");

		// Example on how to theme based on row content
		// grid.setStyleGenerator(item -> {
		// if(item.lastName.length()>6) {
		// return "highlight";
		// }
		// return null;
		// });
		grid.setColumns("#", "gc", "firstName", "lastName", "email", "department");

		grid.getColumn("#").setHidden(true);
		grid.getColumn("#").setHidable(true);
		grid.getColumn("department").setHidden(true);
		grid.getColumn("department").setHidable(true);
		GridContextMenu<Person> contextMenu = new GridContextMenu<>(grid);
		contextMenu.addGridBodyContextMenuListener(e -> {
			contextMenu.removeItems();
			contextMenu.addItem("Remove", select -> {
				ListDataProvider<Person> dp = ((ListDataProvider<Person>) grid.getDataProvider());
				dp.getItems().remove(e.getItem());
				dp.refreshAll();
			});
			grid.getEditor().cancel();
		});

		return verticalLayout;
	}

	private Grid<Person> createPersonGrid() {
		Grid<Person> grid = new Grid<Person>(Person.class);

		grid.setColumns("firstName", "lastName", "email", "department");

		grid.setItems(getPersons());

		// ListDataProvider<Person> dataProvider = new ListDataProvider<>(getPersons());
		//
		// grid.setDataProvider(dataProvider);

		grid.setSizeFull();
		return grid;
	}

	private ArrayList<Person> getPersons() {
		ArrayList<Person> persons = new ArrayList<>();
		for (int i = 1; i < 420; i++) {
			persons.add(new Person(i));
		}
		return persons;
	}

	public static Faker faker = new Faker();

	public class Person {
		String firstName = faker.name().firstName();
		String lastName = faker.name().lastName();
		String email = faker.internet().emailAddress(firstName.toLowerCase());
		private String department = faker.options().option("Admin", "Services", "Marketing");
		private boolean active = faker.bool().bool();
		private int orderNumber;
		private List<String> data = new ArrayList<>();

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

		public List<String> getData() {
			return data;
		}

		public void setData(List<String> data) {
			this.data = data;
		}

	}

	@WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
	@VaadinServletConfiguration(ui = MyUI.class, productionMode = false)
	public static class MyUIServlet extends VaadinServlet {
	}
}
