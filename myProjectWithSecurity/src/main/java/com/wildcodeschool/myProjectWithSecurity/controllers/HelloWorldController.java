package com.wildcodeschool.myProjectWithSecurity.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController {

    @GetMapping("/")
    public String welcome() {
        return "Welcome to the SHIELD";
    }
    @GetMapping("/avengers/assemble")
    public String assemble() {
        return "Avengers..... Assemble";
    }
    @GetMapping("/secret-bases")
    public String bases() {
    	return "Amsterdam, Barcelona, Berlin, Biarritz, Bordeaux, Brussels, Bucharest, Budapest, Dublin, ... going wild all over the EU";
    }
    
}
