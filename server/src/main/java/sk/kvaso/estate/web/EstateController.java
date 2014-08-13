package sk.kvaso.estate.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import sk.kvaso.estate.EstateStore;
import sk.kvaso.estate.collector.DataCollector;
import sk.kvaso.estate.db.AppState;
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

	@Autowired
	private AppState appState;

	private void setCORSHeaders(final HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
		response.setHeader("Access-Control-Max-Age", "3600");
		response.setHeader("Access-Control-Allow-Headers", "Content-Type");
		response.setHeader("Access-Control-Allow-Headers", "x-requested-with");
	}

	@RequestMapping(value = "/estates", method = RequestMethod.GET)
	public @ResponseBody WebResponse getEstates(final HttpServletRequest request, final HttpServletResponse response) {
		return getEstates(request, response, null);
	}

	@RequestMapping(value = "/search/{street}", method = RequestMethod.GET)
	public @ResponseBody WebResponse getEstates(final HttpServletRequest request, final HttpServletResponse response,
			@PathVariable(value = "street") final String streets) {
		setCORSHeaders(response);
		final WebResponse result = new WebResponse();

		final boolean searchForStreet = !StringUtils.isBlank(streets);
		String[] splittedStreets = null;
		if (searchForStreet) {
			log.info("Searching for streets '" + streets + "'");
			splittedStreets = streets.split(",");
		}

		this.databaseUtils.loadAppState(this.appState);

		result.setNewEstatesCount(0);

		result.setEstates(new ArrayList<Estate>());
		for (final Estate e : this.store) {
			if (e.isVISIBLE()) {
				boolean add = false;
				if (searchForStreet) {
					if (!StringUtils.isEmpty(e.getSTREET())) {
						for (final String street : splittedStreets) {
							if (StringUtils.getJaroWinklerDistance(street, e.getSTREET()) > 0.95) {
								add = true;
								break;
							}
						}
					}
				} else {
					add = true;
				}
				if (add) {
					result.getEstates().add(e);
					if (this.appState.getLastView() == null || e.getTIMESTAMP().after(this.appState.getLastView())) {
						result.setNewEstatesCount(result.getNewEstatesCount() + 1);
					}
				}
			}
		}

		Collections.sort(result.getEstates(), new Comparator<Estate>() {

			@Override
			public int compare(final Estate e1, final Estate e2) {
				final Date lastView = EstateController.this.appState.getLastView();
				int result;
				if (EstateController.this.store != null && lastView != null && lastView.before(e1.getTIMESTAMP())
						&& lastView.after(e2.getTIMESTAMP())) {
					return -1;
				} else if (lastView != null
						&& lastView.after(e1.getTIMESTAMP())
						&& lastView.before(e2.getTIMESTAMP())) {
					return 1;
				} else {
					final String s1 = (e1.getSTREET() != null) ? e1.getSTREET() : "";
					final String s2 = (e2.getSTREET() != null) ? e2.getSTREET() : "";
					result = s1.compareTo(s2);
					if (result == 0) {
						result = Integer.compare(e1.getAREA(), e2.getAREA());
					}
				}
				return result;
			}
		});

		result.setLastView(this.appState.getLastView());

		this.appState.setLastView(new Date());

		this.databaseUtils.saveAppState(this.appState);

		result.setLastUpdate(this.appState.getLastScan());
		result.setStreets(this.store.getStreets());
		return result;
	}

	@RequestMapping(value = "/hide/{ids}", method = RequestMethod.GET)
	@ResponseStatus(value = HttpStatus.OK)
	public void hideEstates(final HttpServletResponse response, @PathVariable("ids") final long[] estateIds) {
		setCORSHeaders(response);
		for (final Estate e : this.store) {
			if (ArrayUtils.contains(estateIds, e.getID())) {
				e.setVISIBLE(false);
			}
		}
	}

	@RequestMapping(value = "/save", method = RequestMethod.GET)
	public ResponseEntity<String> save(final HttpServletResponse response) {
		try {
			this.databaseUtils.saveEstates(this.store);
		} catch (final Exception ex) {
			log.severe(DatabaseUtils.printStackTrace(ex));
			return new ResponseEntity<String>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<String>(HttpStatus.OK);
	}

	@RequestMapping(value = "/deleteAll", method = RequestMethod.GET)
	@ResponseStatus(value = HttpStatus.OK)
	public void deleteAll(final HttpServletResponse response) {
		setCORSHeaders(response);
		this.databaseUtils.deleteAllEstates(this.store);
		this.databaseUtils.clearAppState(this.appState);
	}

	@RequestMapping(value = "/prepareRecollect", method = RequestMethod.GET)
	@ResponseStatus(value = HttpStatus.OK)
	public void clearState(final HttpServletResponse response) {
		setCORSHeaders(response);
		this.databaseUtils.clearAppState(this.appState);
	}

	@RequestMapping(value = "/count", method = RequestMethod.GET)
	public @ResponseBody int count(final HttpServletResponse response) {
		setCORSHeaders(response);
		return this.databaseUtils.count();
	}

	@RequestMapping(value = "/collect", method = RequestMethod.GET)
	public @ResponseBody WebResponse collectNew(final HttpServletRequest request, final HttpServletResponse response) {
		collect(true);
		return getEstates(request, response);
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
			t.printStackTrace();
			log.severe("Error collecting data: " + t.getMessage());
		}
	}
}
