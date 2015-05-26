package org.jogger;

import org.jogger.http.Request;
import org.jogger.http.Response;

/**
 * Class used for testing by {@link JoggerServletTest}.
 * 
 * @author German Escobar
 */
public abstract class MockController {
	
	public abstract void show(Request request, Response response);
	
}
