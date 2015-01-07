/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.serialbus.internal;

import org.openhab.binding.serialbus.SerialBusBindingProvider;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for parsing the binding configuration.
 *
 * @author holgero
 * @since 0.1.0
 */
public class SerialBusGenericBindingProvider extends
		AbstractGenericBindingProvider implements SerialBusBindingProvider {
	private static final Logger logger = LoggerFactory
			.getLogger(SerialBusGenericBindingProvider.class);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getBindingType() {
		return "serialbus";
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	public void validateItemType(final Item item, final String bindingConfig)
			throws BindingConfigParseException {
		// if (!(item instanceof SwitchItem || item instanceof DimmerItem)) {
		// throw new BindingConfigParseException("item '" + item.getName()
		// + "' is of type '" + item.getClass().getSimpleName()
		// +
		// "', only Switch- and DimmerItems are allowed - please check your *.items configuration");
		// }
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processBindingConfiguration(final String context,
			final Item item, final String bindingConfig)
					throws BindingConfigParseException {
		super.processBindingConfiguration(context, item, bindingConfig);
		logger.debug("processing binding configuration: " + bindingConfig);

		addBindingConfig(item, new SerialBusBindingConfig(bindingConfig));
	}

	class SerialBusBindingConfig implements BindingConfig {
		SerialBusCommand command;
		String path;

		SerialBusBindingConfig(final String bindingConfig)
				throws BindingConfigParseException {
			final String[] strings = bindingConfig.split("=");
			if (strings.length != 2) {
				throw new BindingConfigParseException(
						"Invalid configuration: '" + bindingConfig + "'.");
			}
			try {
				command = SerialBusCommand.valueOf(strings[0]);
			} catch (final IllegalArgumentException e) {
				throw (BindingConfigParseException) new BindingConfigParseException(
						"Invalid configuration: '" + bindingConfig + "'.")
				.initCause(e);
			}
			path = strings[1];
		}
	}

	@Override
	public SerialBusCommand getCommand(final String itemName) {
		if (bindingConfigs.containsKey(itemName)) {
			return ((SerialBusBindingConfig) bindingConfigs.get(itemName)).command;
		}
		return null;
	}

	@Override
	public String getPath(final String itemName) {
		if (bindingConfigs.containsKey(itemName)) {
			return ((SerialBusBindingConfig) bindingConfigs.get(itemName)).path;
		}
		return null;
	}

}
