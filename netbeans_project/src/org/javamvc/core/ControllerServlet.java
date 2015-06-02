package org.javamvc.core;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.javamvc.core.annotations.Action;
import org.javamvc.core.annotations.Authorize;

/**
 * In order to make use of ASP.NET MVC style view and controllers, you can use
 * this servlet. Following three init parameters may be supplied:
 * <ol>
 * <li>controller.package.name -- Fully qualified name of the java package in
 * your application where controller classes will be placed. This is a required
 * parameter.</li>
 * <li>view.provider.class -- Fully qualified name of the class which implements
 * {@link ViewProvider}. If not specified, this servlet will use
 * {@link FreemarkerViewProvider} included in this library.</li>
 * <li>template.extension -- Optionally, you can specify the extension used for
 * template files used for creating views. e.g. .html, .ftl etc.</li>
 * <li>load.extra.config -- Optionally, you can specify path to a properties
 * file to be loaded for use in the application. This file must be loadable as
 * a resource by servlet (e.g. "/WEB-INF/my_extra_config.properties"). The
 * loaded properties are set as servlet context attribute named by 
 * {@link #EXTRA_CONFIG}.</li>
 * </ol>
 *
 * @author Balwinder Sodhi
 */
@MultipartConfig
public class ControllerServlet extends HttpServlet {

    private String controllerPkg;
    private ViewProvider viewProvider;
    public static final String EXTRA_CONFIG = "ControllerServlet.EXTRA_CONFIG";
    private static ConcurrentHashMap sharedData;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        synchronized (ControllerServlet.class) {
            if (sharedData == null) {
                sharedData = new ConcurrentHashMap();
                log("Initialized shared data container instance. "+sharedData);
            }
        }
        controllerPkg = config.getInitParameter("controller.package.name");
        if (controllerPkg != null) {
            controllerPkg = controllerPkg.trim();
        }
        String extraConfig = config.getInitParameter("load.extra.config");
        if (extraConfig != null) {
            InputStream stream = getServletContext().getResourceAsStream(extraConfig);
            Properties prop = new Properties();
            try {
                prop.load(stream);
                getServletContext().setAttribute(EXTRA_CONFIG, prop);
            } catch (IOException ex) {
                throw new ServletException("Could not load extra config. ", ex);
            }
        }

