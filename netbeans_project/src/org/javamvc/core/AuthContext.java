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

import java.util.List;
import org.javamvc.core.annotations.Authorize;

/**
 * An application is expected to provide an implementation of this interface if
 * the application uses role based access to controller action methods via
 * {@link Authorize} annotation. The application on successfully authenticating
 * a user should create an instance of this class and store it in a session
 * attribute named <code>auth.context</code>.
 * 
 * @author Balwinder Sodhi
 */
public interface AuthContext {
    /**
     * Returns the login name of currently authenticated user.
     * 
     * @return 
     */
    String getLoginName();
    
    /**
     * Returns the list of role names granted to currently authenticated user.
     * @return 
     */
    List<String> getRoles();

    /**
     * Verifies whether current user has specified role or not. This is a 
     * required method for using RBAC via {@link Authorize} annotations.
     * @param role
     * @return 
     */
    boolean hasRole(String role);

    /**
     * Checks whether current user is in authenticated state.This is a 
     * required method for using RBAC via {@link Authorize} annotations.
     * 
     * @return 
     */
    boolean isAuthenticated();
}
