package me.robin.wx.web;

import me.robin.wx.web.listener.AppContextListener;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.servlet.*;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by xuanlubin on 2017/3/28.
 */
@WebServlet(name = "DispatchServlet", value = "/*", loadOnStartup = 1)
public class DispatchServlet extends HttpServlet {

    private AbstractApplicationContext wac;

    private Map<String, Boolean> init = new ConcurrentHashMap<>();

    @Override
    public void init() throws ServletException {
        super.init();
        this.wac = new ClassPathXmlApplicationContext(new String[]{"classpath*:application-web.xml"}, (ApplicationContext) getServletContext().getAttribute(AppContextListener.class.getName()));
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        String path = ((HttpServletRequest) req).getRequestURI();
        String servletPath = StringUtils.substringAfter(path, ((HttpServletRequest) req).getContextPath());
        Servlet servlet = this.wac.getBean(servletPath, Servlet.class);
        if (!init.containsKey(path)) {
            WebInitParam[] initParams = servlet.getClass().getAnnotationsByType(WebInitParam.class);
            ConcurrentHashMap<String, String> parameterMap = new ConcurrentHashMap<>();
            if (null != initParams && initParams.length > 0) {
                for (WebInitParam initParam : initParams) {
                    parameterMap.put(initParam.name(), initParam.value());
                }
            }
            ServletConfig servletConfig = new ServletConfig() {
                @Override
                public String getServletName() {
                    return servlet.getClass().getSimpleName();
                }

                @Override
                public ServletContext getServletContext() {
                    return getServletConfig().getServletContext();
                }

                @Override
                public String getInitParameter(String name) {
                    return parameterMap.get(name);
                }

                @Override
                public Enumeration<String> getInitParameterNames() {
                    return parameterMap.keys();
                }
            };
            servlet.init(servletConfig);
            init.put(path, true);
        }
        servlet.service(req, res);
    }

    @Override
    public void destroy() {
        this.wac.close();
    }
}
