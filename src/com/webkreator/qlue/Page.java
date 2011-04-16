/* 
 * Qlue Web Application Framework
 * Copyright 2009,2010 Ivan Ristic <ivanr@webkreator.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.webkreator.qlue;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.webkreator.canoe.HtmlEncoder;
import com.webkreator.qlue.util.UriBuilder;
import com.webkreator.qlue.view.View;
import com.webkreator.qlue.view.ViewResolver;

/**
 * Represents a single unit of work application will perform.
 *
 */
public abstract class Page {

	public static final String STATE_NEW = "NEW";

	public static final String STATE_POST = "POST";

	public static final String STATE_SUBMIT = "SUBMIT";

	public static final String STATE_FINISHED = "FINISHED";

	public static final String STATE_NEW_OR_POST = "NEW_OR_POST";

	private Integer id;

	private String state = STATE_NEW;

	private Log log = LogFactory.getLog(Page.class);

	protected QlueApplication qlueApp;

	private String uri;

	protected TransactionContext context;

	protected Map<String, Object> model = new HashMap<String, Object>();

	protected String view;

	protected String contentType = "text/html; charset=UTF-8";

	protected Object commandObject;

	protected Errors errors = new Errors();

	protected ShadowInput shadowInput = new ShadowInput();

	/**
	 * Has this page finished its work? 
	 * 
	 * @return
	 */
	public boolean isFinished() {
		return (getState().compareTo(STATE_FINISHED) == 0);
	}

	/**
	 * Retrieve unique page ID.
	 * 
	 * @return
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Set page ID. Pages are allocated IDs only prior to
	 * being persisted. Transient pages do not need IDs.
	 *  
	 * @param id
	 */
	void setId(Integer id) {
		this.id = id;
	}

	/**
	 * Update this page's state. In this default implementation, we change from
	 * STATE_NEW to STATE_SUBMIT on first POST and we never move away from
	 * STATE_SUBMIT. An advanced implementation could have several submit states
	 * and offer means to cycle among them.
	 * 
	 * @return
	 */
	void updateState() {
		if (context.isPost()) {
			setState(STATE_SUBMIT);
		}
	}

	/**
	 * Retrieve shadow input associated with page.
	 * 
	 * @return
	 */
	public ShadowInput getShadowInput() {
		return shadowInput;
	}

	/**
	 * Retrieve page state.
	 * 
	 * @return
	 */
	public String getState() {
		return state;
	}

	/**
	 * Change page state to given value.
	 *  
	 * @param state
	 */
	void setState(String state) {
		this.state = state;
	}

	/**
	 * Retrieve the log object used by this page.
	 * 
	 * @return
	 */
	protected Log getLog() {
		return log;
	}

	/**
	 * Retrieve the application to which this page belongs.
	 * 
	 * @return
	 */
	public QlueApplication getQlueApp() {
		return qlueApp;
	}

	/**
	 * Associate Qlue application with this page.
	 * 
	 * @param qlueApp
	 */
	void setQlueApp(QlueApplication qlueApp) {
		this.qlueApp = qlueApp;
	}

	/**
	 * Return a command object. By default, the page is the command object, but
	 * a subclass has the option to use a different object. The page can use the
	 * supplied context to choose which command object (out of several it might
	 * be using) to return.
	 */
	public final synchronized Object getCommandObject() {
		if (commandObject == null) {
			determineCommandObject();
		}

		return commandObject;
	}

	/**
	 * This method will determine what the command object is supposed to be. The
	 * page itself is the default command object, but subclass can override this
	 * behaviour.
	 */
	void determineCommandObject() {
		commandObject = this;
	}

	/**
	 * Process one HTTP request. By default, pages accept only GET 
	 * (and HEAD, treated as GET) and POST.
	 * 
	 * @param context
	 * @return
	 * @throws Exception
	 */
	public View service() throws Exception {
		if ((context.request.getMethod().compareTo("GET") == 0)
				|| (context.request.getMethod().compareTo("HEAD") == 0)) {
			return onGet();
		} else if (context.request.getMethod().compareTo("POST") == 0) {
			return onPost();
		} else {
			throw new RequestMethodException();
		}
	}

