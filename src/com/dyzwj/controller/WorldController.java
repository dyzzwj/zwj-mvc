package com.dyzwj.controller;

import com.dyzwj.annotation.Controller;
import com.dyzwj.annotation.RequestMapping;

@Controller
@RequestMapping("/world")
public class WorldController {

    @RequestMapping("/demo1.do")
    public void demo1(){
        System.out.println("WorldController ----- demo1");
    }

}
