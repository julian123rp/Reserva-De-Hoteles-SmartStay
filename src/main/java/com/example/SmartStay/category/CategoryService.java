package com.example.SmartStay.category;

import com.example.SmartStay.product.Product;
import com.example.SmartStay.websocket.WebSocketService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    WebSocketService webSocketService;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> getCategoryById(ObjectId id) {
        return categoryRepository.findById(id);
    }

    public Category save(Category category) {
        return categoryRepository.save(category);
    }

    public Category getCategoryByProductId(ObjectId productId) {
        return categoryRepository.findByProductsContaining(productId.toString());
    }

    public Category getCategoryByProduct(Product product) {
        return categoryRepository.findByProductsContaining(product.getId());
    }

    public void removeProductFromCategory(ObjectId categoryId, ObjectId productId) {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category == null) {
            return;
        }
        category.getProducts().remove(productId.toHexString());
        categoryRepository.save(category);
        webSocketService.sendMessage("updateCategory", List.of(category.getId()));
    }

    public void removeProductFromCategory(Category category, ObjectId productId) {
        category.getProducts().remove(productId.toHexString());
        categoryRepository.save(category);
        webSocketService.sendMessage("updateCategory", List.of(category.getId()));
    }

    public void addProductToCategory(ObjectId categoryId, ObjectId productId) {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category == null) {
            return;
        }
        category.getProducts().add(productId.toHexString());
        categoryRepository.save(category);
        webSocketService.sendMessage("updateCategory", List.of(category.getId()));
    }

    public Optional<Category> getCategoryByName(String name) {
        return Optional.ofNullable(categoryRepository.findByName(name));
    }

    public void deleteCategory(ObjectId id) {
        categoryRepository.deleteById(id);
    }
}