	/**
	 * Process a GET request. The default implementation does not actually
	 * do anything -- it just throws an exception.
	 * 
	 * @param context
	 * @return
	 * @throws Exception
	 */
	public View onGet() throws Exception {
		throw new RequestMethodException();
	}

	/**
	 * Process a POST request. The default implementation does not actually
	 * do anything -- it just throws an exception.
	 * 
	 * @param context
	 * @return
	 * @throws Exception
	 */
	public View onPost() throws Exception {
		throw new RequestMethodException();
	}
	
	/**
	 * Retrieve the model associated with a page.
	 * 
	 * @return
	 */
	public Map<String, Object> getModel() {
		return model;
	}
	
	/**
	 * Add key-value pair to the model.
	 * 
	 * @param key
	 * @param value
	 */
	void addToModel(String key, Object value) {
		model.put(key, value);
	}
	
	/**
	 * Retrieve value from the model, using the given key.
	 * 
	 * @param key
	 * @return
	 */
	public Object getFromModel(String key) {
		return model.get(key);
	}

	/**
	 * Retrieve the view associated with page.
	 * 
	 * @return
	 */
	public String getView() {
		return view;
	}

	/**
	 * Retrieve the response content type associated with this page.
	 * 
	 * @return
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * Set response content type.
	 *  
	 * @param contentType
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * Retrieve page transaction context.
	 * 
	 * @return
	 */
	public TransactionContext getContext() {
		return context;
	}

	/**
	 * Set page transaction context.
	 * 
	 * @param context
	 */
	void setContext(TransactionContext context) {
		this.context = context;
	}

	/**
	 * Retrieve the URI that is associated with this page. If the
	 * page was assigned a unique ID (meaning the page is persistent), the
	 * URI will include the ID and thus map back to the page. 
	 * 
	 * @return
	 */
	public String getUri() {		
		if (id == null) {
			// Non-persistent pages can return the original URI
			return uri;
		} else {
			// Persistent pages include page's unique ID
			UriBuilder r = new UriBuilder(uri);
			r.clearParams();
			r.addParam("_pid", id);

			return r.getUri();
		}
	}

	/**
	 * Set page's URI.
	 * 
	 * @param uri
	 */
	void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * Return page's format tool. By default, we respond with application's
	 * format tool, but pages (subclasses) can create their own.
	 * 
	 * @return
	 */
	public Object getFormatTool() {
		return getQlueApp().getFormatTool();
	}

	/**
	 * Construct page's default view, which is constructed from the URI.
	 * 
	 * @param resolver
	 */
	void constructDefaultView(ViewResolver resolver) {
		view = resolver.resolveView(getNoParamUri());
	}

	/**
	 * This method is invoked right before the main service method. It allows
	 * the page to prepare for request processing. The default implementation will,
	 * on POST request, check that there is a nonce value supplied in the request,
	 * and that the value matches the value stored in the session. It will also
	 * expose the nonce to the model.
	 */
	public View preService() throws Exception {
		// Retrieve session nonce
		String nonce = getQlueSession().getNonce();

		// Verify nonce on every POST
		if (context.isPost()
				&& getClass().isAnnotationPresent(QluePersistentPage.class)) {
			String suppliedNonce = context.getParameter("_nonce");
			if (suppliedNonce == null) {
				throw new RuntimeException("Nonce missing.");
			}

			if (suppliedNonce.compareTo(nonce) != 0) {
				throw new RuntimeException("Nonce mismatch. Expected " + nonce
						+ " but got " + suppliedNonce);
			}
		}

		// Add nonce to the model so that it
		// can be used from the templates
		model.put("_nonce", nonce);

		return null;
	}

	/**
	 * Does this page has any parameter validation errors?
	 * 
	 * @return
	 */
	protected boolean hasErrors() {
		return errors.hasErrors();
	}

	/**
	 * Retrieve validation errors.
	 * 
	 * @return
	 */
	public Errors getErrors() {
		return errors;
	}

