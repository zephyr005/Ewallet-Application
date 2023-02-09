package com.example.backendproject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("/add")
    String createUser(UserRequestDTO userRequestDTO){
        return userService.addUser(userRequestDTO);
    }

    @GetMapping("/findUser/{userName}")
    User findUserByUserName(@PathVariable("userName") String userName){
        return userService.findUserByUserName(userName);
    }
}
