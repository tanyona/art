/*
 * Copyright (C) 2001/2003  Enrico Liboni  - enrico@computer.org
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation;
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   (version 2) along with this program (see documentation directory);
 *   otherwise, have a look at http://www.gnu.org or write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
/**
 * ArtDBCP.class
 *
 *
 * Purpose:	loaded at startup, it initializes an array of database connections
 * (pooled - art dbcp datasources) This array is stored in the context with name
 * "ArtDataSources" and used by all other ART classes
 *
 *
 * @version 1.1
 * @author Enrico Liboni @mail enrico(at)computer.org Last changes: Logging
 */
package art.servlets;

import art.dbcp.DataSource;
import art.utils.ArtProps;
import art.utils.Encrypter;
import com.lowagie.text.FontFactory;
import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that initializes datasource connections and holds global variables.
 *
 * @author Enrico Liboni
 * @author Timothy Anyona
 */
public class ArtDBCP extends HttpServlet {

	private static final long serialVersionUID = 1L;
	final static Logger logger = LoggerFactory.getLogger(ArtDBCP.class);
	// Global variables
	private static String art_username, art_password, art_jdbc_driver, art_jdbc_url,
			exportPath, art_testsql, art_pooltimeout;
	private static int poolMaxConnections;
	private static LinkedHashMap<Integer, DataSource> dataSources; //use a LinkedHashMap that should store items sorted as per the order the items are inserted in the map...
	private static boolean artSettingsLoaded = false;
	private static ArtProps ap;
	private static ArrayList<String> userViewModes; //view modes shown to users
	private static boolean schedulingEnabled = true;
	private static String templatesPath; //full path to templates directory where formatted report templates and mondiran cube definitions are stored
	private static String relativeTemplatesPath; //relative path to templates directory. used by showAnalysis.jsp
	private static String appPath; //application path. to be used to get/build file paths in non-servlet classes
	private static String artVersion; //art version string
	private static boolean artFullVersion = true;
	private static org.quartz.Scheduler scheduler; //to allow access to scheduler from non-servlet classes
	private static ArrayList<String> allViewModes; //all view modes
	private static String passwordHashingAlgorithm = "bcrypt"; //use bcrypt for password hashing
	private static int defaultMaxRows;
	private static ServletContext ctx;
	private static int maxRunningQueries;
	private static String artPropertiesFilePath; //full path to art.properties file
	private static boolean useCustomPdfFont = false; //to allow use of custom font for pdf output, enabling display of non-ascii characters
	private static boolean pdfFontEmbedded = false; //determines if custom font should be embedded in the generated pdf
	private static final String DEFAULT_DATE_FORMAT = "dd-MMM-yyyy";
	private static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";
	private static String dateFormat = DEFAULT_DATE_FORMAT; //for date fields, format of date portion
	private static String timeFormat = DEFAULT_TIME_FORMAT; //for date fields, format of time portion

	/**
	 * {@inheritDoc}
	 *
	 * @param config {@inheritDoc}
	 * @throws ServletException
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {

		super.init(config);

		logger.info("ART is starting up...");

		ctx = getServletConfig().getServletContext();

		ArtDBCPInit();
	}

	/**
	 * Close the connection pools
	 */
	@Override
	public void destroy() {

		if (dataSources != null) {
			for (Integer key : dataSources.keySet()) {
				DataSource ds = dataSources.get(key);
				ds.close();
			}
		}

		logger.info("ART Stopped.");
	}

