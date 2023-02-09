package com.example.backendproject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
public class UserService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    RedisTemplate<String,User> redisTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    KafkaTemplate<String,String> kafkaTemplate;

    public String addUser(UserRequestDTO userRequestDTO){
        User user = User.builder().userName(userRequestDTO.getUserName()).email(userRequestDTO.getEmail()).age(userRequestDTO.getAge()).mobileNumber(userRequestDTO.getMobileNumber()).name(userRequestDTO.getName()).build();

        //Save it to DB
        userRepository.save(user);

        //Save it to Cache
        saveInCache(user);

        //Send an update to the wallet module/wallet service --> that create a new wallet from the userName sent as a string
        kafkaTemplate.send("create_wallet",user.getUserName());

        return "User added successfully";
    }

    public void saveInCache(User user) {
        Map map = objectMapper.convertValue(user,Map.class);

        String key = "USER_KEY"+user.getUserName();

        redisTemplate.opsForHash().putAll(key,map);
        redisTemplate.expire(key, Duration.ofHours(12));
    }

    public User findUserByUserName(String userName) {
        //1. Find in the cache
        Map map = redisTemplate.opsForHash().entries(userName);

        //If not found in redis/map
        if(map == null){
            //Find in the DB(userRepo)
            User user = userRepository.findUserByUserName(userName);

            //Save that user in to the cache
            saveInCache(user);
        }
        else{
            //We found out the user
            User user = objectMapper.convertValue(map,User.class);
            return user;
        }
        return null;
    }
}
