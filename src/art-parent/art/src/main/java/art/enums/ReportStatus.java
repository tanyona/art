/*
 * Copyright (C) 2014 Enrico Liboni <eliboni@users.sourceforge.net>
 *
 * This file is part of ART.
 *
 * ART is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2 of the License.
 *
 * ART is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * ART. If not, see <http://www.gnu.org/licenses/>.
 */
package art.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Enum for report status
 *
 * @author Timothy Anyona
 */
public enum ReportStatus {

	//enum values will be stored in the database so avoid changing
	//or update existing database records with the new value, and update i18n strings
	Active("Active"), Disabled("Disabled"), Hidden("Hidden");
	private String value;

	private ReportStatus(String value) {
		this.value = value;
	}

	/**
	 * Get enum value
	 *
	 * @return
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Get a list of all enum values
	 *
	 * @return
	 */
	public static List<ReportStatus> list() {
		//use a new list as Arrays.asList() returns a fixed-size list. can't add or remove from it
		List<ReportStatus> items = new ArrayList<>();
		items.addAll(Arrays.asList(values()));
		return items;
	}

	/**
	 * Convert a value to an enum. If the conversion fails, Active is returned
	 *
	 * @param value
	 * @return
	 */
	public static ReportStatus toEnum(String value) {
		return toEnum(value, Active);
	}

	/**
	 * Convert a value to an enum. If the conversion fails, the specified
	 * default is returned
	 *
	 * @param value
	 * @param defaultEnum
	 * @return
	 */
	public static ReportStatus toEnum(String value, ReportStatus defaultEnum) {
		for (ReportStatus v : values()) {
			if (v.value.equalsIgnoreCase(value)) {
				return v;
			}
		}
		return defaultEnum;
	}

	/**
	 * Get enum description. In case description needs to be different from
	 * internal value
	 *
	 * @return
	 */
	public String getDescription() {
		return value;
	}

	/**
	 * Get description message string for use in the user interface. String must
	 * exist in i18n files
	 *
	 * @return
	 */
	public String getLocalizedDescription() {
		return "reportStatus.option." + value;
	}

}
