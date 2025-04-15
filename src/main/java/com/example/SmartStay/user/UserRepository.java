package com.example.SmartStay.user;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface UserRepository extends MongoRepository<User, ObjectId> {
    List<UserProjection> findAllProjectedBy();
}