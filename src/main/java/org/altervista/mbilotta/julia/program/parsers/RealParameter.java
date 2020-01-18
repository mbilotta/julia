package org.altervista.mbilotta.julia.program.parsers;

import org.altervista.mbilotta.julia.Decimal;
import org.altervista.mbilotta.julia.math.Real;


public final class RealParameter extends DecimalParameter {

	private static final long serialVersionUID = 1L;

	final class Validator extends DecimalParameter.Validator {

		public Validator(DescriptorParser descriptorParser,
				XmlPath parameterPath,
				Class<?> pluginType, Object pluginInstance) throws DomValidationException {
			RealParameter.this.super(descriptorParser, parameterPath, pluginType, pluginInstance);
		}

		@Override
		public String getXMLParameterType() {
			return "real";
		}
	}

	public RealParameter(String id) {
		super(id);
	}

	@Override
	Validator createValidator(DescriptorParser descriptorParser,
			XmlPath parameterPath,
			Class<?> pluginType, Object pluginInstance) throws DomValidationException {
		return new Validator(descriptorParser, parameterPath, pluginType, pluginInstance);
	}

	@Override
	public Class<? super Decimal> getType() {
		return Real.class;
	}	
}