	/**
	 * Load art.properties file and initialize variables
	 *
	 * @return
	 * <code>true</code> if file found.
	 * <code>false</code> otherwise.
	 */
	public static boolean loadArtSettings() {
		logger.debug("Loading art.properties file");

		String propsFilePath = getArtPropertiesFilePath();
		File propsFile = new File(propsFilePath);
		if (!propsFile.exists()) {
			//art.properties doesn't exit. try art.props
			String sep = java.io.File.separator;
			propsFilePath = getAppPath() + sep + "WEB-INF" + sep + "art.props";
		}

		ap = new ArtProps();

		if (ap.load(propsFilePath)) { // file exists
			logger.debug("Loaded settings from {}", propsFilePath);

			art_username = ap.getProp("art_username");
			art_password = ap.getProp("art_password");
			// de-obfuscate the password
			art_password = Encrypter.decrypt(art_password);

			art_jdbc_url = ap.getProp("art_jdbc_url");
			if (StringUtils.isBlank(art_jdbc_url)) {
				art_jdbc_url = ap.getProp("art_url"); //for 2.2.1 to 2.3+ migration. property name changed from art_url to art_jdbc_url
			}
			art_jdbc_driver = ap.getProp("art_jdbc_driver");

			art_pooltimeout = ap.getProp("art_pooltimeout");
			art_testsql = ap.getProp("art_testsql");

			String pdfFontName = ap.getProp("pdf_font_name");
			if (StringUtils.isBlank(pdfFontName)) {
				useCustomPdfFont = false; //font name must be defined in order to use custom font
			} else {
				useCustomPdfFont = true;
			}
			String fontEmbedded = ap.getProp("pdf_font_embedded");
			if (StringUtils.equals(fontEmbedded, "no")) {
				pdfFontEmbedded = false;
			} else {
				pdfFontEmbedded = true;
			}

			//set date format
			dateFormat = ap.getProp("date_format");
			if(StringUtils.isBlank(dateFormat)){
				dateFormat=DEFAULT_DATE_FORMAT;
			}

			//set time format
			timeFormat = ap.getProp("time_format");
			if(StringUtils.isBlank(timeFormat)){
				timeFormat=DEFAULT_TIME_FORMAT;
			}

			artSettingsLoaded = true;

		} else {
			artSettingsLoaded = false;
		}

		return artSettingsLoaded;

	}

	/**
	 * Initialize datasources, viewModes and variables
	 */
	private void ArtDBCPInit() {

		logger.debug("Initializing variables");

		//Get some web.xml parameters                        
		poolMaxConnections = Integer.parseInt(ctx.getInitParameter("poolMaxConnections"));
		artVersion = ctx.getInitParameter("versionNumber");
		defaultMaxRows = Integer.parseInt(ctx.getInitParameter("defaultMaxRows"));
		maxRunningQueries = Integer.parseInt(ctx.getInitParameter("maxNumberOfRunningQueries"));

		if (StringUtils.equals(ctx.getInitParameter("versionType"), "light")) {
			artFullVersion = false;
		}

		if (StringUtils.equals(ctx.getInitParameter("enableJobScheduling"), "false")) {
			schedulingEnabled = false;
		}

		//set application path
		appPath = ctx.getRealPath("");

		//set templates path
		String sep = java.io.File.separator;
		templatesPath = appPath + sep + "WEB-INF" + sep + "templates" + sep;
		relativeTemplatesPath = "/WEB-INF/templates/";

		//set export path
		exportPath = appPath + sep + "export" + sep;

		//set art.properties file path
		artPropertiesFilePath = appPath + sep + "WEB-INF" + sep + "art.properties";

		//Get user view modes from web.xml file. if a view mode is not in the user list, then it's hidden
		StringTokenizer stCode = new StringTokenizer(ctx.getInitParameter("userViewModesList"), ",");
		String token;
		userViewModes = new ArrayList<String>();
		try {
			while (stCode.hasMoreTokens()) {
				token = stCode.nextToken();
				userViewModes.add(token);
			}
		} catch (Exception e) {
			logger.error("Error while initializing user view modes", e);
		}

		//remove any duplicates in user view modes
		Collection<String> noDup;
		noDup = new LinkedHashSet<String>(userViewModes);
		userViewModes.clear();
		userViewModes.addAll(noDup);

		//construct all view modes list
		allViewModes = new ArrayList<String>(userViewModes);

		//add all supported view modes
		allViewModes.add("tsvGz");
		allViewModes.add("xml");
		allViewModes.add("rss20");
		allViewModes.add("htmlGrid");
		allViewModes.add("html");
		allViewModes.add("xls");
		allViewModes.add("xlsx");
		allViewModes.add("pdf");
		allViewModes.add("htmlPlain");
		allViewModes.add("xlsZip");
		allViewModes.add("slk");
		allViewModes.add("slkZip");
		allViewModes.add("tsv");
		allViewModes.add("tsvZip");
		allViewModes.add("htmlDataTable");

		//remove any duplicates that may exist in all view modes
		noDup = new LinkedHashSet<String>(allViewModes);
		allViewModes.clear();
		allViewModes.addAll(noDup);


		//load settings from art.properties file
		if (!loadArtSettings()) {
			//art.properties not available. don't continue as required configuration settings will be missing
			logger.warn("Not able to get ART settings file (WEB-INF/art.properties). Admin should define ART settings on first logon");
			return;
		}

		//initialize datasources
		initializeDatasources();

		//register pdf fonts
		registerPdfFonts();

	}

