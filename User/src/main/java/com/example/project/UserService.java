package com.example.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
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

        callNotificationService(userRequestDTO.getName(),userRequestDTO.getEmail());

        return "User added successfully";
    }

    private void callNotificationService(String name, String email) {
        //Send the email to notifications-service via kafka
        JSONObject emailRequest = new JSONObject();
        emailRequest.put("email",email);

        String messageBody = String.format("Hi %s \n\n" +
                        "Congratulation!!! \n\n\n" +
                        "Your account has been created and 100 rupees credited in to your wallet. \n\n\n\n\n"+
                "Thank You \n"+
                "Ewallet Team",
                name);

        emailRequest.put("message",messageBody);

        String message = emailRequest.toString();

        //Send it to kafka
        kafkaTemplate.send("send_email",message);
    }

    public void saveInCache(User user) {
        Map map = objectMapper.convertValue(user,Map.class);

        String key = "USER_KEY"+user.getUserName();

        redisTemplate.opsForHash().putAll(key,map);
        redisTemplate.expire(key, Duration.ofHours(12));
    }

    public User findUserByUserName(String userName){

        //1. find in the redis cache
        Map map = redisTemplate.opsForHash().entries(userName);

        User user = null;
        //If not found in the redis/map
        if(map==null){

            //Find the userObject from the userRepo
            user = userRepository.findByUserName(userName);
            //Save that found user in the cache
            saveInCache(user);
            return user;
        }else{
            //We found out the User object
            user = objectMapper.convertValue(map, User.class);
            return user;

        }
    }

    public UserResponseDTO findEmailNameDTO(String userName) {
        //1. Find in the redis cache
        Map map = redisTemplate.opsForHash().entries(userName);

        User user = null;
        //If not found in the redis/map
        if(map == null){
            //Find in the DB(userRepo)
            user = userRepository.findByUserName(userName);

            //Save that user in to the cache
            saveInCache(user);
        }
        else{
            //We found out the user
            user = objectMapper.convertValue(map,User.class);
        }

        UserResponseDTO userResponseDTO = UserResponseDTO.builder().email(user.getEmail()).name(user.getName()).build();
        return userResponseDTO;
//        User user = userRepository.findByUserName(userName);
//
//        UserResponseDto userResponseDto = UserResponseDto.builder().email(user.getEmail()).name(user.getName()).build();
//
//        return userResponseDto;

    }
}
