package com.example.SmartStay.product;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, ObjectId> {

    @Query("{ $or: [ " +
            "{ 'address.country': { $regex: ?0, $options: 'i' } }, " +
            "{ 'address.country': { $regex: ?1, $options: 'i' } }, " +
            "{ 'address.country': { $regex: ?2, $options: 'i' } }, " +
            "{ 'address.city': { $regex: ?0, $options: 'i' } }, " +
            "{ 'address.city': { $regex: ?1, $options: 'i' } }, " +
            "{ 'address.city': { $regex: ?2, $options: 'i' } } " +
            "] }"
    )
    List<Product> findByMultipleQueriesToAddress(String query1, String query2, String query3);

    @Query("{ $and: [ " +
            "{ 'address.country': { $regex: ?0, $options: 'i' } }, " +
            "{ 'address.city': { $regex: ?1, $options: 'i' } }, " +
            "] }"
    )
    List<Product> findByCountryCity(String country, String city);

    @Query(value = "{}", fields = "{ 'address.country': 1, 'address.city': 1 }")
    Collection<ProductSummary> findAllAddresses();
}