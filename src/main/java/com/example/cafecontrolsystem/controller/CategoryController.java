package com.example.cafecontrolsystem.controller;

import com.example.cafecontrolsystem.entity.MenuCategory;
import com.example.cafecontrolsystem.entity.CategoryType;
import com.example.cafecontrolsystem.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Arrays;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<MenuCategory>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/types")
    public ResponseEntity<List<String>> getCategoryTypes() {
        return ResponseEntity.ok(Arrays.stream(CategoryType.values())
                .map(CategoryType::name)
                .toList());
    }

    @PostMapping
    public ResponseEntity<MenuCategory> createCategory(@RequestBody MenuCategory category) {
        return ResponseEntity.ok(categoryService.saveCategory(category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MenuCategory> updateCategory(
            @PathVariable Long id,
            @RequestBody MenuCategory category) {
        category.setId(id);
        return ResponseEntity.ok(categoryService.saveCategory(category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok().build();
    }
} 