package art.enums;

/**
 * Enum for null value display setting
 *
 * @author Timothy Anyona
 */
public enum DisplayNull {

	Yes("yes"), NoNumbersAsBlank("no-numbers-blank"), NoNumbersAsZero("no-numbers-zero");
	private String value;

	private DisplayNull(String value) {
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
	 * Get enum object based on a string
	 *
	 * @param value
	 * @return
	 */
	public static DisplayNull getEnum(String value) {
		for (DisplayNull v : values()) {
			if (v.value.equalsIgnoreCase(value)) {
				return v;
			}
		}
		return Yes; //default
	}

	/**
	 * Get enum display value for use in the user interface. In case display
	 * value needs to be different from internal value
	 *
	 * @return
	 */
	public String getDisplayValue() {
		switch (this) {
			case Yes:
				return "settings.displayNullOption.yes";
			case NoNumbersAsBlank:
				return "settings.displayNullOption.noNumbersAsBlank";
			case NoNumbersAsZero:
				return "settings.displayNullOption.noNumbersAsZero";
			default:
				return "";
		}
	}
}