	/**
	 * Register custom fonts to be used in pdf output
	 */
	public static void registerPdfFonts() {
		//register pdf fonts. 
		//fresh registering of 661 fonts in c:\windows\fonts can take as little as 10 secs
		//re-registering already registered directory of 661 fonts takes as little as 1 sec

		if (useCustomPdfFont) {
			//register pdf font if not already registered
			String pdfFontName = getArtSetting("pdf_font_name");
			if (!FontFactory.isRegistered(pdfFontName)) {
				//font not registered. register any defined font files or directories
				String pdfFontDirectory = ap.getProp("pdf_font_directory");
				if (StringUtils.isNotBlank(pdfFontDirectory)) {
					logger.info("Registering fonts from directory: {}", pdfFontDirectory);
					int i = FontFactory.registerDirectory(pdfFontDirectory);
					logger.info("{} fonts registered", i);
				}

				String pdfFontFile = ap.getProp("pdf_font_file");
				if (StringUtils.isNotBlank(pdfFontFile)) {
					logger.info("Registering font file: {}", pdfFontFile);
					FontFactory.register(pdfFontFile);
					logger.info("Font file {} registered", pdfFontFile);
				}

				//output registerd fonts
				StringBuilder sb = new StringBuilder();
				String newline = System.getProperty("line.separator");
				Set fonts = FontFactory.getRegisteredFonts();
				for (Iterator it = fonts.iterator(); it.hasNext();) {
					String f = (String) it.next();
					sb.append(newline);
					sb.append(f);
				}
				logger.info("Registered fonts: {}", sb.toString());
			}
		}
	}