	/**
	 * Adds a page-specific error message.
	 * 
	 * @param message
	 */
	void addError(String message) {
		errors.addError(message);
	}

	/**
	 * Adds a field-specific error message.
	 * 
	 * @param fieldName
	 * @param message
	 */
	public void addError(String fieldName, String message) {
		errors.addError(fieldName, message);
	}

	/**
	 * Retrieve session associated with this page.
	 *  
	 * @return
	 */
	protected QlueSession getQlueSession() {
		return qlueApp.getQlueSession(context.getRequest());
	}

	public boolean allowDirectOutput() {
		return qlueApp.allowDirectOutput();
	}

	public boolean isDeveloperAccess() {
		return qlueApp.isDeveloperAccess(context);
	}

	public String getNoParamUri() {
		int i = uri.indexOf('?');
		if (i == -1) {
			return uri;
		} else {
			return uri.substring(0, i);
		}
	}

	/**
	 * Outputs page-specific debugging information.
	 * 
	 * @param out
	 */
	void writeDevelopmentInformation(PrintWriter out) {
		out.println(" Id: " + getId());
		out.println(" Class: " + this.getClass());
		out.println(" State: " + HtmlEncoder.encodeForHTML(getState()));
		out.println(" Errors {");

		int i = 1;
		for (Error e : errors.getAllErrors()) {
			out.print("   " + i++ + ". ");
			out.print(HtmlEncoder.encodeForHTML(e.getMessage()));

			if (e.getField() != null) {
				out.print(" [field " + HtmlEncoder.encodeForHTML(e.getField())
						+ "]");
			}

			out.println();
		}

		out.println(" }");
		out.println("");
		out.println("<b>Model</b>\n");

		Map<String, Object> model = getModel();

		for (Iterator<String> it = model.keySet().iterator(); it.hasNext();) {
			String name = it.next();
			Object o = model.get(name);
			out.println(" "
					+ HtmlEncoder.encodeForHTML(name)
					+ ": "
					+ ((o != null) ? HtmlEncoder.encodeForHTML(o.toString())
							: "null"));
		}
	}

	/**
	 * Executes page rollback. The default implementation cleans up resources. 
	 */
	public void rollback() {
		cleanup();
	}

	/**
	 * Executes page commit. The default implementation cleans up resources.
	 */
	public void commit() {
		cleanup();
	}
	
	/**
	 * In the default implementation, we delete any files that were created
	 * during the processing of a multipart/form-data request.
	 */
	void cleanup() {
		deleteFiles();
	}

	/**
	 * TODO
	 * 
	 * @throws Exception
	 */
	public void loadData() throws Exception {
		// This method exists to be overrided in subclasses
	}

	/**
	 * Delete files created by processing multipart/form-data. 
	 */
	void deleteFiles() {
		// Retrieve the command object
		Object commandObject = getCommandObject();
		if (commandObject == null) {
			return;
		}

		// Look for QlueFile instances
		Field[] fields = commandObject.getClass().getFields();
		for (Field f : fields) {
			if (f.isAnnotationPresent(QlueParameter.class)) {
				if (QlueFile.class.isAssignableFrom(f.getType())) {
					try {
						// Delete temporaty file
						QlueFile qf = (QlueFile) f.get(commandObject);
						qf.delete();
					} catch (Exception e) {
						// XXX
						e.printStackTrace(System.err);
					}
				}
			}
		}
	}

	/**
	 * Is page persistent?
	 * 
	 * @return
	 */
	public boolean isPersistent() {
		return getClass().isAnnotationPresent(QluePersistentPage.class);
	}

	/**
	 * This method is invoked after built-in parameter validation fails. The
	 * default implementation will throw an exception for non-persistent pages,
	 * and ignore the problem for persistent pages.
	 * 
	 * @return
	 * @throws Exception
	 */
	View onValidationError() throws Exception {
		if (isPersistent() == true) {
			// Let the page handle validation errors
			return null;
		}

		// Report fatal error
		throw new ValidationException("Parameter validation failed");
	}
}
