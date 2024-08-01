package com.shib.batch.writer;

import com.shib.batch.entity.User;
import com.shib.batch.repository.UserRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserWriter implements ItemWriter<User> {

    @Autowired
    UserRepository userRepository;

    @Override
    public void write(Chunk<? extends User> chunk) throws Exception {
        System.out.println("Thread Name"+ Thread.currentThread().getName());
        userRepository.saveAll(chunk);
    }
}
