/*
 * ART. A Reporting Tool.
 * Copyright (C) 2017 Enrico Liboni <eliboni@users.sf.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package art.login.method;

import art.servlets.Config;
import art.dbutils.DatabaseUtils;
import art.login.LoginResult;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authenticates a user using a database (using users that are allowed to
 * connect to the database)
 *
 * @author Timothy Anyona
 */
public class DbLogin {

	private static final Logger logger = LoggerFactory.getLogger(DbLogin.class);

	/**
	 * Authenticates a user using database connection credentials
	 *
	 * @param username the username to use
	 * @param password the password to use
	 * @return the result of the authentication process
	 */
	public static LoginResult authenticate(String username, String password) {
		logger.debug("Entering authenticate: username='{}'", username);

		LoginResult result = new LoginResult();

		String url = Config.getSettings().getDatabaseAuthenticationUrl();

		logger.debug("Url='{}'", url);

		if (StringUtils.isBlank(url)) {
			logger.info("Database authentication not configured. username='{}'", username);

			result.setMessage("login.message.databaseAuthenticationNotConfigured");
			result.setDetails("database authentication not configured");
			return result;
		}

		try {
			//Connection conn = DriverManager.getConnection(url, username, password);
			Properties dbProperties = new Properties();
			dbProperties.put("user", username);
			dbProperties.put("password", password);
			Driver driverObject = DriverManager.getDriver(url); //get the right driver for the given url
			Connection conn = driverObject.connect(url, dbProperties);

			//if we are here, authentication is successful
			result.setAuthenticated(true);
			DatabaseUtils.close(conn);
		} catch (SQLException ex) {
			logger.error("Error. username='{}'", username, ex);

			result.setMessage("login.message.invalidCredentials");
			result.setDetails(ex.getMessage());
			result.setError(ex.getMessage());
		}

		return result;
	}
}