        String viewProviderClass = config.getInitParameter("view.provider.class");
        if (viewProviderClass == null) {
            viewProviderClass
                    = getClass().getPackage().getName() + ".FreemarkerViewProvider";
        }
        try {
            viewProvider = (ViewProvider) Class.forName(viewProviderClass).newInstance();
            viewProvider.init(getServletContext(), config);
        } catch (Exception ex) {
            throw new ServletException("Could not initialize ViewProvider. ", ex);
        }
        log("Using "+viewProvider.getClass().getName()+" view provider.");
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            delegateAction(request, response);
        } catch (Exception ex) {
            ex.printStackTrace();
            sendJsonErrorResponse(response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getLocalizedMessage());
        }
    }

    private void sendJsonErrorResponse(HttpServletResponse response,
            int httpStatus, String message) throws IOException {
        response.setContentType("application/json");
        response.setStatus(httpStatus);
//        StringBuilder sb = new StringBuilder();
//        sb.append("{\"status\":\"").append(httpStatus).append("\", ");
//        sb.append("\"message\":\"").append(message).append("\"}");
        response.getWriter().write(message);
        response.flushBuffer();
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

    /**
     * We extract the name of controller and action from the given HTTP request.
     * The request URL is expected to contain the controller and action name as
     * the last two parts (excluding query parameters) in the URI. E.g.
     * http://host.com:8080/MyApp/SomeController/FooBarAction.someExt?p1=v1&p2=v2
     * will result in the controller name as "SomeController", and action name
     * as "FooBarAction". Extension name, if present, of the action is excluded.
     * Basically, last part (excluding extension) is action name and second-last
     * part if the controller name.
     * @param request Current HTTP request being handled by this servlet.
     * @return A String array whose first element is action name and second
     * element is the name of controller.
     */
    private String[] extractControllerInfo(HttpServletRequest request) {

        String uri = request.getRequestURI().substring(1); // URI without the starting slash
        log("URI=" + uri);
        String[] uriParts = uri.split("/");
        String actionName = tryGetArrayElement(uriParts, uriParts.length-1);
        actionName = actionName.substring(0, actionName.indexOf("."));
        String controllerName = tryGetArrayElement(uriParts, uriParts.length-2);
        return new String[]{actionName, controllerName};
    }

    /**
     * Try to safely get the array element at given index. We just want to see
     * if an element can be accessed without overshooting array bounds.
     *
     * @param array
     * @param index
     * @return The element at given index if it exists. If element doesn't exist
     * then null is returned.
     */
    private String tryGetArrayElement(String[] array, int index) {
        String value = null;
        try {
            value = array[index];
        } catch (Exception e) {
            // Do nothing
        }
        return value;
    }

    /**
     * Delegates the servlet request to suitable controller for processing. We
     * first identify the names of controller class and action method from
     * current HTTP request URL. Then, if the currently logged on application
     * user is authorized to invoke the action, we instantiate the controller
     * class and invoke action method on it.
     *
     * @param request Current HTTP request being processed by servlet.
     * @param response Response to be sent for current HTTP request.
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private void delegateAction(HttpServletRequest request, HttpServletResponse response)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException,
            InvocationTargetException, ClassNotFoundException, IOException {

        String[] actCtrl = extractControllerInfo(request);
        log("Controller=" + actCtrl[1] + ", Action=" + actCtrl[0]);

        // Create and initialize the controller object
        Class ctor = Class.forName((controllerPkg != null ? controllerPkg + "." : "") + actCtrl[1]);

        // Invoke the controller action method
        if (isAuthorized(ctor, actCtrl[0], request)) {
            Object obj = ctor.newInstance();
            MethodUtils.invokeMethod(obj, "init", sharedData, getServletContext(),
                    request, response, viewProvider);
            MethodUtils.invokeMethod(obj, actCtrl[0]);
        } else {
            sendJsonErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                    actCtrl[0] + " is not authorized.");
            //response.sendError(HttpServletResponse.SC_FORBIDDEN, actCtrl[0] + " is not authorized.");
        }
    }

    /**
     * We check here whether the currently logged on application user is
     * authorized to invoke the given method on a class.
     *
     * @param objType Type of class on which given method is being invoked.
     * @param methodName Name of the method being invoked.
     * @param request HTTP request received by this servlet.
     * @return Returns true if authorized, else false is returned.
     */
    private boolean isAuthorized(Class objType, String methodName, HttpServletRequest request) {
        Method m = MethodUtils.getAccessibleMethod(objType, methodName);
        if (null == m) {
            log("Attempt to invoke a non-existing action method ("+methodName+")"
                    + ". Will not authorize request.");
            return false;            
        }
        Action act = m.getAnnotation(Action.class);
        if (null == act) {
            log("Attempt to invoke a non-action method ("+methodName+")"
                    + ". Will not authorize request.");
            return false;
        }
        Authorize a = m.getAnnotation(Authorize.class);
        // If no annotation found then we assume that the action is authorized
        if (null == a) {
            return true;
        }
        /**
         * If annotation is found but no "roles" attribute in session then we
         * don't authorize. A properly authenticated user is expected to have a
         * "roles" attribute in session.
         */
        String[] userRoles = (String[]) request.getSession().getAttribute("roles");
        if (userRoles == null || userRoles.length < 1) {
            log("Attribute 'role' not found in session. Will not authorize request.");
            return false;
        }
        /**
         * If a role matching the one specified in annotation is found then we
         * authorize, else we do not.
         */
        String[] roles = a.roles();
        if (roles != null && roles.length > 0) {
            List list = Arrays.asList(roles);
            boolean authorized = list.contains("*");
            if (!authorized) {
                for (String r : userRoles) {
                    authorized = list.contains(r);
                    if (authorized) {
                        break;
                    }
                }
            }
            return authorized;
        } else {
            return false;
        }
    }

}
