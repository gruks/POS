package com.example.pos.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import com.example.pos.model.MenuProduct;
import com.example.pos.service.MenuService;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

/**
 * MenuController for POS app.
 * - Uses category_id in items, keeps category_name for convenience.
 * - Provides category management (add / rename / delete) with checks.
 */
public class MenuController {

    // FXML controls
    @FXML private TextField searchField;
    @FXML private ChoiceBox<String> categoryFilter;
    @FXML private Button addBtn;
    @FXML private Button manageCategoriesBtn;

    @FXML private TableView<MenuProduct> table;
    @FXML private TableColumn<MenuProduct, String> nameCol;
    @FXML private TableColumn<MenuProduct, String> categoryCol;
    @FXML private TableColumn<MenuProduct, Number> priceCol;
    @FXML private TableColumn<MenuProduct, Number> qtyCol;
    @FXML private TableColumn<MenuProduct, MenuProduct> actionsCol;

    // Local state
    private final ObservableList<MenuProduct> data = FXCollections.observableArrayList();
    private final MenuService menuService = new MenuService();

    private final Map<Long, String> categoryMap = new HashMap<>(); // category_id -> name
    private final ObservableList<String> categoryNames = FXCollections.observableArrayList("All");

    // ----------------------- Constructors -----------------------

    /**
     * No-arg constructor required by JavaFX.
     */
    public MenuController() {
        // no-op: JavaFX will call initialize() after construction
    }

    // ----------------------- Lifecycle -----------------------

    @FXML
    private void initialize() {
        menuService.ensureDefaultCategoriesExist();
        reloadCategories();

        setupTable();
        setupFilters();
        setupActions();
        reloadFromDb();
    }

    // ----------------------- UI Setup -----------------------

