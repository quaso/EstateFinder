package sk.kvaso.estate.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import sk.kvaso.estate.EstateStore;
import sk.kvaso.estate.collector.DataCollector;
import sk.kvaso.estate.db.DatabaseUtils;
import sk.kvaso.estate.db.Estate;

@Controller
@RequestMapping("/rest")
public class EstateController {
	private static final Logger log = Logger.getLogger(EstateController.class.getName());

	@Autowired
	private EstateStore store;

	@Autowired
	private DataCollector collector;

	@Autowired
	private DatabaseUtils databaseUtils;

	private void setCORSHeaders(final HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
		response.setHeader("Access-Control-Max-Age", "3600");
		response.setHeader("Access-Control-Allow-Headers", "Content-Type");
		response.setHeader("Access-Control-Allow-Headers", "x-requested-with");
	}

	@RequestMapping(value = "/estates", method = RequestMethod.GET)
	public @ResponseBody WebResponse getEstates(final HttpServletResponse response) {
		return getEstates(response, null);
	}

	@RequestMapping(value = "/estates/{street}", method = RequestMethod.GET)
	public @ResponseBody WebResponse getEstates(final HttpServletResponse response,
			@PathVariable(value = "street") final String street) {
		setCORSHeaders(response);
		final WebResponse result = new WebResponse();

		result.setEstates(new ArrayList<Estate>());
		for (final Estate e : this.store) {
			if (e.isVISIBLE()) {
				if (!StringUtils.isBlank(street)) {
					if (!StringUtils.isEmpty(e.getSTREET())
							&& StringUtils.getJaroWinklerDistance(street, e.getSTREET()) > 0.95) {
						result.getEstates().add(e);
					}
				} else {
					result.getEstates().add(e);
				}
			}
		}

		Collections.sort(result.getEstates(), new Comparator<Estate>() {

			@Override
			public int compare(final Estate e1, final Estate e2) {
				int result = e2.getTIMESTAMP().compareTo(e1.getTIMESTAMP());
				if (result == 0) {
					if (e1.getSTREET() != null && e2.getSTREET() != null) {
						result = e1.getSTREET().compareTo(e2.getSTREET());
					}
					if (result == 0) {
						result = Long.compare(e1.getID(), e2.getID());
					}
				}
				return result;
			}
		});

		result.setLastUpdate(this.collector.getLastScan());
		return result;
	}

	@RequestMapping(value = "/delete/{id}", method = RequestMethod.GET)
	@ResponseStatus(value = HttpStatus.OK)
	public void deleteEstate(final HttpServletResponse response, @PathVariable("id") final int estateId) {
		setCORSHeaders(response);
		for (final Estate e : this.store) {
			if (estateId == e.getID()) {
				e.setVISIBLE(false);
			}
		}
	}

	@RequestMapping(value = "/deleteAll", method = RequestMethod.GET)
	@ResponseStatus(value = HttpStatus.OK)
	public void deleteAll(final HttpServletResponse response) {
		setCORSHeaders(response);
		this.databaseUtils.deleteAll();
	}

	@RequestMapping(value = "/count", method = RequestMethod.GET)
	public @ResponseBody int count(final HttpServletResponse response) {
		setCORSHeaders(response);
		return this.databaseUtils.count();
	}

	@RequestMapping(value = "/collect", method = RequestMethod.GET)
	public @ResponseBody WebResponse collectNew(final HttpServletResponse response) {
		collect(true);
		return getEstates(response);
	}

	@RequestMapping(value = "/collectCron")
	@ResponseStatus(value = HttpStatus.OK)
	public void collectCron() {
		collect(false);
	}

	@RequestMapping(value = "/pause", method = {RequestMethod.GET, RequestMethod.POST})
	@ResponseStatus(value = HttpStatus.OK)
	public void pauseCollecting() {
		this.collector.setPaused(true);
	}

	@RequestMapping(value = "/resume", method = {RequestMethod.GET, RequestMethod.POST})
	@ResponseStatus(value = HttpStatus.OK)
	public void resumeCollecting() {
		this.collector.setPaused(false);
	}

	private void collect(final boolean force) {
		try {
			this.collector.collect(force);
		} catch (final Throwable t) {
			log.severe("Error collecting data: " + t.getMessage());
		}
	}
}
