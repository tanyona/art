package art.artdatabase;

/**
 * Class to act as a form backing bean for art database configuration
 * 
 * @author Timothy Anyona
 */
public class ArtDatabaseForm {
	
	private String driver;
	private String url;
	private String username;
	private String password;
	private int connectionPoolTimeout;
	private String connectionPoolTestSql;

	/**
	 * Get the value of connectionPoolTestSql
	 *
	 * @return the value of connectionPoolTestSql
	 */
	public String getConnectionPoolTestSql() {
		return connectionPoolTestSql;
	}

	/**
	 * Set the value of connectionPoolTestSql
	 *
	 * @param connectionPoolTestSql new value of connectionPoolTestSql
	 */
	public void setConnectionPoolTestSql(String connectionPoolTestSql) {
		this.connectionPoolTestSql = connectionPoolTestSql;
	}

	/**
	 * Get the value of connectionPoolTimeout
	 *
	 * @return the value of connectionPoolTimeout
	 */
	public int getConnectionPoolTimeout() {
		return connectionPoolTimeout;
	}

	/**
	 * Set the value of connectionPoolTimeout
	 *
	 * @param connectionPoolTimeout new value of connectionPoolTimeout
	 */
	public void setConnectionPoolTimeout(int connectionPoolTimeout) {
		this.connectionPoolTimeout = connectionPoolTimeout;
	}

	/**
	 * Get the value of password
	 *
	 * @return the value of password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Set the value of password
	 *
	 * @param password new value of password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Get the value of username
	 *
	 * @return the value of username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Set the value of username
	 *
	 * @param username new value of username
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Get the value of url
	 *
	 * @return the value of url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Set the value of url
	 *
	 * @param url new value of url
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Get the value of driver
	 *
	 * @return the value of driver
	 */
	public String getDriver() {
		return driver;
	}

	/**
	 * Set the value of driver
	 *
	 * @param driver new value of driver
	 */
	public void setDriver(String driver) {
		this.driver = driver;
	}

	
}
