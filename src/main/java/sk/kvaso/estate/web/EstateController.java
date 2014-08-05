package sk.kvaso.estate.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.servlet.view.RedirectView;

import sk.kvaso.estate.EstateStore;
import sk.kvaso.estate.collector.DataCollector;
import sk.kvaso.estate.model.Estate;

import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

@Controller
public class EstateController {

	@Autowired
	private EstateStore store;

	@Autowired
	private DataCollector collector;

	@RequestMapping(value = "/{estateId}", method = RequestMethod.GET)
	public ModelAndView getEstates(@PathVariable final long estateId) {
		final Map<String, Object> model = new HashMap<String, Object>();
		final List<Estate> result = new ArrayList<>();
		for (final Estate e : this.store) {
			if (e.getID() == estateId) {
				result.add(e);
				break;
			}
		}
		model.put("estates", result);
		model.put("lastScan", this.collector.getLastScan());

		final View view = new InternalResourceView("/WEB-INF/jsp/estates.jsp");
		return new ModelAndView(view, model);
	}

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public ModelAndView getEstates() {
		final Map<String, Object> model = new HashMap<String, Object>();

		final List<Estate> result = new ArrayList<>();
		for (final Estate e : this.store) {
			if (e.isVISIBLE()) {
				result.add(e);
			}
		}

		Collections.sort(result, new Comparator<Estate>() {

			@Override
			public int compare(final Estate e1, final Estate e2) {
				return e1.getTIMESTAMP().compareTo(e2.getTIMESTAMP());
			}
		});

		model.put("estates", result);
		model.put("lastScan", this.collector.getLastScan());

		final View view = new InternalResourceView("/WEB-INF/jsp/estates.jsp");
		return new ModelAndView(view, model);
	}

	@RequestMapping(value = "/", method = RequestMethod.POST)
	public RedirectView getEstates(@RequestParam(value = "selected", required = false) final String[] selected,
			@RequestParam("type") final String type) {
		switch (type) {
			case "Delete" :
				if (!ArrayUtils.isEmpty(selected)) {
					for (final Estate e : this.store) {
						if (ArrayUtils.contains(selected, String.valueOf(e.getID()))) {
							e.setVISIBLE(false);
						}
					}
				}
				break;
			case "CollectNew" :
				collect();
				break;
		}

		return new RedirectView("/");
	}

	@RequestMapping(value = "/collect", method = RequestMethod.GET)
	public RedirectView collectGet() {
		collect();
		return new RedirectView("/");
	}

	@RequestMapping(value = "/collectCron", method = {RequestMethod.GET, RequestMethod.POST})
	@ResponseStatus(value = HttpStatus.OK)
	public void collectCron() {
		collect();
		QueueFactory.getDefaultQueue().add(
				TaskOptions.Builder.withUrl("/collectCron").countdownMillis(TimeUnit.MINUTES.toMillis(10)));
	}

	private void collect() {
		this.collector.collect();
	}
}
