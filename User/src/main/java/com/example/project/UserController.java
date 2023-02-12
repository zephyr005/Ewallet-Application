package com.example.project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserService userService;

    //Add user
    @PostMapping("/add")
    public String createUser(@RequestBody UserRequestDTO userRequestDTO){
        return userService.addUser(userRequestDTO);
    }

    //Find user by userName
    @GetMapping("/findByUserName/{userName}")
    User getUserByUserName(@PathVariable("userName")String userName){
        return userService.findUserByUserName(userName);
    }

    //Find email and name response DTO
    @GetMapping("/findEmailNameDTO/{userName}")
    public UserResponseDTO getEmailNameDTO(@PathVariable("userName") String userName){
        return userService.findEmailNameDTO(userName);
    }
}
