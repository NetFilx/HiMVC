package com.hyson.servlet;

import com.hyson.annotation.Controller;
import com.hyson.annotation.RequestMapping;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @author wyh
 * @version 1.0
 * @time 2020/10/14 3:05 下午
 */
public class DispatcherServlet extends HttpServlet {

    private Properties properties = new Properties();

    private List<String> classNames = new ArrayList<>();

    private Map<String, Object> ioc = new HashMap<>();

    private Map<String, Method> handlerMapping = new HashMap<>();

    private Map<String, Object> controllerMap = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {

        // 1. 加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        // 2. 初始化相关的class，扫描包下所有的类
        doScanner(properties.getProperty("scanPackage"));

        // 3. 拿到扫描的类，通过反射，实例化，并放在ioc中，
        doInstance();

        // 4. 初始化HandlerMapping
        initHandlerMapping();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doDispatch(req, resp);
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (handlerMapping.isEmpty()) {
            return;
        }
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");
        if (handlerMapping.containsKey(url)) {
            resp.getWriter().write("404 NOT FOUND");
            return;
        }
        Method method = handlerMapping.get(url);

        //获取方法的参数列表
        Class<?>[] parameterTypes = method.getParameterTypes();

        //获取请求的参数
        Map<String, String[]> parameterMap = req.getParameterMap();

        // 保存参数值
        Object[] paramValues = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            String requestParam = parameterTypes[i].getSimpleName();
            if (requestParam.equals("HttpServletRequest")) {
                paramValues[i] = req;
                continue;
            }
            if (requestParam.equals("HttpServletResponse")) {
                paramValues[i] = resp;
                continue;
            }
            // 可以进行修改
            if (requestParam.equals("String")) {
                for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
                    String value = Arrays.toString(param.getValue()).replaceAll("[|]", "").replaceAll(",s", ",");
                    paramValues[i] = value;
                }
            }
        }
        try {
            //第一个参数是method所对应的实例 在ioc容器中
            method.invoke(this.controllerMap.get(url), paramValues);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doLoadConfig(String location) {
        try (InputStream config = this.getClass().getClassLoader().getResourceAsStream(location)) {
            properties.load(config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doScanner(String packageName) {
        URL packages = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll(".", "/"));
        File file = new File(packages.getFile());
        for (File f : file.listFiles()) {
            if (f.isDirectory()) {
                // 递归扫描包
                doScanner(packageName + "." + file.getName());
            } else {
                String c = packageName + "." + file.getName().replaceAll(".class", "");
                classNames.add(c);
            }
        }
    }

    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }

        for (String className : classNames) {
            try {
                Class<?> c = Class.forName(className);
                if (c.isAnnotationPresent(Controller.class)) {
                    ioc.put(toLowerFirstWord(className), c.newInstance());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        try {
            for (Map.Entry<String, Object> entry : ioc.entrySet()) {
                Class<?> c = entry.getValue().getClass();
                // 这个判断是必要的，因为ioc里面不一点全是controller
                if (!c.isAnnotationPresent(Controller.class)) {
                    continue;
                }
                String baseUrl = "";
                if (c.isAnnotationPresent(RequestMapping.class)) {
                    RequestMapping r = c.getAnnotation(RequestMapping.class);
                    baseUrl = r.value();
                }
                Method[] methods = c.getMethods();
                for (Method method : methods) {
                    if (!method.isAnnotationPresent(RequestMapping.class)) {
                        continue;
                    }
                    RequestMapping r = method.getAnnotation(RequestMapping.class);
                    String url = r.value();
                    url = (baseUrl + "/" + url).replace("/+", "/");
                    handlerMapping.put(url, method);
                    controllerMap.put(url, c.newInstance());
                    System.out.println(url + "," + method);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 首字母小写
     *
     * @param name
     * @return
     */
    private String toLowerFirstWord(String name) {
        char[] charArray = name.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }


}
