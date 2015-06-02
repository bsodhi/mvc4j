package org.javamvc.core;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * A class intended for rendering views must implement this interface.
 * For example, one may use a templating engine like Freemarker or Velocity etc.
 * to render views by combining suitable markup with data objects.
 * @author Balwinder Sodhi
 */
public interface ViewProvider {
    /**
     * Implementer can make use of servlet and it environment related
     * configuration objects to initialize this object.
     * @param servletContext
     * @param servletConfig 
     */
    void init(ServletContext servletContext, ServletConfig servletConfig);
    /**
     * 
     * @param view. Full path of the view template. If the template.extension 
     * servlet parameter is configured for the {@link ControllerServlet} then 
     * that extension can also be used by the implementer to locate the template. 
     * @param model. Model object to be used to populate the template.
     * @return Final view markup produced by combining view template with the
     * model data.
     * @throws IOException 
     */
    String renderView(String view, Object model) throws IOException;
}
