package com.example.SmartStay.category;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface CategoryRepository extends MongoRepository<Category, ObjectId> {

    @Query("{ 'name' : { $regex: ?0, $options: 'i' } }")
    Category findByName(String name);

    @Query("{ 'products' : { $in: [ ?0 ] } }")
    Category findByProductsContaining(String productId);
}
