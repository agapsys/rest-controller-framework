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

package action.dispatcher.integration;

import action.dispatcher.integration.servlets.DefaultActionServlet;
import action.dispatcher.integration.servlets.PhaseActionsServlet;
import action.dispatcher.integration.servlets.PublicServlet;
import com.agapsys.http.HttpGet;
import com.agapsys.http.HttpHeader;
import com.agapsys.http.HttpResponse.StringResponse;
import com.agapsys.http.StringEntityRequest.StringEntityPost;
import com.agapsys.sevlet.container.ServletContainer;
import com.agapsys.sevlet.container.ServletContainerBuilder;
import com.agapsys.sevlet.container.StacktraceErrorHandler;
import com.agapsys.web.action.dispatcher.ActionServlet;
import com.agapsys.web.action.dispatcher.WebAction;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ActionServletGeneralTest {
	// CLASS SCOPE =============================================================
	@WebServlet("/invalid1")
	public static class InvalidUrlPatternServlet extends ActionServlet {
		@WebAction
		public void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {}
	}
	
	// Default actions ---------------------------------------------------------
	public static final String DEFAULT_ACTION_DEFAULT_URL = "/default";
	public static final String DEFAULT_ACTION_GET_URL     = "/default/get";
	public static final String DEFAULT_ACTION_POST_URL    = "/default/post";
	
	// Phase actions -----------------------------------------------------------	
	public static final String PHASE_DEFAULT_URL       = "/phase";
	public static final String PHASE_BEFORE_HEADER     = "before-header";
	public static final String PHASE_AFTER_HEADER      = "before-header";
	public static final String PHASE_NOT_FOUND_HEADER  = "not-found";
	
	// Secured actions ---------------------------------------------------------	
	public static final String PUBLIC_DEFAULT                   = "/public";
	public static final String PUBLIC_GET_URL                   = "/public/get";
	public static final String PUBLIC_MAPPED_GET_URL            = "/public/mapped/get";
	public static final String PUBLIC_MAPPED_WITH_SLASH_GET_URL = "/public/mapped/get2";
	public static final String PUBLIC_POST_URL                  = "/public/post";
	public static final String PUBLIC_MAPPED_POST_URL           = "/public/mapped/post";
	public static final String PUBLIC_WEBACTIONS_URL            = "/public/repeatableGetOrPost";
	public static final String PUBLIC_MULTIPLE_METHODS_URL      = "/public/multipleMethods";
	// =========================================================================
	
	// INSTANCE SCOPE ==========================================================	
	private ServletContainer sc;
	
	private void expectNullPhaseHeaders(StringResponse resp) {
		Assert.assertNull(resp.getFirstHeader(PHASE_BEFORE_HEADER));
		Assert.assertNull(resp.getFirstHeader(PHASE_AFTER_HEADER));
	}
	
	@Before
	public void before() {
		// Register dispatcher servlet...
		sc = new ServletContainerBuilder()
			.addRootContext()
				.registerServlet(InvalidUrlPatternServlet.class)
				.registerServlet(PublicServlet.class)
				.registerServlet(PhaseActionsServlet.class)
				.registerServlet(DefaultActionServlet.class)
				.setErrorHandler(new StacktraceErrorHandler())
			.endContext()
			.build();
		
		sc.startServer();
	}
	
	@After
	public void after() {
		sc.stopServer();
	}
	
	@Test
	public void callInvalidMappedServlet() {
		StringResponse resp = sc.doRequest(new HttpGet("/invalid1"));
		Assert.assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resp.getStatusCode());
		String expectedErrorMessage = String.format("Invalid URL pattern '%s' for class '%s' (pattern must end with '/*')", "/invalid1", InvalidUrlPatternServlet.class.getName());
		Assert.assertTrue(resp.getContentString().contains(expectedErrorMessage));
	}
	
	@Test
	public void testDefaultActions() {
		StringResponse resp;
		
		// GET: GET
		resp = sc.doRequest(new HttpGet(DEFAULT_ACTION_GET_URL));
		Assert.assertEquals(HttpServletResponse.SC_OK, resp.getStatusCode());
		Assert.assertEquals(DEFAULT_ACTION_GET_URL, resp.getContentString());
		
		
		// POST: POST		
		resp = sc.doRequest(new StringEntityPost("text/plain", "utf-8", DEFAULT_ACTION_POST_URL));
		Assert.assertEquals(HttpServletResponse.SC_OK, resp.getStatusCode());
		Assert.assertEquals(DEFAULT_ACTION_POST_URL, resp.getContentString());
		
		// GET: DEFAULT
		resp = sc.doRequest(new HttpGet(DEFAULT_ACTION_DEFAULT_URL));
		Assert.assertEquals(HttpServletResponse.SC_OK, resp.getStatusCode());
		Assert.assertEquals(DEFAULT_ACTION_GET_URL, resp.getContentString());
		
		// GET: DEFAULT + "/"
		resp = sc.doRequest(new HttpGet(DEFAULT_ACTION_DEFAULT_URL + "/"));
		Assert.assertEquals(HttpServletResponse.SC_OK, resp.getStatusCode());
		Assert.assertEquals(DEFAULT_ACTION_GET_URL, resp.getContentString());
		
		// POST: DEFAULT
		resp = sc.doRequest(new StringEntityPost("text/plain", "utf-8", DEFAULT_ACTION_DEFAULT_URL));
		Assert.assertEquals(HttpServletResponse.SC_OK, resp.getStatusCode());
		Assert.assertEquals(DEFAULT_ACTION_POST_URL, resp.getContentString());
		
		// POST: DEFAULT + "/"
		resp = sc.doRequest(new StringEntityPost("text/plain", "utf-8", DEFAULT_ACTION_DEFAULT_URL + "/"));
		Assert.assertEquals(HttpServletResponse.SC_OK, resp.getStatusCode());
		Assert.assertEquals(DEFAULT_ACTION_POST_URL, resp.getContentString());
	}
	
	@Test
	public void testMappingSlash() {
		StringResponse resp;
		
		// GET: PUBLIC GET
		resp = sc.doRequest(new HttpGet(PUBLIC_MAPPED_WITH_SLASH_GET_URL));
		Assert.assertEquals(HttpServletResponse.SC_OK, resp.getStatusCode());
		Assert.assertEquals(PUBLIC_MAPPED_WITH_SLASH_GET_URL, resp.getContentString());
		
		expectNullPhaseHeaders(resp);
	}
	
	@Test
	public void testPhaseActions() {
		StringResponse resp;
		HttpHeader beforeHeader;
		HttpHeader afterHeader;
		HttpHeader notFoundHeader;
		
		// GET
		resp = sc.doRequest(new HttpGet(PHASE_DEFAULT_URL));
		beforeHeader = resp.getFirstHeader(PHASE_BEFORE_HEADER);
		afterHeader = resp.getFirstHeader(PHASE_AFTER_HEADER);
		notFoundHeader = resp.getFirstHeader(PHASE_NOT_FOUND_HEADER);
		
		Assert.assertEquals(HttpServletResponse.SC_OK, resp.getStatusCode());
		Assert.assertEquals(PHASE_DEFAULT_URL, resp.getContentString());
		
		Assert.assertNotNull(beforeHeader);
		Assert.assertNotNull(afterHeader);
		Assert.assertNull(notFoundHeader);
		
		Assert.assertEquals(PHASE_BEFORE_HEADER, beforeHeader.getValue());
		Assert.assertEquals(PHASE_AFTER_HEADER, afterHeader.getValue());
		
		
		// POST
		resp = sc.doRequest(new StringEntityPost("text/plain", "utf-8", PHASE_DEFAULT_URL));
		beforeHeader = resp.getFirstHeader(PHASE_BEFORE_HEADER);
		afterHeader = resp.getFirstHeader(PHASE_AFTER_HEADER);
		notFoundHeader = resp.getFirstHeader(PHASE_NOT_FOUND_HEADER);

		Assert.assertEquals(HttpServletResponse.SC_OK, resp.getStatusCode());
		Assert.assertEquals(PHASE_DEFAULT_URL, resp.getContentString());
		
		Assert.assertNotNull(beforeHeader);
		Assert.assertNotNull(afterHeader);
		Assert.assertNull(notFoundHeader);
		
		Assert.assertEquals(PHASE_BEFORE_HEADER, beforeHeader.getValue());
		Assert.assertEquals(PHASE_AFTER_HEADER, afterHeader.getValue());
		
		// GET: NOT FOUND
		resp = sc.doRequest(new HttpGet(PHASE_DEFAULT_URL + "/unknown"));
		beforeHeader = resp.getFirstHeader(PHASE_BEFORE_HEADER);
		afterHeader = resp.getFirstHeader(PHASE_AFTER_HEADER);
		notFoundHeader = resp.getFirstHeader(PHASE_NOT_FOUND_HEADER);
		
		Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, resp.getStatusCode());
		Assert.assertEquals("", resp.getContentString());
		
		Assert.assertNull(beforeHeader);
		Assert.assertNull(afterHeader);
		Assert.assertNotNull(notFoundHeader);
		
		Assert.assertEquals(PHASE_NOT_FOUND_HEADER, notFoundHeader.getValue());
		
		// POST: NOT FOUND
		resp = sc.doRequest(new StringEntityPost("text/plain", "utf-8", PHASE_DEFAULT_URL + "/unknown"));
		beforeHeader = resp.getFirstHeader(PHASE_BEFORE_HEADER);
		afterHeader = resp.getFirstHeader(PHASE_AFTER_HEADER);
		notFoundHeader = resp.getFirstHeader(PHASE_NOT_FOUND_HEADER);
		
		Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, resp.getStatusCode());
		Assert.assertEquals("", resp.getContentString());
		
		Assert.assertNull(beforeHeader);
		Assert.assertNull(afterHeader);
		Assert.assertNotNull(notFoundHeader);
		
		Assert.assertEquals(PHASE_NOT_FOUND_HEADER, notFoundHeader.getValue());
	}
	
	@Test
	public void testPublicActions() {
		StringResponse resp;
		
		// GET: PUBLIC GET
		resp = sc.doRequest(new HttpGet(PUBLIC_GET_URL));
		Assert.assertEquals(HttpServletResponse.SC_OK, resp.getStatusCode());
		Assert.assertEquals(PUBLIC_GET_URL, resp.getContentString());

		expectNullPhaseHeaders(resp);
		
		// GET: PUBLIC MAPPED GET
		resp = sc.doRequest(new HttpGet(PUBLIC_MAPPED_GET_URL));
		Assert.assertEquals(HttpServletResponse.SC_OK, resp.getStatusCode());
		Assert.assertEquals(PUBLIC_MAPPED_GET_URL, resp.getContentString());
		
		expectNullPhaseHeaders(resp);
		
		// POST: PUBLIC POST		
		resp = sc.doRequest(new StringEntityPost("text/plain", "utf-8", PUBLIC_POST_URL));
		Assert.assertEquals(HttpServletResponse.SC_OK, resp.getStatusCode());
		Assert.assertEquals(PUBLIC_POST_URL, resp.getContentString());
		
		expectNullPhaseHeaders(resp);
		
		// POST: PUBLIC MAPPED POST		
		resp = sc.doRequest(new StringEntityPost("text/plain", "utf-8", PUBLIC_MAPPED_POST_URL));
		Assert.assertEquals(HttpServletResponse.SC_OK, resp.getStatusCode());
		Assert.assertEquals(PUBLIC_MAPPED_POST_URL, resp.getContentString());
		
		expectNullPhaseHeaders(resp);
		
		// GET: PUBLIC POST
		resp = sc.doRequest(new HttpGet(PUBLIC_POST_URL));
		Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, resp.getStatusCode());
		
		// GET: PUBLIC MAPPED POST
		resp = sc.doRequest(new HttpGet(PUBLIC_MAPPED_POST_URL));
		Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, resp.getStatusCode());
		
		// POST: PUBLIC GET
		resp = sc.doRequest(new StringEntityPost("text/plain", "utf-8", PUBLIC_GET_URL));
		Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, resp.getStatusCode());
		
		// POST: PUBLIC MAPPED GET
		resp = sc.doRequest(new StringEntityPost("text/plain", "utf-8", PUBLIC_MAPPED_GET_URL));
		Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, resp.getStatusCode());
	}
	
	@Test
	public void testPublicRepeatble() {
		StringResponse resp;
		
		// Multiple @WebAction's...
		// GET:
		resp = sc.doRequest(new HttpGet(PUBLIC_WEBACTIONS_URL));
		Assert.assertEquals(HttpServletResponse.SC_OK, resp.getStatusCode());
		Assert.assertEquals(PUBLIC_WEBACTIONS_URL + "GET", resp.getContentString());
		
		// POST:
		resp = sc.doRequest(new StringEntityPost("text/plain", "utf-8", PUBLIC_WEBACTIONS_URL));
		Assert.assertEquals(HttpServletResponse.SC_OK, resp.getStatusCode());
		Assert.assertEquals(PUBLIC_WEBACTIONS_URL + "POST", resp.getContentString());
		
		
		// Multiple methods, same @WebAction...
		// GET:
		resp = sc.doRequest(new HttpGet(PUBLIC_MULTIPLE_METHODS_URL));
		Assert.assertEquals(HttpServletResponse.SC_OK, resp.getStatusCode());
		Assert.assertEquals(PUBLIC_MULTIPLE_METHODS_URL + "GET", resp.getContentString());
		
		// POST:
		resp = sc.doRequest(new StringEntityPost("text/plain", "utf-8", PUBLIC_MULTIPLE_METHODS_URL));
		Assert.assertEquals(HttpServletResponse.SC_OK, resp.getStatusCode());
		Assert.assertEquals(PUBLIC_MULTIPLE_METHODS_URL + "POST", resp.getContentString());
	}
	// =========================================================================
}
