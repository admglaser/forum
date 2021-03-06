package hu.bme.aut.onlab.util;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import hu.bme.aut.onlab.model.Member;
import hu.bme.aut.onlab.model.StyleOfMemberGroup;

/**
 * Created by N. Vilagos.
 */
public class Formatter {

	private static DateTimeFormatter fullDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy hh:mm a", Locale.ENGLISH);
	private static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
	private static final String TODAY = "Today, ";
	private static final String YESTERDAY = "Yesterday, ";
	
	/**
	 * Default time formatter. Uses Today..., Yesterday... and full date.
	 * <br>Examples:
	 * <br>- Today, 09:48 PM
	 * <br>- Yesterday, 09:48 PM
	 * <br>- 06 Jun 2016 11:24 PM
	 * @param timestamp
	 * @return
	 */
	public static String formatTimeStamp(Timestamp timestamp) {
		if (timestamp != null) {
			LocalDateTime localDateTime = timestamp.toLocalDateTime();
			int year = localDateTime.getYear();
			int dayOfYear = localDateTime.getDayOfYear();
			LocalDateTime now = LocalDateTime.now();
			if (year == now.getYear() && dayOfYear == now.getDayOfYear()) {
				return TODAY + localDateTime.format(timeFormatter);
			} else if (year == now.getYear() && dayOfYear == now.getDayOfYear() - 1) {
				return YESTERDAY + localDateTime.format(timeFormatter);
			} else {
				return localDateTime.format(fullDateFormatter);
			}
		}
		else {
			return "";
		}
	}

	/**
	 * Time formatter for messages and notifcations. Uses 59 minutes ago, Today..., Yesterdy.. and full date.
	 * <br>- 36 minutes ago
	 * <br>- Today, 09:48 PM
	 * <br>- Yesterday, 09:48 PM
	 * <br>- 06 Jun 2016 11:24 PM
	 * @param timestamp
	 * @return
	 */
	public static String formatTimeStampForMessage(Timestamp timestamp) {
		if (timestamp != null) {
			LocalDateTime localDateTime = timestamp.toLocalDateTime();
			int year = localDateTime.getYear();
			int dayOfYear = localDateTime.getDayOfYear();
			int hour = localDateTime.getHour();
			int minute = localDateTime.getMinute();
			LocalDateTime now = LocalDateTime.now();
			if (year == now.getYear() && dayOfYear == now.getDayOfYear()) {
				int minutesOfTimeStamp = hour * 60 + minute;
				int minutesAnHourAgo = now.getHour() * 60 + now.getMinute()-60;
				if (minutesOfTimeStamp > minutesAnHourAgo) {
					int m = minutesOfTimeStamp - minutesAnHourAgo;
					if (m == 1) {
						return "1 minutes ago";
					} else {
						return String.format("%d minutes ago", 60-m);
					}
				} else {
					return TODAY + localDateTime.format(timeFormatter);
				}
			} else if (year == now.getYear() && dayOfYear == now.getDayOfYear() - 1) {
				return YESTERDAY + localDateTime.format(timeFormatter);
			} else {
				return localDateTime.format(fullDateFormatter);
			}
		}
		else {
			return "";
		}
	}
	
	public static String getMemberStyle(Member member) {
		StringBuilder sb = new StringBuilder();
		List<StyleOfMemberGroup> styles = member.getMemberGroup().getStyleOfMemberGroups();
		for (int i = 0; i < styles.size(); i++) {
			StyleOfMemberGroup style = styles.get(i);
			sb.append(style.getStyle() + ";");
		}
		return sb.toString();
	}
	
}
