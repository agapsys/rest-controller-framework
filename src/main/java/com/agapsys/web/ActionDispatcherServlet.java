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

package com.agapsys.web;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/*")
public class ActionDispatcherServlet extends HttpServlet {
	// CLASS SCOPE =============================================================	
	public static final String ATTR_EXCEPTION  = "javax.servlet.error.exception";
	
	private static final ActionDispatcher dispatcher = new ActionDispatcher();
	
	private static WebAction beforeAction   = null;
	private static WebAction afterAction    = null;
	private static WebAction errorAction    = null;
	private static WebAction notFoundAction = null;
	
	public static void registerAction(WebAction action, HttpMethod httpMethod, String url) {
		dispatcher.registerAction(action, httpMethod, url);
	}
	
	public static void registerBeforeAction(WebAction beforeAction) {
		ActionDispatcherServlet.beforeAction = beforeAction;
	}
	public static WebAction getBeforeAction() {
		return beforeAction;
	}
	
	public static void registerAfterAction(WebAction afterAction) {
		ActionDispatcherServlet.afterAction = afterAction;
	}
	public static WebAction getAfterAction() {
		return afterAction;
	}
	
	public static void registerErrorAction(WebAction errorAction) {
		ActionDispatcherServlet.errorAction = errorAction;
	}
	public static WebAction getErrorAction() {
		return errorAction;
	}
	
	public static void registerNotFoundAction(WebAction notFoundAction) {
		ActionDispatcherServlet.notFoundAction = notFoundAction;
	}
	public static WebAction getNotFoundAction() {
		return notFoundAction;
	}
	// =========================================================================

	// INSTANCE SCOPE ==========================================================
	@Override
	protected final void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		WebAction action = dispatcher.getAction(req);
		if (action == null) {
			if (notFoundAction != null) {
				notFoundAction.processRequest(req, resp);
			} else {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
		} else {
			try {
				if (beforeAction != null)
					beforeAction.processRequest(req, resp);
				
				action.processRequest(req, resp);
				
				if (afterAction != null)
					afterAction.processRequest(req, resp);
				
			} catch (Throwable t) {
				if (errorAction == null) {
					throw t;
				} else {
					req.setAttribute(ATTR_EXCEPTION, t);
					errorAction.processRequest(req, resp);
				}
			}
		}
	}
	// =========================================================================
}