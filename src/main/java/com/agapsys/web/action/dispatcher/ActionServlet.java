/*
 * Copyright 2015 Agapsys Tecnologia Ltda-ME.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.agapsys.web.action.dispatcher;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ActionServlet extends HttpServlet {
	// CLASS SCOPE =============================================================
	private static final CsrfSecurityHandler DEFAULT_CSRF_SECURITY_HANDLER = new CsrfSecurityHandler();
	private static final UserManager         DEFAULT_USER_MANAGER          = new UserManager() {

		@Override
		public void setSessionUser(User user, HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
			super.setSessionUser(user, req, resp);
			String csrfToken = DEFAULT_CSRF_SECURITY_HANDLER.generateCsrfToken();
			DEFAULT_CSRF_SECURITY_HANDLER.setSessionCsrfToken(csrfToken, req, resp);
			DEFAULT_CSRF_SECURITY_HANDLER.sendCsrfToken(csrfToken, req, resp);
		}
		
	};
	
	private static void matchSignature(Method method) throws RuntimeException {
		String signature = method.toGenericString();

		String errMsg = String.format("Invalid signature (%s). Required: public void <method_name>(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse) throws javax.servlet.ServletException,java.io.IOException", signature);
		
		if (!signature.startsWith("public void "))
			throw new RuntimeException(errMsg);
		
		int indexOfOpenParenthesis = signature.indexOf("(");
		int indexOfCloseParenthesis = signature.indexOf(")");
		
		String args = signature.substring(indexOfOpenParenthesis + 1, indexOfCloseParenthesis);
		if (!args.equals("javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse"))
			throw new RuntimeException(errMsg);
		
		Set<String> thrownExceptions = new LinkedHashSet<>(Arrays.asList(signature.substring(indexOfCloseParenthesis + 1).trim().replace("throws ", "").split(",")));
		if (!(thrownExceptions.size() == 2 && thrownExceptions.contains("javax.servlet.ServletException") && thrownExceptions.contains("java.io.IOException")))
			throw new RuntimeException(errMsg);
	}
	// =========================================================================
	
	// INSTANCE SCOPE ==========================================================	
	private class CallerAction extends AbstractAction {
		private final Method method;
		
		public CallerAction(Method method) {
			super(null);
			this.method = method;
		}
		
		public CallerAction(Method method, SecurityHandler securityHandler) {
			super(securityHandler);
			this.method = method;
		}
		
		@Override
		protected void onProcessRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
			try {
				method.invoke(ActionServlet.this, req, resp);
			} catch (InvocationTargetException ex) {
				Throwable cause = ex.getCause();
				
				if (cause instanceof IOException)
					throw (IOException) cause;
				
				if (cause instanceof ServletException)
					throw (ServletException) cause;
				
				throw new RuntimeException(cause);
			} catch (IllegalAccessException | IllegalArgumentException ex) {
				throw new RuntimeException(ex);
			}
		}
	}
	
	private final ActionDispatcher dispatcher = new ActionDispatcher();
	
	private volatile boolean initialized = false;
	
	protected synchronized void initialize() {
		if (!initialized) {
			Method[] methods = this.getClass().getDeclaredMethods();
			for (Method method : methods) {
				Annotation[] annotations = method.getAnnotations();
				for (Annotation annotation : annotations) {
					if (annotation instanceof WebAction) {
						matchSignature(method);
						WebAction webAction = (WebAction) annotation;

						String[] requiredRoles = webAction.requiredRoles();
						Set<String> requiredRoleSet = new LinkedHashSet<>();

						for (String role : requiredRoles) {
							if (!requiredRoleSet.add(role))
								throw new RuntimeException("Duplicate role: " + role);
						}

						HttpMethod httpMethod = webAction.httpMethod();
						String url = webAction.mapping();

						if (url.trim().isEmpty())
							url = method.getName();

						if (!url.startsWith("/"))
							url = "/" + url;

						SecurityHandler handler = getSecurityHandler(requiredRoleSet);
						CallerAction callerAction = new CallerAction(method, handler);
						dispatcher.registerAction(callerAction, httpMethod, url);
						
						if (webAction.defaultAction()) {
							dispatcher.registerAction(callerAction, httpMethod, ActionDispatcher.DEFAULT_URL);
						}
					}
				}
			}
			initialized = true;
		}
	}
	
	/** 
	 * Called before an action. 
	 * This method will be called only if an action associated to given request is found.
	 * Default implementation does nothing.
	 * @param req HTTP request
	 * @param resp HTTP Response
	 * @throws IOException when there is an error processing the request
	 * @throws ServletException when there is an error processing the request
	 */
	protected void beforeAction(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException  {}
	
	/** 
	 * Called after an action. 
	 * This method will be called only if an action associated to given request is found.
	 * Default implementation does nothing.
	 * @param req HTTP request
	 * @param resp HTTP Response
	 * @throws IOException when there is an error processing the request
	 * @throws ServletException when there is an error processing the request
	 */
	protected void afterAction(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException  {}
	
	/** 
	 * Called when an action is not found.
	 * Default implementation sends {@linkplain HttpServletResponse#SC_NOT_FOUND} error.
	 * @param req HTTP request
	 * @param resp HTTP Response
	 * @throws IOException when there is an error processing the request
	 * @throws ServletException when there is an error processing the request
	 */
	protected void onNotFound(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException  {
		resp.sendError(HttpServletResponse.SC_NOT_FOUND);
	}
	
	/**
	 * Returns the user manager used by this servlet
	 * @return the user manager used by this servlet
	 */
	protected UserManager getUserManager() {
		return DEFAULT_USER_MANAGER;
	}
	
	/**
	 * Returns the security manager used by this servlet
	 * @param requiredRoles required roles
	 * @return the security manager used by this servlet
	 */
	protected SecurityHandler getSecurityHandler(Set<String> requiredRoles) {
		Set<SecurityHandler> handlerSet = new LinkedHashSet<>();
		handlerSet.add(new UserRoleSecurityHandler(getUserManager(), requiredRoles));
		handlerSet.add(DEFAULT_CSRF_SECURITY_HANDLER);
		return new SecurityHandlerSet(handlerSet);
	}
	
	@Override
	protected final void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!initialized)
			initialize();
		
		Action action = dispatcher.getAction(req);
		if (action == null) {
			onNotFound(req, resp);
		} else {
			beforeAction(req, resp);
			action.processRequest(req, resp);
			afterAction(req, resp);
		}
	}
	// =========================================================================
}