	/**
	 * Initialize art repository datasource, and other defined datasources
	 */
	private static void initializeDatasources() {
		//Register jdbc driver for art repository
		try {
			Class.forName(art_jdbc_driver).newInstance();
		} catch (Exception e) {
			logger.error("Error wihle registering driver for ART repository: {}", art_jdbc_driver, e);
		}

		//initialize art repository datasource
		int artPoolTimeout = 15;
		if (art_pooltimeout != null) {
			artPoolTimeout = Integer.parseInt(art_pooltimeout);
		}
		DataSource artdb = new DataSource(artPoolTimeout * 60);
		artdb.setName("ART_Repository"); // custom name
		artdb.setUrl(art_jdbc_url);
		artdb.setUsername(art_username);
		artdb.setPassword(art_password);
		artdb.setLogToStandardOutput(true);
		artdb.setMaxConnections(poolMaxConnections);
		artdb.setDriver(art_jdbc_driver);
		if (StringUtils.length(art_testsql) > 3) {
			artdb.setTestSQL(art_testsql);
		}

		//Initialize the datasources array
		try {
			Connection conn = artdb.getConnection();
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT max(DATABASE_ID) FROM ART_DATABASES");

			if (rs.next()) {
				if (rs.getInt(1) > 0) { // datasources exist
					dataSources = new LinkedHashMap<Integer, DataSource>();
					rs.close();
					rs = st.executeQuery("SELECT DATABASE_ID, NAME, URL, USERNAME, PASSWORD, POOL_TIMEOUT, TEST_SQL FROM ART_DATABASES WHERE DATABASE_ID > 0 ORDER BY NAME");// ordered by NAME to have them inserted in order in the LinkedHashMap dataSources (note: first item is always the ArtRepository)
					int i;

					/**
					 * ******************************************
					 * ART database is the 0 one
					 */
					dataSources.put(new Integer(0), artdb);

					/**
					 * *****************************************
					 * Create other datasources 1-...
					 */
					while (rs.next()) {
						i = rs.getInt("DATABASE_ID");
						int thisPoolTimeoutSecs = 20 * 60; // set the default value to 20 mins
						if (rs.getString("POOL_TIMEOUT") != null) {
							thisPoolTimeoutSecs = Integer.parseInt(rs.getString("POOL_TIMEOUT")) * 60;
						}

						DataSource ds = new DataSource(thisPoolTimeoutSecs);
						ds.setName(rs.getString("NAME"));
						ds.setUrl(rs.getString("URL"));
						ds.setUsername(rs.getString("USERNAME"));
						String pwd = rs.getString("PASSWORD");
						// decrypt password if stored encrypted
						if (pwd.startsWith("o:")) {
							pwd = Encrypter.decrypt(pwd.substring(2));
						}
						String testSQL = rs.getString("TEST_SQL");
						if (StringUtils.length(testSQL) > 3) {
							ds.setTestSQL(testSQL);
						}
						ds.setPassword(pwd);
						ds.setLogToStandardOutput(true);
						ds.setMaxConnections(poolMaxConnections);

						dataSources.put(new Integer(i), ds);
					}
					rs.close();

					// Get jdbc classes to load
					rs = st.executeQuery("SELECT DISTINCT DRIVER FROM ART_DATABASES");
					while (rs.next()) {
						String dbDriver = rs.getString("DRIVER");
						if (!dbDriver.equals(art_jdbc_driver)) {
							// Register a query database driver only if different from the ART one
							// (since ART db one has been already registered by the JVM)
							try {
								Class.forName(dbDriver).newInstance();
								logger.info("Datasource JDBC Driver Registered: {}", dbDriver);
							} catch (Exception e) {
								logger.error("Error while registering Datasource Driver: {}", dbDriver, e);
							}
						}
					}
					st.close();
					conn.close();
				} else { // only art repository has been defined...
					dataSources = new LinkedHashMap<Integer, DataSource>();
					dataSources.put(new Integer(0), artdb);
				}
			}
		} catch (Exception e) {
			logger.error("Error", e);
		}
	}

	/**
	 * Get full path to the export directory.
	 *
	 * @return full path to the export directory
	 */
	public static String getExportPath() {
		return exportPath;
	}

	/**
	 * Get full path to the templates directory.
	 *
	 * @return full path to the templates directory
	 */
	public static String getTemplatesPath() {
		return templatesPath;
	}

	/**
	 * Get the relative path to the templates directory. Used by
	 * showAnalysis.jsp
	 *
	 * @return relative path to the templates directory
	 */
	public static String getRelativeTemplatesPath() {
		return relativeTemplatesPath;
	}

	/**
	 * Get the full application path
	 *
	 * @return the full application path
	 */
	public static String getAppPath() {
		return appPath;
	}

	/**
	 * Get the full path to the art.properties file
	 *
	 * @return the full path to the art.properties file
	 */
	public static String getArtPropertiesFilePath() {
		return artPropertiesFilePath;
	}

	/**
	 * Determine whether a custom font should be used in pdf output
	 *
	 * @return
	 * <code>true</code> if a custom font should be used in pdf output
	 */
	public static boolean isUseCustomPdfFont() {
		return useCustomPdfFont;
	}

	/**
	 * Determine if the custom pdf font should be embedded in the generated pdf
	 *
	 * @return
	 * <code>true</code> if the custom pdf font should be embedded
	 */
	public static boolean isPdfFontEmbedded() {
		return pdfFontEmbedded;
	}

	/**
	 * Log login attempts to the ART_LOGS table.
	 *
	 * @param user username
	 * @param type "login" if successful or "loginerr" if not
	 * @param ip ip address from which login was done or attempted
	 * @param message log message
	 */
	public static void log(String user, String type, String ip, String message) {
		java.sql.Timestamp now = new java.sql.Timestamp(new java.util.Date().getTime());
		Connection logConn = null;

		if (StringUtils.length(message) > 4000) {
			message = message.substring(0, 4000);
		}

		try {
			logConn = getConnection();
			String SQLUpdate = "INSERT INTO ART_LOGS"
					+ " (UPDATE_TIME, USERNAME, LOG_TYPE, IP, MESSAGE) "
					+ " values (?,?,?,?,?) ";

			PreparedStatement psUpdate = logConn.prepareStatement(SQLUpdate);
			psUpdate.setTimestamp(1, now);
			psUpdate.setString(2, user);
			psUpdate.setString(3, type);
			psUpdate.setString(4, ip);
			psUpdate.setString(5, message);

			psUpdate.executeUpdate();

			psUpdate.close();

		} catch (Exception e) {
			logger.error("Error", e);
		} finally {
			try {
				logConn.close();
			} catch (Exception e) {
				logger.error("Error", e);
			}
		}
	}

