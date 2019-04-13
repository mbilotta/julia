/*
 * Copyright (C) 2015 Maurizio Bilotta.
 * 
 * This file is part of Julia. See <http://mbilotta.altervista.org/>.
 * 
 * Julia is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Julia is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Julia. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.altervista.mbilotta.julia;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.altervista.mbilotta.julia.math.Real;


public final class Decimal extends PartialReal implements Serializable {
	
	public static final Decimal ZERO;

	private static final long serialVersionUID = 1L;

	private static final Pattern regex;
	
	private final int signum;
	private final String mantissa;
	private final int exponentSignum;
	private final String exponent;

	static {
		String optionalSignum = "(?<signum>[+-])?";
		String integerOrDecimal = "0*(?<integerPart>[0-9]+)(\\.(?<fractionalPart1>[0-9]*[1-9])?0*)?";
		String decimal = "\\.((?<fractionalPart2>[0-9]*[1-9])0*|0+)";
		String optionalExponent = "([eE](?<exponentSignum>[+-])?0*(?<exponent>[0-9]+))?";
		regex = Pattern.compile(optionalSignum + '(' + integerOrDecimal + '|' + decimal + ')' + optionalExponent);
		ZERO = new Decimal(0);
	}

	public Decimal(int n) {
		int signum = Integer.signum(n);
		if (signum == 0) {
			this.mantissa = null;
			this.exponentSignum = 0;
			this.exponent = null;
		} else {
			String mantissa = removeLeadingSign(Integer.toString(n));
			int exponent = mantissa.length() - 1;
			this.mantissa = removeTrailingZeroes(mantissa);
			if (exponent == 0) {
				this.exponentSignum = 0;
				this.exponent = null;
			} else {
				this.exponentSignum = 1;
				this.exponent = Integer.toString(exponent);
			}
		}
		this.signum = signum;
	}

	public Decimal(String string) {
		Matcher matcher = regex.matcher(string);
		if (!matcher.matches()) {
			throw new NumberFormatException(string);
		}

		String signum = matcher.group("signum");
		String integerPart = matcher.group("integerPart");
		String fractionalPart1 = matcher.group("fractionalPart1");
		String fractionalPart2 = matcher.group("fractionalPart2");
		String exponentSignum = matcher.group("exponentSignum");
		String exponent = matcher.group("exponent");

		Object[] data;
		if (integerPart == null) {
			if (fractionalPart2 == null) {
				data = new Object[] { 0, null, 0, null};
			} else {
				data = build(signum, fractionalPart2, exponentSignum, exponent);
			}
		} else {
			if (integerPart.charAt(0) == '0') {
				if (fractionalPart1 == null) {
					data = new Object[] { 0, null, 0, null};
				} else {
					data = build(signum, fractionalPart1, exponentSignum, exponent);
				}
			} else {
				data = build(signum, integerPart, fractionalPart1, exponentSignum, exponent);
			}
		}

		this.signum = (Integer) data[0];
		this.mantissa = (String) data[1];
		this.exponentSignum = (Integer) data[2];
		this.exponent = (String) data[3];
	}

	private Decimal(int signum, String mantissa, int exponentSignum, String exponent) {
		this.signum = signum;
		this.mantissa = mantissa;
		this.exponentSignum = exponentSignum;
		this.exponent = exponent;
	}

	private static Object[] build(String signum, String fractionalPart, String exponentSignum, String exponent) {
		Object[] rv = new Object[4];

		rv[0] = signum == null ? 1 : (signum.charAt(0) == '+' ? 1 : -1);

		int leadingZeroes = 0;
		while (fractionalPart.charAt(leadingZeroes) == '0') {
			leadingZeroes++;
		}
		rv[1] = fractionalPart.substring(leadingZeroes);

		int dotShifts = leadingZeroes + 1;

		if (exponent == null || exponent.charAt(0) == '0') {
			rv[2] = -1;
			rv[3] = Integer.toString(dotShifts);
		} else {
			BigInteger e = new BigInteger(exponent);
			if (exponentSignum != null && exponentSignum.charAt(0) == '-') {
				e = e.negate();
			}
			e = e.subtract(BigInteger.valueOf(dotShifts));

			rv[2] = e.signum();
			if (e.signum() != 0) {
				rv[3] = e.abs().toString();
			}
		}

		return rv;
	}

	private static Object[] build(String signum, String integerPart, String fractionalPart, String exponentSignum, String exponent) {
		Object[] rv = new Object[4];

		rv[0] = signum == null ? 1 : (signum.charAt(0) == '+' ? 1 : -1);

		int dotShifts = integerPart.length() - 1;
		if (dotShifts == 0) {
			rv[1] = fractionalPart == null ? integerPart : integerPart.concat(fractionalPart);
			if (exponent != null && exponent.charAt(0) != '0') {
				rv[2] = exponentSignum == null ? 1 : (exponentSignum.charAt(0) == '+' ? 1 : -1);
				rv[3] = exponent;
			} else {
				rv[2] = 0;
			}
		} else {
			if (fractionalPart == null) {
				int trailingZeroes = 0;
				for (int i = integerPart.length() - 1; i > 0 && integerPart.charAt(i) == '0'; i--) {
					trailingZeroes++;
				}
				rv[1] = integerPart.substring(0, integerPart.length() - trailingZeroes);
			} else {
				rv[1] = integerPart.concat(fractionalPart);
			}

			if (exponent == null || exponent.charAt(0) == '0') {
				rv[2] = 1;
				rv[3] = Integer.toString(dotShifts);
			} else {
				BigInteger e = new BigInteger(exponent);
				if (exponentSignum != null && exponentSignum.charAt(0) == '-') {
					e = e.negate();
				}
				e = e.add(BigInteger.valueOf(dotShifts));

				rv[2] = e.signum();
				if (e.signum() != 0) {
					rv[3] = e.abs().toString();
				}
			}
		}

		return rv;
	}

	private static String removeLeadingSign(String number) {
		return number.charAt(0) == '-' ? number.substring(1) : number;
	}

	private static String removeTrailingZeroes(String number) {
		int newLength = number.length();
		while (number.charAt(newLength - 1) == '0') newLength--;
		return number.substring(0, newLength);
	}

	@Override
	public Decimal abs() {
		return signum < 0 ? new Decimal(-signum, mantissa, exponentSignum, exponent) : this;
	}

	@Override
	public Decimal negate() {
		return signum != 0 ? new Decimal(-signum, mantissa, exponentSignum, exponent) : this;
	}

	public int getSignum() {
		return signum;
	}

	public String getMantissa() {
		return mantissa;
	}

	public int getExponentSignum() {
		return exponentSignum;
	}

	public String getExponent() {
		return exponent;
	}

	@Override
	public int intValue() {
		if (signum == 0 || exponentSignum < 0) {
			return 0;
		}
		if (exponentSignum == 0) {
			return Integer.parseInt(mantissa);
		}
		if (exponent.length() > 1) {
			return signum > 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
		}

		int intPartLen = Integer.parseInt(exponent) + 1;
		String intPart;
		if (mantissa.length() > intPartLen) {
			intPart = mantissa.substring(0, intPartLen);
		} else {
			intPart = padMantissa(mantissa, intPartLen);
		}

		if (intPart.length() < 10) {
			return signum * Integer.parseInt(intPart);
		} else {
			try {
				return Integer.parseInt((signum > 0 ? "" : "-") + intPart);
			} catch (NumberFormatException e) {
				return signum > 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
			}
		}
	}

	@Override
	public float floatValue() {
		return Float.parseFloat(toNormalizedString());
	}

	@Override
	public double doubleValue() {
		return Double.parseDouble(toNormalizedString());
	}

	public BigDecimal bigDecimalValue() {
		return new BigDecimal(toNormalizedString());
	}

	public int compareTo(Decimal d) {
		if (signum == 0 && d.signum == 0)
			return 0;

		if (signum != d.signum)
			return signum - d.signum;

		if (exponentSignum != d.exponentSignum)
			return signum * (exponentSignum - d.exponentSignum);

		if (!(exponentSignum == 0 && d.exponentSignum == 0)) {
			int result = exponent.length() - d.exponent.length();
			if (result == 0) {
				result = exponent.compareTo(d.exponent);
			}
			if (result != 0) {
				return signum * exponentSignum * result;				
			}
		}

		int result = mantissa.compareTo(d.mantissa);

		return signum * result;
	}

	@Override
	public int compareTo(Real r) {
		return compareTo((Decimal) r);
	}

	@Override
	public boolean lt(int i) {
		return lt(new Decimal(i));
	}

	@Override
	public boolean lte(int i) {
		return lte(new Decimal(i));
	}

	@Override
	public boolean gt(int i) {
		return gt(new Decimal(i));
	}

	@Override
	public boolean gte(int i) {
		return gte(new Decimal(i));
	}

	@Override
	public boolean eq(int i) {
		return eq(new Decimal(i));
	}

	private static String padMantissa(String mantissa, int length) {
		if (mantissa.length() == length)
			return mantissa;
		
		StringBuilder buffer = new StringBuilder(length);
		buffer.append(mantissa);
		int zeroes = length - mantissa.length();
		for (int i = 0; i < zeroes; i++) {
			buffer.append('0');
		}
		return buffer.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;

		if (o instanceof Decimal) {
			Decimal d = (Decimal) o;
			return (signum == 0 && d.signum == 0) || (
						signum == d.signum && mantissa.equals(d.mantissa) && (
								(exponentSignum == 0 && d.exponentSignum == 0) ||
								(exponentSignum == d.exponentSignum && exponent.equals(d.exponent))
						)
					);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return signum * Objects.hash(exponentSignum, exponentSignum == 0 ? null : exponent, signum == 0 ? null : mantissa);
	}

	@Override
	public String toString() {
		if (signum == 0)
			return "0";

		StringBuilder buffer = new StringBuilder();
		if (signum < 0) buffer.append('-');

		if (exponentSignum == 0) {
			buffer.append(mantissa.charAt(0));
			if (mantissa.length() > 1) {
				buffer.append('.')
					.append(mantissa, 1, mantissa.length());
			}
		} else {
			BigInteger e = new BigInteger(exponent);
			if (exponentSignum > 0){
				int result = e.compareTo(BigInteger.valueOf(mantissa.length() - 1));
				if (result < 0) {
					int integerPartEnd = e.intValue() + 1;
					buffer.append(mantissa, 0, integerPartEnd)
						.append('.')
						.append(mantissa, integerPartEnd, mantissa.length());
				} else if (result == 0) {
					buffer.append(mantissa);
				} else {
					int minExponent = 10;
					if (e.compareTo(BigInteger.valueOf(minExponent)) < 0) {
						int zeroes = e.intValue() - mantissa.length() + 1;
						buffer.append(mantissa);
						for (int i = 0; i < zeroes; i++) {
							buffer.append('0');
						}
					} else {
						buffer.append(mantissa.charAt(0))
							.append('.')
							.append(mantissa, 1, mantissa.length())
							.append('E')
							.append('+')
							.append(exponent);
					}
				}
			} else {
				int minExponent = 3;
				if (e.compareTo(BigInteger.valueOf(minExponent)) < 0) {
					buffer.append('0')
						.append('.');
					int zeroes = e.intValue() - 1;
					for (int i = 0; i < zeroes; i++) {
						buffer.append('0');
					}
					buffer.append(mantissa);
				} else {
					buffer.append(mantissa.charAt(0))
						.append('.')
						.append(mantissa, 1, mantissa.length())
						.append('E')
						.append('-')
						.append(exponent);
				}
			}
		}

		return buffer.toString();
	}

	public String toNormalizedString() {
		if (signum == 0)
			return "0";

		StringBuilder buffer = new StringBuilder();
		buffer.append(signum > 0 ? '+' : '-')
			.append(mantissa.charAt(0))
			.append('.')
			.append(mantissa, 1, mantissa.length());
		
		if (exponentSignum != 0) {
			buffer.append('E')
				.append(exponentSignum > 0 ? '+' : '-')
				.append(exponent);
		}

		return buffer.toString();
	}

	private void readObject(ObjectInputStream in)
			throws ClassNotFoundException, IOException {
		in.defaultReadObject();
		if (signum < -1 || signum > 1)
			throw new InvalidObjectException("[signum=" + signum + "] out of range");
		if (exponentSignum < -1 || exponentSignum > 1)
			throw new InvalidObjectException("[exponentSignum=" + exponentSignum + "] out of range");
		if (signum != 0 && mantissa == null)
			throw new InvalidObjectException("[mantissa=null] allowed only when [signum=" + signum + "] is 0");
		if (exponentSignum != 0 && exponent == null)
			throw new InvalidObjectException("[exponent=null] allowed only when [exponentSignum=" + exponentSignum + "] is 0");

		if (mantissa != null) {
			char mfirst = mantissa.charAt(0);
			char mlast = mantissa.charAt(mantissa.length() - 1);
			if (mfirst < '1' || mfirst > '9' || mlast < '1' || mlast > '9' || notOnlyDigits(mantissa, 1, mantissa.length() - 1))
				throw new InvalidObjectException("[mantissa=" + mantissa + "] does not have the form [1-9]([0-9]*[1-9])?");
		}

		if (exponent != null) {
			char efirst = exponent.charAt(0);
			if (efirst < '1' || efirst > '9' || notOnlyDigits(exponent, 1, exponent.length()))
				throw new InvalidObjectException("[exponent=" + exponent + "] does not have the form [1-9][0-9]*");	
		}
	}

	private static boolean notOnlyDigits(String string, int start, int end) {
		for (int i = start; i < end; i++) {
			char c = string.charAt(i);
			if (c < '0' || c > '9')
				return true;
		}
		return false;
	}
}
