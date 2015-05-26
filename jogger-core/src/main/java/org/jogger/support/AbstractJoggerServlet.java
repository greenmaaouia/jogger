package org.jogger.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import javax.servlet.http.HttpServlet;

import org.jogger.config.ConfigurationException;
import org.jogger.config.Interceptors;
import org.jogger.http.Request;
import org.jogger.http.Response;
import org.jogger.interceptor.Action;
import org.jogger.interceptor.Controller;
import org.jogger.interceptor.Interceptor;
import org.jogger.interceptor.InterceptorExecution;
import org.jogger.router.Route;

import freemarker.template.Configuration;

/**
 * Support class that acts as a base class for {@link org.jogger.JoggerServlet} and 
 * {@link org.jogger.test.MockJoggerServlet} providing common functionality. It extends from HttpServlet because Java 
 * only allows to extend from one class.
 * 
 * @author German Escobar
 */
public abstract class AbstractJoggerServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	/**
	 * The FreeMarker configuration
	 */
	private Configuration freemarker;

	/**
	 * Service the request using our {@link Request} and {@link Response} objects. It's public so we can test it.
	 * 
	 * @param request
	 * @param response
	 * 
	 * @throws RouteNotFoundException
	 * @throws Exception
	 */
	public void service(Route route, Request request, Response response) throws Exception {
		
		// load the interceptors of the request
		List<Interceptor> requestInterceptors = getInterceptors().getInterceptors(request.getPath());
			
		// execute the controller
		ControllerExecutor controllerExecutor = new ControllerExecutor(route, request, response, 
				requestInterceptors);   
		controllerExecutor.proceed();
			
		// clean
		route = null;
	}
	
	/**
	 * This is a helper class that executes the interceptor chain and calls the controller. Notice that this class 
	 * uses recursion to call each interceptor and the controller. It uses an index to keep track of the next 
	 * interceptor to be executed. Finally, it calls the controller.
	 * 
	 * @author German Escobar
	 */
	private class ControllerExecutor implements InterceptorExecution {
		
		private Route route;
		
		private Request request;
		
		private Response response;
		
		private List<Interceptor> interceptors;
		
		private int index = 0;
		
		/**
		 * Constructor. Initializes the object with the specified parameters.
		 * 
		 * @param route holds the controller class and the action method.
		 * @param request an object that represents the current HTTP request.
		 * @param response an object that represents the current HTTP response.
		 * @param interceptors a list of interceptors that we need to execute before calling the action.
		 */
		public ControllerExecutor(Route route, Request request, Response response, List<Interceptor> interceptors) {
			this.route = route;
			this.request = request;
			this.response = response;
			this.interceptors = interceptors;
		}

		@Override
		public void proceed() throws Exception {
			
			// if we finished executing all the interceptors, call the controller method
			if (index == interceptors.size()) {
				
				Object controller = route.getController();
				Method method = route.getAction();
				
				method.invoke(controller, request, response);
				
				return;
			}
			
			// retrieve the interceptor and increase the index 
			Interceptor interceptor = interceptors.get(index);
			index++;
			
			// execute the interceptor - notice that the interceptor can eventually call the proceed() method recursively.
			interceptor.intercept(request, response, this);
			
		}

		@Override
		public Controller getController() {
			
			// create and return a new instance of the Controller class
			return new Controller() {
				public <A extends Annotation> A getAnnotation(Class<A> annotation) {
					return route.getController().getClass().getAnnotation(annotation);
				}
			};
			
		}

		@Override
		public Action getAction() {
			
			// create and return a new instance of the Action class
			return new Action() {
				public <A extends Annotation> A getAnnotation(Class<A> annotation) {
					return route.getAction().getAnnotation(annotation);
				}
			};
			
		}
		
	}
	
	/**
	 * Returns the FreeMarker configuration that the Servlet will use to render templates.
	 * 
	 * @return the FreeMarker {@link freemarker.template.Configuration} object.
	 * @throws ConfigurationException if there is a problem creating the {@link freemarker.template.Configuration} object.
	 */
	public Configuration getFreeMarker() throws ConfigurationException {
		
		if (freemarker != null) {
			return freemarker;
		}
		
		freemarker = buildFreeMarker();
		
		return freemarker;
	}
	
	/**
	 * Configure the FreeMarker configuration object. Implementations of this method <strong>must</strong>, at least,
	 * call one of the following:
	 * 
	 * <pre>
	 * 	freemarker.setServletContextForTemplateLoading(servletContext, templatesPath);
	 * 	// or
	 * 	freemarker.setDirectoryForTemplateLoading(new File(templatesPath));
	 * 	// or
	 * 	freemarker.setClassForTemplateLoading(Some.class, templatesPath);
	 * <pre>
	 * 
	 * @param freemarker
	 */
	protected abstract Configuration buildFreeMarker();
	
	/**
	 * Must return the interceptors that the Servlet will use to intercept requests.
	 * 
	 * @return an initialized {@link Interceptors} implementation. 
	 * 
	 * @throws ConfigurationException if there is a problem creating the {@link Interceptors} implementation.
	 */
	protected abstract Interceptors getInterceptors() throws ConfigurationException;
	
}