	/**
	 * Log object execution to the ART_LOGS table.
	 *
	 * @param user username of user who executed the query
	 * @param type "object"
	 * @param ip ip address from which query was run
	 * @param objectId id of the query that was run
	 * @param totalTime total time to execute the query and display the results
	 * @param fetchTime time to fetch the results from the database
	 * @param message log message
	 */
	public static void log(String user, String type, String ip, int objectId, long totalTime, long fetchTime, String message) {
		java.sql.Timestamp now = new java.sql.Timestamp(new java.util.Date().getTime());
		Connection logConn = null;

		if (StringUtils.length(message) > 4000) {
			message = message.substring(0, 4000);
		}

		try {
			logConn = getConnection();
			String SQLUpdate = "INSERT INTO ART_LOGS"
					+ " (UPDATE_TIME, USERNAME, LOG_TYPE, IP, OBJECT_ID, TOTAL_TIME, FETCH_TIME, MESSAGE) "
					+ " values (?,?,?,?,?,?,?,?) ";

			PreparedStatement psUpdate = logConn.prepareStatement(SQLUpdate);
			psUpdate.setTimestamp(1, now);
			psUpdate.setString(2, user);
			psUpdate.setString(3, type);
			psUpdate.setString(4, ip);
			psUpdate.setInt(5, objectId);
			psUpdate.setInt(6, (int) totalTime);
			psUpdate.setInt(7, (int) fetchTime);
			psUpdate.setString(8, message);

			psUpdate.executeUpdate();

			psUpdate.close();

		} catch (Exception e) {
			logger.error("Error", e);
		} finally {
			try {
				logConn.close();
			} catch (Exception e) {
				logger.error("Error", e);
			}
		}
	}

	/**
	 * Determine if art.props file is available and settings have been loaded.
	 *
	 * @return
	 * <code>true</code> if file is available and settings have been loaded
	 * correctly.
	 * <code>false</code> otherwise.
	 */
	public static boolean isArtSettingsLoaded() {
		return artSettingsLoaded; // is false if art.props is not defined
	}

	/**
	 * Return a connection to the datasource with a given ID from the connection
	 * pool.
	 *
	 * @param i id of datasource. 0 = ART repository.
	 * @return connection to datasource or null if connection doesn't exist
	 */
	public static Connection getConnection(int i) {
		Connection conn = null;

		try {
			if (artSettingsLoaded) {
				//artprops has been defined
				DataSource ds = dataSources.get(new Integer(i));
				conn = ds.getConnection(); // i=0 => ART Repository
			}
		} catch (Exception e) {
			logger.error("Error while getting connection for datasource: {}", i, e);
		}

		return conn;
	}

	/**
	 * Return a connection to ART repository from the pool (same as
	 * getConnection(0))
	 *
	 * @return connection to the ART repository or null if connection doesn't
	 * exist
	 */
	public static Connection getConnection() {
		return getConnection(0); // i=0 => ART Repository
	}

