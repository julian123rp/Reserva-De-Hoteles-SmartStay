package com.example.SmartStay.category;

import com.example.SmartStay.user.UserService;
import com.example.SmartStay.util.JwtUtil;
import com.example.SmartStay.websocket.WebSocketService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

    @Autowired
    private WebSocketService webSocketService;

    @GetMapping
    public ResponseEntity<List<Category>> allCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable ObjectId id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id).orElse(null));
    }

    @PostMapping("/add")
    public ResponseEntity<String> addCategory(@RequestBody CreateCategoryRequest category, @RequestHeader("Authorization") String token) {
        if (!userService.isAdmin(token)) {
            return new ResponseEntity<>("null", HttpStatus.UNAUTHORIZED);
        }

        // check if exists category with same name
        Category existingCategory = categoryService.getCategoryByName(category.getName()).orElse(null);
        if (existingCategory != null) {
            return new ResponseEntity<>("null", HttpStatus.CONFLICT);
        }

        Category newCategory = categoryService.save(new Category(category.getName(), category.getDescription(), category.getImage()));

        // send new category
        webSocketService.sendMessage("createCategory", List.of(newCategory.getId()));
        return new ResponseEntity<>(newCategory.getId(), HttpStatus.CREATED);
    }

    @PostMapping("/update")
    public ResponseEntity<Category> updateCategory(@RequestBody Category category, @RequestHeader("Authorization") String token) {
        if (!userService.isAdmin(token)) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }

        if (!ObjectId.isValid(category.getId())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Category categoryToUpdate = categoryService.getCategoryById(new ObjectId(category.getId())).orElse(null);
        if (categoryToUpdate == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        categoryToUpdate.setName(category.getName());
        categoryToUpdate.setDescription(category.getDescription());
        categoryToUpdate.setImage(category.getImage());
        categoryService.save(categoryToUpdate);

        // send updated category
        webSocketService.sendMessage("updateCategory", List.of(categoryToUpdate.getId()));
        return new ResponseEntity<>(categoryService.save(categoryToUpdate), HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable ObjectId id, @RequestHeader("Authorization") String token) {
        if (!userService.isAdmin(token)) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }

        Category category = categoryService.getCategoryById(id).orElse(null);
        if (category == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        categoryService.deleteCategory(id);

        // send deleted category
        webSocketService.sendMessage("deleteCategory", List.of(id.toString()));
        return new ResponseEntity<>(null, HttpStatus.OK);
    }
}