package org.coolreader.crengine;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

final class FeedTimestampParser {
	private static final int UTC_LENGTH = 20;
	private static final int COMPACT_OFFSET_LENGTH = 24;
	private static final int COLON_OFFSET_LENGTH = 25;

	private FeedTimestampParser() {
	}

	static long parse(String timestamp) {
		if (timestamp == null)
			return 0;
		String value = timestamp.trim();
		SimpleDateFormat parser;
		if (value.length() == UTC_LENGTH
				&& value.charAt(value.length() - 1) == 'Z') {
			parser = new SimpleDateFormat(
					"yyyy-MM-dd'T'HH:mm:ss'Z'",
					Locale.US);
			parser.setTimeZone(TimeZone.getTimeZone("UTC"));
		} else {
			if (value.length() == COLON_OFFSET_LENGTH
					&& value.charAt(22) == ':') {
				value = value.substring(0, 22)
						+ value.substring(23);
			}
			if (value.length() != COMPACT_OFFSET_LENGTH
					|| !hasValidOffset(value))
				return 0;
			parser = new SimpleDateFormat(
					"yyyy-MM-dd'T'HH:mm:ssZ",
					Locale.US);
		}
		parser.setLenient(false);
		ParsePosition position = new ParsePosition(0);
		Date parsed = parser.parse(value, position);
		if (parsed == null || position.getIndex() != value.length())
			return 0;
		return parsed.getTime();
	}

	private static boolean hasValidOffset(String value) {
		char sign = value.charAt(19);
		if (sign != '+' && sign != '-')
			return false;
		for (int i = 20; i < 24; i++) {
			if (!Character.isDigit(value.charAt(i)))
				return false;
		}
		int hour = (value.charAt(20) - '0') * 10
				+ value.charAt(21) - '0';
		int minute = (value.charAt(22) - '0') * 10
				+ value.charAt(23) - '0';
		return hour <= 23 && minute <= 59;
	}
}
