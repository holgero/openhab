/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.serialbus.internal;

import java.util.Dictionary;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.serialbus.SerialBusBindingProvider;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement this class if you are going create an actively polling service like
 * querying a Website/Device.
 *
 * @author holgero
 * @since 0.1.0
 */
public class SerialBusBinding extends
		AbstractActiveBinding<SerialBusBindingProvider> implements
		ManagedService {

	private static final Logger logger = LoggerFactory
			.getLogger(SerialBusBinding.class);

	/**
	 * the refresh interval which is used to poll values from the SerialBus
	 * server (optional, defaults to 60000ms)
	 */
	private long refreshInterval = 60000;

	public SerialBusBinding() {
	}

	@Override
	public void activate() {
	}

	@Override
	public void deactivate() {
		// deallocate resources here that are no longer needed and
		// should be reset when activating this binding again
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected long getRefreshInterval() {
		return refreshInterval;
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected String getName() {
		return "SerialBus Refresh Service";
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected void execute() {
		for (final SerialBusBindingProvider provider : providers) {
			logger.debug("polling thread running for provider " + provider);
			for (final String itemName : provider.getItemNames()) {
				logger.debug("polling thread running for item " + itemName);
				final SerialBusCommand command = provider.getCommand(itemName);
				switch (command) {
				case getValue:
					final double value = retrieveValue(provider
							.getPath(itemName));
					eventPublisher.postUpdate(itemName, new DecimalType(value));
					break;
				case setValue:
					break;
				default:
					throw new IllegalStateException("Unknown command "
							+ command + " to execute.");
				}
			}
		}
	}

	private double retrieveValue(final String path) {
		logger.debug("retrieve value for path '" + path + "'");
		return 0;
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected void internalReceiveCommand(final String itemName,
			final Command command) {
		// the code being executed when a command was sent on the openHAB
		// event bus goes here. This method is only called if one of the
		// BindingProviders provide a binding for the given 'itemName'.
		logger.debug("internalReceiveCommand() is called!");
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected void internalReceiveUpdate(final String itemName,
			final State newState) {
		// the code being executed when a state was sent on the openHAB
		// event bus goes here. This method is only called if one of the
		// BindingProviders provide a binding for the given 'itemName'.
		logger.debug("internalReceiveCommand() is called!");
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	public void updated(final Dictionary<String, ?> config)
			throws ConfigurationException {
		if (config != null) {

			// to override the default refresh interval one has to add a
			// parameter to openhab.cfg like
			// <bindingName>:refresh=<intervalInMs>
			final String refreshIntervalString = (String) config.get("refresh");
			if (StringUtils.isNotBlank(refreshIntervalString)) {
				refreshInterval = Long.parseLong(refreshIntervalString);
				logger.info("set refresh interval to " + refreshInterval);
			}

			// read further config parameters here ...

			setProperlyConfigured(true);
		}
	}

}
