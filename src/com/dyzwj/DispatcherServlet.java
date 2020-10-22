package com.dyzwj;

import com.dyzwj.annotation.Controller;
import com.dyzwj.annotation.RequestMapping;
import com.dyzwj.annotation.ResponseBody;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;

public class DispatcherServlet extends HttpServlet {

    private Properties properties = new Properties();

    private List<String> classNames = new ArrayList<>();

    private Map<String, Object> ioc = new HashMap<>();

    private Map<String, Method> handlerMapping = new HashMap<>();

    private Map<String, Object> controllerMap = new HashMap<>();


    @Override
    public void init(ServletConfig config) throws ServletException {


        System.out.println("--------init-------------");
        //1、加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
//        properties.setProperty("basePackage","com.com.dyzwj.controller");
        //2、初始化所有相关联的类，扫描用户设定的包下面的所有的类
        doScanner(properties.getProperty("basePackage"));

        classNames.forEach(System.out::println);

        //3、拿到扫描到的类，通过反射机制 实例化并且放到ioc容器中 (k-v  beanName-bean) beanName默认是首字母小写
        doInstance();
        //4、初始化HandlerMapping (将url和method对应上)
        initHandlerMapping();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatcher(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (handlerMapping.isEmpty()) {
            return;
        }

        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();
        String uri = requestURI.replace(contextPath, "");
        if (!handlerMapping.containsKey(uri)) {
            resp.getWriter().write("404 NOT FOUND!");
            return;
        }

        Method method = handlerMapping.get(uri);
        if (method != null) {

            Parameter[] parameters = method.getParameters();
            Object[] param = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                String className = parameters[i].getType().getSimpleName();
                if (className.equalsIgnoreCase(String.class.getSimpleName())) {
                    param[i] = req.getParameter(parameters[i].getName());
                } else if (className.equalsIgnoreCase(HttpServletRequest.class.getSimpleName())) {
                    param[i] = req;
                } else if (className.equalsIgnoreCase(HttpServletResponse.class.getSimpleName())) {
                    param[i] = resp;
                }
            }
            //执行方法 拿到返回值
            Object invoke = method.invoke(controllerMap.get(uri), param);

            if(!method.isAnnotationPresent(ResponseBody.class)){
                resp.sendRedirect(invoke.toString());
            }
            //简单写到页面上
            resp.getWriter().write(invoke.toString());

        }


    }

    //初始化HandlerMapping (将url和method对应上)
    private void initHandlerMapping() {
        Set<Map.Entry<String, Object>> entries = ioc.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            Class<?> aClass = entry.getValue().getClass();
            //判断类上有没有加Controller注解
            if (!aClass.isAnnotationPresent(Controller.class) && !aClass.isAnnotationPresent(RequestMapping.class)) {
                continue;
            }
            RequestMapping aClassAnnotation = aClass.getAnnotation(RequestMapping.class);
            String parentPath = aClassAnnotation.value();

            //拿到所有的method
            Method[] methods = aClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(RequestMapping.class)) {
                    RequestMapping annotation = method.getAnnotation(RequestMapping.class);
                    String subPath = annotation.value();
                    handlerMapping.put(parentPath + subPath, method);
                    controllerMap.put(parentPath + subPath, entry.getValue());
                    System.out.println("路径：" + parentPath + subPath + "，映射到：类" + entry.getValue() + "的" + method.getName() + "方法");
                }
            }
        }
    }

    //拿到扫描到的类，通过反射机制 实例化并且放到ioc容器中 (k-v  beanName-bean) beanName默认是首字母小写
    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        classNames.forEach(className -> {
            try {
                Class<?> clazz = Class.forName(className);
                //判断 类上 有没有加 @Controller 或 @RequestMapping注解
                if (clazz.isAnnotationPresent(Controller.class) || clazz.isAnnotationPresent(RequestMapping.class)) {
                    Object instance = clazz.newInstance();
                    ioc.put(toLowerFirstWord(clazz.getSimpleName()), instance);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    //首字母小写
    private String toLowerFirstWord(String className) {
        char[] chars = className.toCharArray();
        chars[0] += 32;
        return new String(chars);
    }


    //初始化所有相关联的类，扫描用户设定的包下面的所有的类
    public void doScanner(String basePackage) {
        URL url = this.getClass().getResource("/" + basePackage.replaceAll("\\.", "/"));
        File file = new File(url.getFile());
        File[] files = file.listFiles();
        for (File file1 : files) {
            if (file1.isDirectory()) {
                doScanner(basePackage + "." + file1.getName());
            } else {
                String name = file1.getName();
                if (name.substring(name.lastIndexOf(".")).equalsIgnoreCase(".class")) {
                    String className = basePackage + "." + name.substring(0, name.lastIndexOf("."));
                    this.classNames.add(className);
                }
            }
        }
    }


    //加载配置文件
    private void doLoadConfig(String contextConfigLocation) {
        InputStream inputStream = null;
        try {
            inputStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
            properties.load(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