    private void setupTable() {
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getName()));
        categoryCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCategory()));
        priceCol.setCellValueFactory(cd -> new SimpleDoubleProperty(cd.getValue().getPrice()));
        qtyCol.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getQuantity()));

        actionsCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button edit = new Button("Edit");
            private final Button del = new Button("Delete");
            private final HBox box = new HBox(8, edit, del);

            {
                edit.setOnAction(e -> openEditDialog(getTableRow().getItem()));
                del.setOnAction(e -> deleteItem(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(MenuProduct item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        table.setItems(data);
    }

    private void setupFilters() {
        categoryFilter.setItems(categoryNames);
        categoryFilter.setValue("All");
        categoryFilter.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> applyFilters());
        searchField.textProperty().addListener((o, a, b) -> applyFilters());
    }

    private void setupActions() {
        addBtn.setOnAction(e -> openAddDialog());
        if (manageCategoriesBtn != null) {
            manageCategoriesBtn.setOnAction(e -> openCategoryManager());
        }
    }

    // ----------------------- Data Loading & Filtering -----------------------

    private void reloadFromDb() {
        data.setAll(menuService.loadProducts());
        applyFilters();
    }

    private void applyFilters() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String cat = categoryFilter.getValue();
        table.setItems(data.filtered(p -> {
            boolean matchesText = q.isEmpty() || p.getName().toLowerCase().contains(q);
            boolean matchesCat = (cat == null || cat.equals("All")) || p.getCategory().equals(cat);
            return matchesText && matchesCat;
        }));
    }

    // ----------------------- Category Management -----------------------

    private void reloadCategories() {
        categoryMap.clear();
        categoryNames.clear();
        categoryNames.add("All");

        categoryMap.putAll(menuService.loadCategories());
        categoryNames.addAll(categoryMap.values());

        if (!categoryNames.contains(categoryFilter.getValue())) {
            categoryFilter.setValue("All");
        }
    }

    private void openCategoryManager() {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Manage Categories");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
    
        ListView<String> list = new ListView<>();
        list.setItems(FXCollections.observableArrayList(categoryNames).filtered(c -> !"All".equals(c)));
    
        TextField newCat = new TextField();
        newCat.setPromptText("New category name");
        Button add = new Button("Add");
        Button rename = new Button("Rename");
        Button del = new Button("Delete");
    
        // ✅ Add Category (instant refresh)
        add.setOnAction(e -> {
            String n = newCat.getText() == null ? "" : newCat.getText().trim();
            if (n.isEmpty()) return;
    
            menuService.ensureCategory(n);
            refreshCategoryUI(list);
            newCat.clear();
        });
    
        // ✅ Rename Category (already updates immediately)
        rename.setOnAction(e -> {
            String sel = list.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            TextInputDialog td = new TextInputDialog(sel);
            td.setHeaderText("Rename Category");
            td.showAndWait().ifPresent(newName -> {
                if (newName == null) return;
                newName = newName.trim();
                if (newName.isEmpty()) return;
    
                long catId = getCategoryIdByName(sel);
                if (catId == -1) {
                    return;
                }
                menuService.renameCategory(catId, newName);
                refreshCategoryUI(list);
                reloadFromDb();
            });
        });
    
        // ✅ Delete Category (instant refresh)
        del.setOnAction(e -> {
            String sel = list.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            long catId = getCategoryIdByName(sel);
            if (catId == -1) return;
    
            long used = menuService.countProductsInCategory(catId);
            if (used > 0) {
                new Alert(Alert.AlertType.WARNING,
                        "Cannot delete. " + used + " item(s) use this category.").showAndWait();
                return;
            }
    
            menuService.deleteCategory(catId);
    
            refreshCategoryUI(list);
        });
    
        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(8);
        g.addRow(0, new Label("Categories"));
        g.add(list, 0, 1, 4, 1);
        g.addRow(2, newCat, add, rename, del);
    
        dlg.getDialogPane().setContent(g);
        dlg.show();
    }    

    private void refreshCategoryUI(ListView<String> list) {
        reloadCategories();
        list.setItems(FXCollections.observableArrayList(categoryNames).filtered(c -> !"All".equals(c)));
        categoryFilter.setItems(categoryNames);
        categoryFilter.setValue("All");
    }

    // ----------------------- Item CRUD -----------------------

    private void openAddDialog() {
        Dialog<MenuProduct> dialog = buildEditorDialog(null);
        dialog.setTitle("Add Item");
        dialog.showAndWait().ifPresent(this::insertItem);
    }

    private void openEditDialog(MenuProduct toEdit) {
        if (toEdit == null) return;
        Dialog<MenuProduct> dialog = buildEditorDialog(toEdit);
        dialog.setTitle("Edit Item");
        dialog.showAndWait().ifPresent(updated -> updateItem(toEdit.getId(), updated));
    }

    private Dialog<MenuProduct> buildEditorDialog(MenuProduct initial) {
        reloadCategories();

        Dialog<MenuProduct> dialog = new Dialog<>();
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        TextField name = new TextField(initial == null ? "" : initial.getName());
        TextField price = new TextField(initial == null ? "" : String.valueOf(initial.getPrice()));
        TextField taxRate = new TextField(initial == null ? "5.0" : String.valueOf(initial.getTaxRate()));
        taxRate.setPromptText("Tax % (e.g., 5.0 for 5%)");
        ChoiceBox<String> category = new ChoiceBox<>(FXCollections.observableArrayList(categoryNames).filtered(c -> !"All".equals(c)));
        if (initial == null) category.setValue(category.getItems().isEmpty() ? null : category.getItems().get(0));
        else category.setValue(initial.getCategory());
        Spinner<Integer> qty = new Spinner<>(0, 1_000_000, initial == null ? 0 : initial.getQuantity());
        qty.setEditable(true);

        // Image chooser
        TextField imageField = new TextField(initial == null ? "" : (initial.getImageUrl() == null ? "" : initial.getImageUrl()));
        imageField.setPromptText("images/filename.png");
        Button browseImage = new Button("Choose Image");
        browseImage.setOnAction(e -> {
            javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
            chooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            File f = chooser.showOpenDialog(name.getScene().getWindow());
            if (f != null) {
                try {
                    String stored = storeImageToLocalFolder(f);
                    imageField.setText(stored);
                } catch (IOException ex) {
                    new Alert(Alert.AlertType.ERROR, "Failed to copy image: " + ex.getMessage()).showAndWait();
                }
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(8);
        grid.addRow(0, new Label("Name"), name);
        grid.addRow(1, new Label("Price"), price);
        grid.addRow(2, new Label("Tax Rate (%)"), taxRate);
        grid.addRow(3, new Label("Category"), category);
        grid.addRow(4, new Label("Quantity"), qty);
        grid.addRow(5, new Label("Image"), new HBox(8, imageField, browseImage));
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    double p = Double.parseDouble(price.getText().trim());
                    double tax = Double.parseDouble(taxRate.getText().trim());
                    String n = name.getText().trim();
                    String c = category.getValue();
                    int qv = qty.getValue();
                    if (n.isEmpty() || c == null) return null;
                    String img = imageField.getText() == null ? null : imageField.getText().trim();
                    long catId = getCategoryIdByName(c);
                    return new MenuProduct(initial == null ? 0 : initial.getId(), n, p, c, catId, qv, null, img, tax);
                } catch (NumberFormatException ex) {
                    new Alert(Alert.AlertType.ERROR, "Invalid price or tax rate").showAndWait();
                    return null;
                }
            }
            return null;
        });

        return dialog;
    }

    private void insertItem(MenuProduct p) {
        if (p == null) return;
        MenuProduct created = menuService.createProduct(p);
        if (created != null) {
            data.add(created);
            applyFilters();
        }
    }

    private void updateItem(long id, MenuProduct updated) {
        if (updated == null) return;
        updated.setId(id);
        menuService.updateProduct(updated);
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getId() == id) {
                data.set(i, updated);
                break;
            }
        }
        applyFilters();
    }

    private void deleteItem(MenuProduct p) {
        if (p == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete '" + p.getName() + "'?", ButtonType.CANCEL, ButtonType.OK);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                menuService.deleteProduct(p.getId());
                data.remove(p);
                applyFilters();
            }
        });
    }

    // ----------------------- Utilities -----------------------

    private long getCategoryIdByName(String name) {
        if (name == null) return -1;
        for (Map.Entry<Long, String> e : categoryMap.entrySet()) {
            if (e.getValue().equals(name)) {
                return e.getKey();
            }
        }
        long id = menuService.ensureCategory(name);
        reloadCategories();
        return id;
    }

    private String storeImageToLocalFolder(File source) throws IOException {
        File imagesDir = new File("images");
        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
        }
        String ext = "";
        String name = source.getName();
        int i = name.lastIndexOf('.');
        if (i > 0) ext = name.substring(i);
        String safeName = java.util.UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = imagesDir.toPath().resolve(safeName);
        Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        return "images/" + safeName; // relative path to app working dir
    }
}
