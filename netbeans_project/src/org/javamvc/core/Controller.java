/*
Copyright 2015 Balwinder Sodhi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package org.javamvc.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.beanutils.BeanUtils;

/**
 * Base class for controllers. It implements some common convenience methods and
 * performs necessary initialization. A controller instance is not expected to
 * keep any per-request information, i.e., a controller is expected to be a
 * stateless object. A new instance of a controller will be created for each
 * HTTP request targeted to be handled by a controller.
 *
 * @author Balwinder Sodhi
 */
public abstract class Controller {

    protected HttpServletRequest request;
    protected ServletContext context;
    protected HttpServletResponse response;
    protected ViewProvider viewProvider;
    protected MemCacheProvider cache;

    /**
     * Initializes the controller instance. It injects a suitable
     * {@link ViewProvider} and also the {@link HttpServletRequest}, 
     * {@link HttpServletResponse} and {@link ServletContext} objects 
     * associated with current request.
     * @param cache Cached data across the application.
     * @param context ServletContext reference.
     * @param req Request object being handled.
     * @param res Response object.
     * @param vp View provider instance to be used for producing the view.
     */
    public void init(MemCacheProvider cache, ServletContext context,
            HttpServletRequest req, HttpServletResponse res, ViewProvider vp) {
        this.cache = cache;
        this.context = context;
        this.request = req;
        this.response = res;
        this.viewProvider = vp;
        try {
            trace();
        } catch (Exception ex) {
            /**
             * Initializing a controller should not stop due to any errors in 
             * trace method. So we simply print the error and continue.
             */
            ex.printStackTrace();
        }
    }

    /**
     * You can log any request specific information by overriding this method in
     * a controller subclass. Default implementation provided by this class does
     * nothing.
     */
    public void trace() {
        // Do nothing. Subclass should log trace info if needed.
    }
    
    /**
     * Returns the value of specified extra config property.
     * @see ControllerServlet
     * @param key Name of the extra config property.
     * @return Value of the property if found, else returns null.
     */
    public String getConfigValue(String key) {
        String value=null;
        Properties conf = (Properties) context.
                getAttribute(ControllerServlet.EXTRA_CONFIG);
        if (conf != null) {
            value = conf.getProperty(key);
        }
        return value;
    }

    /**
     * Sends the error response via JSON message to the client. HTTP response is
     * flushed.
     *
     * @param httpStatusCode Error status code to send.
     * @param message JSON string.
     * @throws IOException
     */
    public void sendJsonErrorResponse(int httpStatusCode, String message) throws IOException {
        response.setContentType("application/json");
        response.setStatus(httpStatusCode);
        response.getWriter().write(message);
        response.flushBuffer();
    }

    /**
     * Checks for the presence of <code>json</code> in <code>Accept</code>
     * header of HTTP request.
     *
     * @return
     */
    public boolean isJsonRequest() {
        String accept = request.getHeader("Accept");
        return accept.toLowerCase().contains("json");
    }

    /**
     * Reads the string data line by line from {@link BufferedReader} object of
     * the current HTTP request.
     *
     * @return Request data as string.
     * @throws IOException
     */
    public String getJsonData() throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } finally {
            reader.close();
        }
        return sb.toString();
    }

    /**
     * Returns reference to the {@link ViewProvider} configured for the
     * {@link ControllerServlet}. Default is {@link FreemarkerViewProvider}.
     *
     * @return
     */
    public ViewProvider getViewProvider() {
        return viewProvider;
    }

    /**
     * Returns the reference to current HTTP request being handled.
     *
     * @return
     */
    public HttpServletRequest getRequest() {
        return request;
    }

    /**
     * Returns the reference to response object for current HTTP request.
     *
     * @return
     */
    public HttpServletResponse getResponse() {
        return response;
    }

    public MemCacheProvider getMemCache() {
        return cache;
    }

    public ServletContext getContext() {
        return context;
    }

    /**
     * Populates specified template with given model.
     * @param templatePath Path of the template to be filled.
     * @param model Model with which to fill the template
     * @return Filled template as a string
     * @throws IOException 
     */
    public String populateTemplate(String templatePath, Object model) throws IOException {
        return viewProvider.renderView(templatePath, model);
    }

    /**
     * Finds the template for given view name and fills it with the supplied
     * model object's data. The filled template is then sent back to client as
     * string response.
     *
     * @param viewName
     * @param model
     * @throws IOException
     */
    public void View(String viewName, Object model) throws IOException {
        String result = viewProvider.renderView(findViewName(viewName), model);
        sendViewResponse(result);
    }

    /**
     * Finds the template for default view name (which is same as action name)
     * and fills it with the supplied model object's data. The filled template
     * is then sent back to client as string response.
     *
     * @param model
     * @throws IOException
     */
    public void View(Object model) throws IOException {
        View(null, model);
    }

    /**
     * Finds the template for default view name (which is same as action name)
     * and sent the empty view back to client as string response.
     *
     * @throws IOException
     */
    public void View() throws IOException {
        View(null, null);
    }

    /**
     * Sends the given string as response to the client. Suitable headers are
     * set on response so as to prevent caching by browser. Content type is set
     * as "text/html;charset=UTF-8".
     *
     * @param result
     * @throws IOException
     */
    private void sendViewResponse(String result) throws IOException {
        // No caching
        response.setHeader("Expires", "Tue, 03 Jul 1990 06:00:00 GMT");
        response.setDateHeader("Last-Modified", new Date().getTime());
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0, post-check=0, pre-check=0");
        response.setHeader("Pragma", "no-cache");

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter w = response.getWriter();
        w.write(result);
        w.flush();
    }

    /**
     * Sends given JSON string as response to the client.
     *
     * @param json
     * @throws IOException
     */
    public void Json(String json) throws IOException {
        response.setContentType("text/json;charset=UTF-8");
        PrintWriter w = response.getWriter();
        w.write(json);
        w.flush();
    }

    /**
     * Sends given JSON string as response to the client.
     *
     * @param json
     * @throws IOException
     */
    public void JsonScript(String json) throws IOException {
        response.setContentType("text/javascript;charset=UTF-8");
        PrintWriter w = response.getWriter();
        w.write(json);
        w.flush();
    }

    /**
     * Calculates the full path of the given view. If supplied view name is
     * null, then name of calling method in controller is taken as view name.
     * Final name is of the form: <code>Views/MyController/MyView</code>.
     *
     * @param viewNm
     * @return
     */
    private String findViewName(String viewNm) {
        if (viewNm == null) {
            // By default view name is same as calling method name in controller.
            StackTraceElement[] st = Thread.currentThread().getStackTrace();
            viewNm = st[4].getMethodName();
        }
        String ct = getClass().getSimpleName();
        String view = "Views/" + ct + "/" + viewNm;
        return view;
    }

    protected <T> void fromRequestParamsToObj(T obj)
            throws IllegalAccessException, InvocationTargetException {
        BeanUtils.populate(obj, request.getParameterMap());
    }

}
