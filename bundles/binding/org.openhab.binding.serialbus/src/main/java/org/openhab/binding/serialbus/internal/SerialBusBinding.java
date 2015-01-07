/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.serialbus.internal;

import java.io.IOException;
import java.util.Dictionary;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	private String hostName = "raspi2.";
	private int port = 32032;
	private SerialBusSocket socket;

	public SerialBusBinding() {
	}

	@Override
	public void activate() {
		try {
			socket = new SerialBusSocket(hostName, port);
			logger.info("Socket connected to " + hostName + ":" + port);
		} catch (final IOException e) {
			logger.error("Failed to create socket", e);
		}
	}

	@Override
	public void deactivate() {
		try {
			socket = null;
			SerialBusSocket.shutDownInternal();
		} catch (final IOException e) {
			logger.error("Failed to properly shutdown socket", e);
		}
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
					logger.debug(String.format("Got value '%.1f'", value));
					eventPublisher.postUpdate(itemName, new DecimalType(value));
					break;
				case setValue:
					logger.debug(String.format("setValue for '%s'",
							provider.getPath(itemName)));
					logger.error("setValue not (yet) implemented.");
					break;
				default:
					throw new IllegalStateException("Unknown command "
							+ command + " to execute.");
				}
			}
		}
	}

	private static final Pattern PATH_PATTERN = Pattern
			.compile("^/sensor(\\d+)/(.*)$");

	private double retrieveValue(final String path) {
		logger.debug("retrieve value for path '" + path + "'");
		final Matcher matcher = PATH_PATTERN.matcher(path);
		if (matcher.matches()) {
			final int sensorNumber = Integer.parseInt(matcher.group(1));
			final String subPath = matcher.group(2);
			if (subPath.equals("radiatorTemperature")) {
				final String errorMessage = String.format(
						"Failed to read %s from sensor %d", subPath,
						sensorNumber);
				try {
					return RawCommand
							.byteToTemperature(RawCommand.GET_TEMPERATURE
									.execute(socket, sensorNumber));
				} catch (final PortShutDownException e) {
					logger.error(errorMessage, e);
				} catch (final IOException e) {
					logger.error(errorMessage, e);
				} catch (final PortTimeoutException e) {
					logger.error(errorMessage, e);
				}
			} else {
				logger.error("Subpath " + subPath + " not (yet) implemented.");
			}
		} else {
			logger.error("Failed to parse path '" + path + "'.");
		}
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
		logger.debug("internalReceiveUpdate() is called!");
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	public void updated(final Dictionary<String, ?> config)
			throws ConfigurationException {
		if (config != null) {
			final String refreshIntervalString = (String) config.get("refresh");
			if (StringUtils.isNotBlank(refreshIntervalString)) {
				refreshInterval = Long.parseLong(refreshIntervalString);
				logger.info("set refresh interval to " + refreshInterval);
			}
			final String hostNameString = (String) config.get("hostName");
			if (StringUtils.isNotBlank(hostNameString)) {
				hostName = hostNameString;
				logger.info("set hostName to " + hostName);
			}

			final String portString = (String) config.get("port");
			if (StringUtils.isNotBlank(portString)) {
				port = Integer.parseInt(portString);
				logger.info("set port to " + port);
			}

			setProperlyConfigured(true);
		}
	}

}
