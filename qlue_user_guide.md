# Qlue User Guide

Qlue is a lightweight framework for Java web applications. Its main purpose is to provide a structure
in which applications can be easily developed with as little complexity as possible.

Features (some not implemented yet):

- Structure via application, session, and page (request) objects.

- Request routing (static and dynamic) and caching configuration.

- Parameter binding and validation.

- Integration with Apache Velocity.

- Support for stateful operations (e.g., multi-page forms).

  - Built-in redirection after POST.

- Security:

  - Built-in XSS defence (via automatic context-aware output encoding).

  - Built-in CSRF defence (w/token masking for Breach mitigation).

## Getting started

To give you a taste of the simplicity that Qlue offers, in this section we will implement the simplest possible Qlue application. The entire application will consist of one class -- a page in Qlue terminology -- which will print "Hello World" in response to a HTTP request. Here it is, in all its glory:

	package com.example.site.pages;
	
	public class helloWorld extends Page {

		@Override
		public View onGet() throws Exception {
			PrintWriter out = context.response.getWriter();

			out.println("Hello World!");

			return new NullView();
		}
	}

To deploy this page we use QlueServlet to act as brige to a Servlet container. We use the QLUE_PAGES_ROOT_PACKAGE parameter to specify the location of your application pages:

	<web-app xmlns="http://java.sun.com/xml/ns/javaee" version="3.1">
    	<servlet>
        	<servlet-name>QlueServlet</servlet-name>
        		<servlet-class>com.webkreator.qlue.QlueServlet</servlet-class>
				<init-param>
					<param-name>QLUE_PAGES_ROOT_PACKAGE</param-name>
					<param-value>com.example.site.pages</param-value>
				</init-param>
    		</servlet>
    		<servlet-mapping>
        		<servlet-name>QlueServlet</servlet-name>
        		<url-pattern>/*</url-pattern>
    		</servlet-mapping>
	</web-app>

That's all. Now when you start the web server and invoke "/helloWorld" in your browser, you should get "Hello World" back.

## Concepts

### Pages

In Qlue, you write your web application by creating pages that handle HTTP requests. By default, one unique URL maps to one page, and one page is implemented via one Java class. Consider this simple page:

	public class helloWorld extends Page {

		@Override
		public View service() throws Exception {
			PrintWriter out = context.response.getWriter();

			out.println("Hello World!");

			return new NullView();
		}
	}
	
Here's what you should know about page creation:
	
 * To create a page, create a new class inheriting Page
 * The name of the class should correspond to the URL; in the above example, the page will be executed when the path /helloWorld is invoked.
 * Simple pages are stateless; the framework will create a new page instance for each HTTP request.
 * To do something in your page, override the method onGet().
 * In this simple example we output directly to a HTTPS response by working directly with an instance of HttpServletResponse.
 * To indicate to the framework that no further response handling is needed, we return an instance of NullView.
	
#### Page name mapping

By default, case-sensitive comparison is done between the last URL segment and the page name. Thus, only "/helloWorld" will map to the above helloWorld class. If you wish you can have your pages use a suffix externally (e.g., ".html"). In that case, in your application class, invoke setSuffix() on the correct RouteManager. It is also possible to configure the suffix on per-page basis using the QlueMapping annotation.

You can use packages to group pages. For example, if you create a package "books" in your root package, the URL "/books/index" will map to the class com.example.site.pages.books.index.

By default, all pages are reachable. By convention, packages whose names begin with the dollar sign ("$") are considered private. Private packages and pages can't be accessed directly (via URL-to-page mapping), but can be used via internal custom routing. This feature is useful for dynamic URL construction.

Because not all characters are allowed in package and class names, you are limited in how you're creating your external URLs. Additionally, Qlue does not allow dots (".") in the URLs (excluding file suffixes). You can work around these limitations using custom routing, which is described later in this guide. Because dashes ("-") are commonly used in URLs, there's a setting that automatically converts dashes to underscores. This setting is disabled by default; to enable it, invoke RouteManager.setConcertDashesToUnderscores().

#### Responding to specific HTTP methods only

When you override Page.service(), your page will respond to any HTTP method, which is generally not a good idea. Pages usually only need to respond to GET requests. If that's the case, override onGet() insteaf service(). If any other HTTP method is used, Qlue will respond with the 405 status code. The Page class also defines onPost(), but this method is rarely used; it's usually more convenient to use persistent pages, which will be explained later. If you need to respond to arbitrary request methods, override service() and determine course of action by checking the request method.

#### Page state

Page state is an arbitrary string. Some values are reserved for use by the framework and have special
meanings. Other than that, any custom value is possible. The starting state of any page is always NEW.

Non-persistent pages have no use for this field because they terminate after processing one HTTP transaction.

Simper persistent pages also might not care about the state much, because they are typically designed to
collect some data from the user (e.g., using a form) then perform some action. They finish immediately after
the action is carried out.

More complicated persistent pages might consist of multiple forms and can move from one step to another, finally
finishing in the FINISHED state. Qlue generally doesn't care about page states, except in two cases. First, when a
page changes its state to FINISHED, the cleanup() method is invoked. Second, each page parameter can be designed so
that it is updated from HTTP parameters when only on certain states.
	
#### Page processing

Pages execution is split into many methods, with each method designed to serve a specific purpose.
A non-persistent page will typically use the following methods:

 * initBackend() - invoked first, for example to configure database access.
 * checkAccess() - this is an early hook that is invoked before any work is carried out; intended for access control.
 * validateParameters() - invoked after parameter binding and validation. This is an opportunity for the page to
                          perform additional work checking the data.
 * init() - called only once per persistent page, to be used to do some initialisation, for example fetch
            some data from the database.
 * prepareForService() - this method is intended for use when a group of pages share common functionality. Such work
                     can be implemented only once in a parent class, leaving subclasses to focus on the main
                     functionality. This method is called after successful parameter binding and validation.
 * service() - the main page entry point where the main work is done.
 * commit() - executed immediately after the service() method completes. This method should commit
              all the work carried out by the page.
 * rollback() - executed if there is an unhandled exception during page processing. This method should
                undo all work (if any) attempted by the page.
 * cleanup() - called at the end of transaction that transitioned the page to the FINISHED state.

Additional methods of interest:

 * handleValidationError() - this method is called when parameter binding and validation fails, allowing the page to
                             construct a custom view to respond. This method is typically intended for non-persistent
                             pages, which typically use the GET method and which are typically not intended to fail
                             parameter validation. The default implementation will return a 400 status code, but
                             application might want to show a friendly error message. If this method returns null
                             then page processing continues as if there were no errors.

### Views

Although it's possible to write pages that do some work and generate output, in general that's not recommended. Instead, each page should delegate output generation to an instance of View. Qlue is bundled with Apache Velocity, which is a generic templating language. When used with Velocity, it's a page's job to create a set of objects (model), and determine which Velocity template should be invoked to turn the model into a HTTP response.

By convention, Velocity templates use the same name as the pages they're written for. When that's the case, a page can simply return an instance of DefaultView to indicate to the framework that the same-name template should be used:

	return new DefaultView();
	
If a page wants to use a different template, it can indicate that by returning an instance of NamedView:

	return new NamedView("helloWorld");
	
To issue a redirection, return an instance of RedirectView:

	RedirectView rv = new RedirectView("https://elsewhere.example.com");
	rv.addParam("param1", "value1");
	rv.addParam("param2", "value2");
	return rv;
	
### Model

Pages that wish to generate output need to build a model, which is simply a map of named objects. There are two ways to add to the model:

 1. Implicitly, because Qlue will automatically add all public fields of the page to the model.
 
 2. Explicitly, by using Page.addToModel(String name, Object object).

Just before view generation is started, Qlue will automatically add a number of useful objects to the model. The names of these objects start with an underscore to avoid collision with application objects.

| Name    | Description            |
| ----    | -----------            |
| _f      | Formatting helper      |
| _app    | Application            |
| _page   | Page itself            |
| _i      | Shadow input           |
| _ctx    | Qlue context           |
| _sess   | Application session    |
| _m      | Message source         |
| _req    | Servlet HTTP request   |
| _res    | Servlet HTTP response  |
| _cmd    | Command object, if any |
| _errors | Processing errors      |
| _secret | Session CSRF token     |

## Routing

Usually only trivial applications can rely 100% on routing by convention. In all other cases you'll need to use custom routing, defined by editing routes.conf placed in the application's WEB-INF folder. This file borrowed most of the syntax of the routing configuration as used by the Play framework some years ago.

#### Setting response headers

The routes file can be used to set custom HTTP response headers. If a line begins with @header, the rest of the line is interpreted as a custom response header. For example:

	@header Cache-Control no-cache
	
Header configuration directives apply to all routes below themm. Thus, to establish defaults, place your confifuration directives at the top of the routes file. A directive for a header of the same name will overwrite the previous header version; this is useful, for example, to use different caching strategies for different parts of the application.

## Velocity configuration

The default Velocity configuration should be sufficient for most situations. Custom configuration can be deployed programmatically, by building a custom ViewFactory inherting from VelocityViewFactory. Then override and implement tweakVelocityContext().

Qlue supports several configuration parameters that control Velocity:

 * qlue.velocity.cache - controls if template caching is enabled; should be disabled in development and enabled in production. Defaults to false.

 * qlue.velocity.modificationCheckInterval - if caching is enabled, controls the interval between checks for modified templates.
 
 * qlue.velocity.priorityTemplatePath - specifies a priority path on the filesystem from which the templates will be loaded. This feature is intended for use in development when application is run from an IDE.
 
It is possible to configure Velocity directly from Qlue configuration; if there are any properties that start with the "qlue.velocity.raw" prefix they will be passed through unmodified (with the prefix removed) to the Velocity engine as the last step in the configuration process.
 
 VelocityViewFactory will dump Velocity configuration to the log at level INFO just prior to creating an instance of the Velocity engine.
 