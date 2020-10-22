package com.dyzwj.controller;


import com.dyzwj.annotation.Controller;
import com.dyzwj.annotation.RequestMapping;
import com.dyzwj.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/hello")
public class HelloController {

    @RequestMapping("/test1.do")
    @ResponseBody
    public String test1(String name, HttpServletRequest request, HttpServletResponse response){
        System.out.println("HelloController-----test1");
        System.out.println(name + "---" + request + "---" + response);

        return name;
    }
}
