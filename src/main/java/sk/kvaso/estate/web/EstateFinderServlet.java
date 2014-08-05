package sk.kvaso.estate.web;
import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import sk.kvaso.estate.EstateStore;
import sk.kvaso.estate.collector.DataCollector;

import com.google.gson.Gson;

@SuppressWarnings("serial")
public class EstateFinderServlet extends HttpServlet {

	//	@Autowired
	//	private EstateStore store;

	public void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {

		final WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(this
				.getServletContext());
		final EstateStore store = ctx.getBean(EstateStore.class);

		//		final Estate estate = new Estate();
		//		estate.setTEXT("test " + Math.random());
		//		estate.setID(store.size());
		//
		//		store.add(estate);

		resp.setContentType("text/plain");

		ctx.getBean(DataCollector.class).collect();

		final Gson gson = new Gson();

		resp.getWriter().println("Hello. We have " + store.size() + " number of entries.\n" + gson.toJson(store));
	}
}