	/**
	 * Get a datasource connection based on the datasource name.
	 *
	 * @param name datasource name
	 * @return connection to the datasource or null if connection doesn't exist
	 */
	public static Connection getConnection(String name) {
		Connection conn = null;

		try {
			if (artSettingsLoaded) {
				//artprops has been defined
				if (dataSources != null) {
					for (Integer key : dataSources.keySet()) {
						DataSource ds = dataSources.get(key);
						if (ds != null) {
							if (StringUtils.equalsIgnoreCase(name, ds.getName())) {
								//this is the required datasource. get connection and exit loop
								conn = ds.getConnection();
								break;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error while getting connection for datasource: {}", name, e);
		}

		return conn;
	}

	/**
	 * Return a normal JDBC connection to the ART repository with autocommit
	 * disabled (used for Admins)
	 *
	 * @return connection to the ART repository with autocommit disabled
	 * @throws java.sql.SQLException
	 */
	public static Connection getAdminConnection() throws java.sql.SQLException {
		logger.debug("Getting admin connection");
		// Create a connection to the ART repository for this admin and store it in the
		// admin session (we are not getting this from the pool since it should not be in Autocommit mode)
		Connection connArt = DriverManager.getConnection(art_jdbc_url, art_username, art_password);
		connArt.setAutoCommit(false);
		return connArt;
	}

	/**
	 * Get the username used to connect to the ART repository.
	 *
	 *
	 * @return the username used to connect to the ART repository
	 */
	public static String getArtRepositoryUsername() {
		return art_username;

	}

	/**
	 * Get the password used to connect to the ART repository.
	 *
	 * @return the password used to connect to the ART repository
	 */
	public static String getArtRepositoryPassword() {
		return art_password;
	}

	/**
	 * Get an ART setting as defined in the art.properties file
	 *
	 * @param key setting name
	 * @return setting value
	 */
	public static String getArtSetting(String key) {
		return ap.getProp(key);
	}

	/**
	 * Get a DataSource object.
	 *
	 * @param i id of the datasource
	 * @return DataSource object
	 */
	public static DataSource getDataSource(int i) {
		return dataSources.get(new Integer(i));
	}

	/**
	 * Get all datasources
	 *
	 * @return all datasources
	 */
	public static HashMap getDataSources() {
		return dataSources;
	}

	/**
	 * Refresh all connections in the pool, attempting to properly close the
	 * connections before recreating them.
	 *
	 */
	public static void refreshConnections() {
		//properly close connections
		if (dataSources != null) {
			for (Integer key : dataSources.keySet()) {
				DataSource ds = dataSources.get(key);
				if (ds != null) {
					ds.close();
				}
			}
		}

		//reset datasources array
		initializeDatasources();

		logger.info("Datasources Refresh: Completed at {}", new java.util.Date().toString());
	}

	/**
	 * Refresh all connections in the pool, without attempting to close any
	 * existing connections.
	 *
	 * This is intended to be used on buggy jdbc drivers where for some reasons
	 * the connection.close() method hangs. This may produce a memory leak since
	 * connections are not closed, just removed from the pool: let's hope the
	 * garbage collector decide to remove them sooner or later...
	 */
	public static void forceRefreshConnections() {
		//no attempt to close connections
		dataSources = null;

		//reset datasources array
		initializeDatasources();

		logger.info("Datasources Force Refresh: Completed at {}", new java.util.Date().toString());
	}

	/**
	 * Get the list of available view modes. To allow users to select how to
	 * display the query result Note: not all the viewmodes are displayed here,
	 * see web.xml
	 *
	 * @return list of available view modes
	 */
	public static List<String> getUserViewModes() {
		return userViewModes;
	}

	/**
	 * Get the list of all valid view modes.
	 *
	 * @return list of all valid view modes
	 */
	public static List<String> getAllViewModes() {
		return allViewModes;
	}

	/**
	 * Determine if job scheduling is enabled.
	 *
	 * @return
	 * <code>true</code> if job scheduling is enabled
	 */
	public static boolean isSchedulingEnabled() {
		return schedulingEnabled;
	}

	/**
	 * Utility method to remove characters from query name that may result in an
	 * invalid output file name.
	 *
	 * @param fileName query name
	 * @return modified query name to be used in file names
	 */
	public static String cleanFileName(String fileName) {
		return fileName.replace('/', '_').replace('*', '_').replace('&', '_').replace('?', '_').replace('!', '_').replace('\\', '_').replace('[', '_').replace(']', '_').replace(':', '_');
	}

	/**
	 * Determine if this is the full or light version.
	 *
	 * @return
	 * <code>true</code> if this is the full version
	 */
	public static boolean isArtFullVersion() {
		return artFullVersion;
	}

	/**
	 * Get the art version string. Displayed in art user pages.
	 *
	 * @return the art version string
	 */
	public static String getArtVersion() {
		String version = artVersion;

		if (!artFullVersion) {
			version = artVersion + " - light";
		}

		return version;
	}

	/**
	 * Get job files retention period in days
	 *
	 * @return job files retention period in days
	 */
	public static int getPublishedFilesRetentionPeriod() {
		int retentionPeriod;
		String retentionPeriodString = "";

		try {
			retentionPeriodString = getArtSetting("published_files_retention_period");
			retentionPeriod = Integer.parseInt(retentionPeriodString);
		} catch (NumberFormatException e) {
			logger.warn("Invalid number for published files retention period: {}", retentionPeriodString, e);
			retentionPeriod = 1;
		}

		return retentionPeriod;
	}

	/**
	 * Get mondrian cache expiry period in hours
	 *
	 * @return mondrian cache expiry period in hours
	 */
	public static int getMondrianCacheExpiry() {
		int cacheExpiry;
		String cacheExpiryString = "";

		try {
			cacheExpiryString = getArtSetting("mondrian_cache_expiry");
			cacheExpiry = Integer.parseInt(cacheExpiryString);
		} catch (NumberFormatException e) {
			//invalid number set for cache expiry. default to 0 (no automatic clearing of cache)
			logger.warn("Invalid number for mondrian cache expiry: {}", cacheExpiryString, e);
			cacheExpiry = 0;
		}

		return cacheExpiry;
	}

	/**
	 * Get the hash algorithm setting
	 *
	 * @return hash algorithm setting
	 */
	public static String getPasswordHashingAlgorithm() {
		return passwordHashingAlgorithm;
	}

	/**
	 * Store the quartz scheduler object
	 *
	 * @param s quartz scheduler object
	 */
	public static void setScheduler(org.quartz.Scheduler s) {
		scheduler = s;
	}

	/**
	 * Get the quartz scheduler object
	 *
	 * @return quartz scheduler object
	 */
	public static org.quartz.Scheduler getScheduler() {
		return scheduler;
	}

	/**
	 * Get the default max rows
	 *
	 * @return the default max rows
	 */
	public static int getDefaultMaxRows() {
		return defaultMaxRows;
	}

	/**
	 * Get the max rows for the given view mode
	 *
	 * @param viewMode
	 * @return the max rows for the given view mode
	 */
	public static int getMaxRows(String viewMode) {
		int max;

		String sMax = ctx.getInitParameter(viewMode + "OutputMaxRows");
		if (sMax == null) {
			max = defaultMaxRows;
		} else {
			max = Integer.parseInt(sMax);
		}

		return max;
	}

	/**
	 * Get the maximum number of running queries
	 *
	 * @return the maximum number of running queries
	 */
	public static int getMaxRunningQueries() {
		return maxRunningQueries;
	}

	/**
	 * Get random string to be appended to output filenames
	 *
	 * @return random string to be appended to output filenames
	 */
	public static String getRandomString() {
		return "-" + RandomStringUtils.randomAlphanumeric(10);
	}

	/**
	 * Get string to be displayed in query output for a date field
	 * @param dt
	 * @return 
	 */
	public static String getDateString(java.util.Date dt) {
		String dateString;

		SimpleDateFormat zf = new SimpleDateFormat("HH:mm:ss.SSS"); //use to check if time component is 0
		SimpleDateFormat df = new SimpleDateFormat(dateFormat);
		SimpleDateFormat dtf = new SimpleDateFormat(dateFormat + " " + timeFormat);
		
		if (dt==null) {
			dateString="";
		} else if (zf.format(dt).equals("00:00:00.000")) {
			//time component is 0. don't display time component
			dateString= df.format(dt);
		} else {
			//display both date and time
			dateString= dtf.format(dt);
		}
		
		return dateString;
	}
	
	/**
	 * Get a sort key string to be used for sorting dates in htmlgrid output
	 * @param dt
	 * @return 
	 */
	public static String getSortKey(java.util.Date dt){
		String sortKey;
		
		if(dt==null){
			sortKey="null";
		} else {
			SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss:SSS");
			sortKey=sf.format(dt);
		}
		
		return sortKey;
	}
}
