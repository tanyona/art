-- Create the ART database

-- IMPORTANT:
-- after running this script, ALSO RUN the tables_xxx.sql script for your database
-- (found in the quartz directory)

-- NOTES:
-- for sql server, mysql replace TIMESTAMP with DATETIME

-- UPGRADING:
-- if you are upgrading, don't use this script. run the scripts available in the
-- upgrade directory run the scripts one at a time to upgrade to newer versions.
-- e.g. from 2.0 to 2.1, then 2.1 to 2.2 etc.


-- ------------------------------------------------


-- ART_DATABASE_VERSION
-- stores the version of the ART database

CREATE TABLE ART_DATABASE_VERSION
(
	DATABASE_VERSION VARCHAR(50)
);
-- insert database version
INSERT INTO ART_DATABASE_VERSION VALUES('3.0.0-alpha.3');


-- ART_USERS 
-- Stores user info

-- ACCESS_LEVEL: 0 = normal user, 5 = normal user who can schedule jobs
-- 10 = junior admin, 30 = mid admin, 40 = standard admin, 80 = senior admin
-- 100 = super admin
-- ACTIVE: boolean value. 0=false, 1=true
-- CAN_CHANGE_PASSWORD: boolean value. 0=false, 1=true

CREATE TABLE ART_USERS
(
	USER_ID INTEGER NOT NULL,
	USERNAME VARCHAR(50) NOT NULL,
	PASSWORD VARCHAR(200) NOT NULL,
	PASSWORD_ALGORITHM VARCHAR(20),
	FULL_NAME VARCHAR(40),  
	EMAIL VARCHAR(40),    
	ACCESS_LEVEL INTEGER,
	DEFAULT_QUERY_GROUP INTEGER,
	START_QUERY VARCHAR(500),
	CAN_CHANGE_PASSWORD INTEGER, 
	ACTIVE INTEGER, 
	CREATION_DATE TIMESTAMP,
	UPDATE_DATE TIMESTAMP,	
	CONSTRAINT au_pk PRIMARY KEY(USERNAME)	
);


-- ART_ACCESS_LEVELS
-- Reference table for user access levels

CREATE TABLE ART_ACCESS_LEVELS
(
	ACCESS_LEVEL INTEGER NOT NULL,
	DESCRIPTION VARCHAR(50),
	CONSTRAINT aal_pk PRIMARY KEY(ACCESS_LEVEL)
);
-- insert access levels
INSERT INTO ART_ACCESS_LEVELS VALUES (0,'Normal User');
INSERT INTO ART_ACCESS_LEVELS VALUES (5,'Schedule User');
INSERT INTO ART_ACCESS_LEVELS VALUES (10,'Junior Admin');
INSERT INTO ART_ACCESS_LEVELS VALUES (30,'Mid Admin');
INSERT INTO ART_ACCESS_LEVELS VALUES (40,'Standard Admin');
INSERT INTO ART_ACCESS_LEVELS VALUES (80,'Senior Admin');
INSERT INTO ART_ACCESS_LEVELS VALUES (100,'Super Admin');


-- ART_DATABASES
-- Stores Target Database definitions

-- ACTIVE: boolean. 0=false, 1=true
-- JNDI: boolean. 0=false, 1=true

CREATE TABLE ART_DATABASES
(
	DATABASE_ID INTEGER NOT NULL,
	NAME	          VARCHAR(25) NOT NULL,
	DESCRIPTION VARCHAR(200),
	JNDI INTEGER,
	DRIVER            VARCHAR(200) NOT NULL,
	URL               VARCHAR(2000) NOT NULL,
	USERNAME          VARCHAR(25) NOT NULL,
	PASSWORD          VARCHAR(40) NOT NULL,
	POOL_TIMEOUT      INTEGER,  
	TEST_SQL          VARCHAR(60),
	ACTIVE    INTEGER,
	CREATION_DATE TIMESTAMP,
	UPDATE_DATE TIMESTAMP,
	CONSTRAINT ad_pk PRIMARY KEY(DATABASE_ID),
	CONSTRAINT ad_name_uq UNIQUE(NAME)
);


-- ART_QUERY_GROUPS
-- Stores name and description of query groups

CREATE TABLE ART_QUERY_GROUPS
(
	QUERY_GROUP_ID  INTEGER  NOT NULL,  
	NAME            VARCHAR(25) NOT NULL,
	DESCRIPTION     VARCHAR(60),
	CREATION_DATE TIMESTAMP,
	UPDATE_DATE TIMESTAMP,
	CONSTRAINT aqg_pk PRIMARY KEY(QUERY_GROUP_ID),
	CONSTRAINT aqg_name_uq UNIQUE(NAME)	
);


-- ART_QUERIES
-- Stores query definitions 

-- Query types:
-- 0 = normal query, 1-99 = group, 100 = update, 101 = crosstab
-- 102 = crosstab html only, 103 = normal query html only, 110 = dashboard
-- 111 = text (public), 112 = mondrian cube, 113 = mondrian cube via xmla
-- 114 = sql server analysis services cube via xmla
-- 115 = jasper report with template query, 116 = jasper report with art query
-- 117 = jxls spreadsheet with template query, 118 = jxls spreadsheet with art query
-- 119 = dynamic lov, 120 = static lov, 121 = dynamic job recipients, 122 = text (standard)

-- Query types for graphs:
-- -1 = XY, -2 = Pie 3D, -3 = Horizontal bar 3D, -4 = Vertical bar 3D, -5 = Line
-- -6 = Time series, -7 = Date series, -8 = Stacked vertical bar 3D
-- -9 = Stacked horizontal bar 3D, -10 = Speedometer, -11 = Bubble chart
-- -12 = Heat Map, -13 = Pie 2D, -14 = Vertical bar 2D
-- -15 = Stacked vertical bar 2D, -16 = Horizontal bar 2D
-- -17 = Stacked horizontal bar 2D

-- USES_FILTERS: boolean. 0=false, 1=true
-- PARAMETERS_IN_OUTPUT: boolean. 0=false, 1=true. indicates whether
-- selected report parameters will be shown in the report output

CREATE TABLE ART_QUERIES
(
	QUERY_ID    INTEGER NOT NULL,	
	NAME              VARCHAR(50) NOT NULL,
	SHORT_DESCRIPTION VARCHAR(254) NOT NULL,
	DESCRIPTION       VARCHAR(2000) NOT NULL,
	QUERY_TYPE        INTEGER,
	QUERY_GROUP_ID  INTEGER NOT NULL,	
	DATABASE_ID	    INTEGER NOT NULL,
	CONTACT_PERSON        VARCHAR(20), 
	USES_FILTERS  INTEGER,	 
	REPORT_STATUS    VARCHAR(50),
	PARAMETERS_IN_OUTPUT INTEGER,
	X_AXIS_LABEL VARCHAR(50),
	Y_AXIS_LABEL VARCHAR(50),
	GRAPH_OPTIONS VARCHAR(200),
	TEMPLATE VARCHAR(100),
	DISPLAY_RESULTSET INTEGER,
	XMLA_URL VARCHAR(2000),
	XMLA_DATASOURCE VARCHAR(50),
	XMLA_CATALOG VARCHAR(50),
	XMLA_USERNAME VARCHAR(50),
	XMLA_PASSWORD VARCHAR(50),
	CREATION_DATE TIMESTAMP,
	UPDATE_DATE TIMESTAMP,
	CONSTRAINT aq_pk PRIMARY KEY(QUERY_ID),
	CONSTRAINT aq_name_uq UNIQUE(NAME)
);


-- ART_REPORT_TYPES
-- Reference table for report types

-- report type 1-99 are group reports. only 1-5 is stored in the reference table

CREATE TABLE ART_REPORT_TYPES
(
	REPORT_TYPE INTEGER NOT NULL,
	DESCRIPTION VARCHAR(100),
	CONSTRAINT art_pk PRIMARY KEY(REPORT_TYPE)
);
-- insert report types
INSERT INTO ART_REPORT_TYPES VALUES (0,'Tabular');
INSERT INTO ART_REPORT_TYPES VALUES (1,'Group: 1 column');
INSERT INTO ART_REPORT_TYPES VALUES (2,'Group: 2 columns');
INSERT INTO ART_REPORT_TYPES VALUES (3,'Group: 3 columns');
INSERT INTO ART_REPORT_TYPES VALUES (4,'Group: 4 columns');
INSERT INTO ART_REPORT_TYPES VALUES (5,'Group: 5 columns');
INSERT INTO ART_REPORT_TYPES VALUES (100,'Update Statement');
INSERT INTO ART_REPORT_TYPES VALUES (101,'Crosstab');
INSERT INTO ART_REPORT_TYPES VALUES (102,'Crosstab (html only)');
INSERT INTO ART_REPORT_TYPES VALUES (103,'Tabular (html only)');
INSERT INTO ART_REPORT_TYPES VALUES (110,'Dashboard');
INSERT INTO ART_REPORT_TYPES VALUES (111,'Text (public)');
INSERT INTO ART_REPORT_TYPES VALUES (112,'Pivot Table: Mondrian');
INSERT INTO ART_REPORT_TYPES VALUES (113,'Pivot Table: Mondrian XMLA');
INSERT INTO ART_REPORT_TYPES VALUES (114,'Pivot Table: SQL Server XMLA');
INSERT INTO ART_REPORT_TYPES VALUES (115,'JasperReport: Template Query');
INSERT INTO ART_REPORT_TYPES VALUES (116,'JasperReport: ART Query');
INSERT INTO ART_REPORT_TYPES VALUES (117,'jXLS Spreadsheet: Template Query');
INSERT INTO ART_REPORT_TYPES VALUES (118,'jXLS Spreadsheet: ART Query');
INSERT INTO ART_REPORT_TYPES VALUES (119,'LOV: Dynamic');
INSERT INTO ART_REPORT_TYPES VALUES (120,'LOV: Static');
INSERT INTO ART_REPORT_TYPES VALUES (121,'Dynamic Job Recipients');
INSERT INTO ART_REPORT_TYPES VALUES (122,'Text');
INSERT INTO ART_REPORT_TYPES VALUES (-1,'Chart: XY');
INSERT INTO ART_REPORT_TYPES VALUES (-2,'Chart: Pie 3D');
INSERT INTO ART_REPORT_TYPES VALUES (-3,'Chart: Horizontal Bar 3D');
INSERT INTO ART_REPORT_TYPES VALUES (-4,'Chart: Vertical Bar 3D');
INSERT INTO ART_REPORT_TYPES VALUES (-5,'Chart: Line');
INSERT INTO ART_REPORT_TYPES VALUES (-6,'Chart: Time Series');
INSERT INTO ART_REPORT_TYPES VALUES (-7,'Chart: Date Series');
INSERT INTO ART_REPORT_TYPES VALUES (-8,'Chart: Stacked Vertical Bar 3D');
INSERT INTO ART_REPORT_TYPES VALUES (-9,'Chart: Stacked Horizontal Bar 3D');
INSERT INTO ART_REPORT_TYPES VALUES (-10,'Chart: Speedometer');
INSERT INTO ART_REPORT_TYPES VALUES (-11,'Chart: Bubble Chart');
INSERT INTO ART_REPORT_TYPES VALUES (-12,'Chart: Heat Map');
INSERT INTO ART_REPORT_TYPES VALUES (-13,'Chart: Pie 2D');
INSERT INTO ART_REPORT_TYPES VALUES (-14,'Chart: Vertical Bar 2D');
INSERT INTO ART_REPORT_TYPES VALUES (-15,'Chart: Stacked Vertical Bar 2D');
INSERT INTO ART_REPORT_TYPES VALUES (-16,'Chart: Horizontal Bar 2D');
INSERT INTO ART_REPORT_TYPES VALUES (-17,'Chart: Stacked Horizontal Bar 2D');


-- ART_ADMIN_PRIVILEGES
-- stores privileges for Junior and Mid Admin (Admin Level <=30)
-- this table is used to limit data extraction for these admins when
-- viewing available groups and datasources

-- PRIVILEGE can be either "DB" (datasource) or "GRP" (query group)
-- VALUE_ID is the datasource id or query group id

CREATE TABLE ART_ADMIN_PRIVILEGES
(	
	USER_ID INTEGER,
	USERNAME    VARCHAR(50) NOT NULL,
	PRIVILEGE   VARCHAR(4) NOT NULL,
	VALUE_ID    INTEGER NOT NULL,
	CONSTRAINT aap_pk PRIMARY KEY(USERNAME,PRIVILEGE,VALUE_ID)	
);


-- ART_USER_QUERIES
-- Stores the queries a user can execute

CREATE TABLE ART_USER_QUERIES
(
	USER_ID INTEGER,
	USERNAME    VARCHAR(50) NOT NULL,
	QUERY_ID    INTEGER     NOT NULL,	 
	CONSTRAINT auq_pk PRIMARY KEY(USERNAME,QUERY_ID)	
);


-- ART_USER_QUERY_GROUPS
-- Stores query groups a user can deal with

CREATE TABLE ART_USER_QUERY_GROUPS
(
	USER_ID INTEGER,
	USERNAME       VARCHAR(50) NOT NULL,
	QUERY_GROUP_ID INTEGER     NOT NULL,        
	CONSTRAINT auqg_pk PRIMARY KEY(USERNAME,QUERY_GROUP_ID)	
);


-- ART_PARAMETERS
-- Stores parameter definitions

-- NAME: stores the column name for non-labelled MULTI params
-- or the parameter name for INLINE params or labelled multi params
-- HIDDEN: boolean
-- USE_LOV: boolean
-- USE_FILTERS_IN_LOV: boolean
-- USE_DIRECT_SUBSTITUTION: boolean
-- CHAINED_POSITION is the position of the chained parameter
-- CHAINED_VALUE_POSITION - allow chained parameter value to come from
-- a different parameter from the previous one in the chained parameter sequence
-- DRILLDOWN_COLUMN_INDEX - if used in a drilldown report, refers to the column in
-- the parent report on which the parameter will be applied (index starts from 1)

CREATE TABLE ART_PARAMETERS
(	
	PARAMETER_ID INTEGER NOT NULL,		
	NAME  VARCHAR(60),
	DESCRIPTION VARCHAR(50),
	PARAMETER_TYPE VARCHAR(30),           
	PARAMETER_LABEL     VARCHAR(50),
	HELP_TEXT            VARCHAR(120),
	DATA_TYPE         VARCHAR(30),
	DEFAULT_VALUE     VARCHAR(80),
	HIDDEN INTEGER,
	USE_LOV INTEGER, 
	LOV_REPORT_ID  INTEGER,
	USE_FILTERS_IN_LOV INTEGER,	
	CHAINED_POSITION  INTEGER,              
	CHAINED_VALUE_POSITION INTEGER,
	DRILLDOWN_COLUMN_INDEX INTEGER,
	USE_DIRECT_SUBSTITUTION INTEGER,	
	CREATION_DATE TIMESTAMP,
	UPDATE_DATE TIMESTAMP,	
	CONSTRAINT ap_pk PRIMARY KEY (PARAMETER_ID)	
);


-- ART_REPORT_PARAMETERS
-- Stores parameters used in reports

CREATE TABLE ART_REPORT_PARAMETERS
(	
	REPORT_PARAMETER_ID INTEGER NOT NULL,
	REPORT_ID INTEGER NOT NULL,	
	PARAMETER_ID INTEGER NOT NULL,	
	PARAMETER_POSITION INTEGER NOT NULL,
	CONSTRAINT arp_pk PRIMARY KEY (REPORT_PARAMETER_ID)	
);


-- ART_QUERY_FIELDS
-- Stores query parameters

-- FIELD_POSITION is the order the parameter is displayed to users
-- FIELD_CLASS stores the data type of the parameter
-- PARAM_TYPE: M for MULTI param, I for INLINE param 
-- PARAM_LABEL stores the column name for non-labelled MULTI params
-- or the parameter label for INLINE params or labelled multi params
-- USE_LOV is set to Y if the param values are provided by an LOV query
-- CHAINED_PARAM_POSITION is the position of the chained param 
-- CHAINED_VALUE_POSITION - allow chained parameter value to come from
-- a different parameter from the previous one in the chained parameter sequence
-- DRILLDOWN_COLUMN - if used in a drill down report, refers to the column in
-- the parent report on which the parameter will be applied 

CREATE TABLE ART_QUERY_FIELDS
(	
	QUERY_ID                INTEGER     NOT NULL,
	FIELD_POSITION          INTEGER     NOT NULL, 
	NAME                    VARCHAR(25),
	SHORT_DESCRIPTION       VARCHAR(40),
	DESCRIPTION             VARCHAR(120),
	PARAM_TYPE VARCHAR(1) NOT NULL,           
	PARAM_LABEL     VARCHAR(55),  
	PARAM_DATA_TYPE         VARCHAR(15) NOT NULL,
	DEFAULT_VALUE           VARCHAR(80),	        
	USE_LOV       VARCHAR(1), 		
	APPLY_RULES_TO_LOV        VARCHAR(1),
	LOV_QUERY_ID  INTEGER,
	CHAINED_PARAM_POSITION  INTEGER,              
	CHAINED_VALUE_POSITION INTEGER,
	DRILLDOWN_COLUMN INTEGER,
	DIRECT_SUBSTITUTION VARCHAR(1),
	MIGRATED INTEGER,
	UPDATE_DATE TIMESTAMP,	
	CONSTRAINT aqf_pk PRIMARY KEY (QUERY_ID,FIELD_POSITION)	
);


-- ART_ALL_SOURCES
-- Stores source code for queries (sql, mdx, xml, html, text)

CREATE TABLE ART_ALL_SOURCES
(
	OBJECT_ID              INTEGER      NOT NULL,	
	LINE_NUMBER            INTEGER      NOT NULL,
	SOURCE_INFO              VARCHAR(4000),
	CONSTRAINT aas_pk PRIMARY KEY (OBJECT_ID,LINE_NUMBER)	
);


-- ART_RULES
-- Stores Rule definitions (names)
 
CREATE TABLE ART_RULES
(
	RULE_ID INTEGER NOT NULL,
	RULE_NAME         VARCHAR(15) NOT NULL,
	SHORT_DESCRIPTION VARCHAR(40),
	DATA_TYPE VARCHAR(30),
	CREATION_DATE TIMESTAMP,
	UPDATE_DATE TIMESTAMP,
	CONSTRAINT ar_pk PRIMARY KEY(RULE_NAME)
);


-- ART_QUERY_RULES
-- Stores rules-query relationships 

CREATE TABLE ART_QUERY_RULES
(
	QUERY_RULE_ID INTEGER NOT NULL,
	QUERY_ID          INTEGER       NOT NULL,
	RULE_ID INTEGER,
	RULE_NAME          VARCHAR(15)   NOT NULL,
	FIELD_NAME        VARCHAR(40)   NOT NULL,
	FIELD_DATA_TYPE VARCHAR(15), 
	CONSTRAINT aqr_pk PRIMARY KEY (QUERY_ID,RULE_NAME)	
);


-- ART_USER_RULES
-- Stores rule values for users
-- RULE_TYPE can be EXACT or LOOKUP
 
CREATE TABLE ART_USER_RULES
(  
	USER_ID INTEGER NOT NULL,
	USERNAME VARCHAR(50) NOT NULL,
	RULE_ID INTEGER,
	RULE_NAME VARCHAR(15) NOT NULL, 
	RULE_VALUE VARCHAR(25) NOT NULL,
	RULE_TYPE VARCHAR(6)	
);

-- ART_USER_GROUP_RULES
-- Stores rule values for user groups
-- RULE_TYPE can be EXACT or LOOKUP
 
CREATE TABLE ART_USER_GROUP_RULES
(  
	USER_GROUP_ID INTEGER  NOT NULL,
	RULE_ID INTEGER,
	RULE_NAME         VARCHAR(15)   NOT NULL, 
	RULE_VALUE        VARCHAR(25)   NOT NULL,
	RULE_TYPE		  VARCHAR(6)	
);


-- ART_JOBS
-- Stores scheduled jobs

-- OUTPUT_FORMAT: html, pdf, xls etc (viewMode code)
-- LAST_FILE_NAME: Contains result of last job execution. Either a status message
-- (if contents start with -), or a file name and status message separated by 
-- newline character (\n) (for publish jobs)
-- MIGRATED_TO_QUARTZ is present to allow seamless migration of jobs when
-- upgrading from ART versions before 1.11
-- (before quartz was used as the scheduling engine)
-- ACTIVE: boolean. 0=false, 1=true

CREATE TABLE ART_JOBS
(
	JOB_ID INTEGER NOT NULL,
	JOB_NAME VARCHAR(50),
	QUERY_ID	    INTEGER NOT NULL,
	USER_ID INTEGER,
	USERNAME          VARCHAR(50) NOT NULL,
	OUTPUT_FORMAT            VARCHAR(15) NOT NULL, 
	JOB_TYPE VARCHAR(50),      
	JOB_MINUTE	    VARCHAR(100),               
	JOB_HOUR		    VARCHAR(100),               
	JOB_DAY		    VARCHAR(100),               
	JOB_WEEKDAY	    VARCHAR(100),               
	JOB_MONTH		    VARCHAR(100),               
	MAIL_TOS          VARCHAR(254),
	MAIL_FROM         VARCHAR(80),
	MAIL_CC VARCHAR(254),
	MAIL_BCC VARCHAR(254),
	SUBJECT	    VARCHAR(254),
	MESSAGE           VARCHAR(4000),
	CACHED_TABLE_NAME VARCHAR(30),	
	START_DATE TIMESTAMP,
	END_DATE TIMESTAMP,
	NEXT_RUN_DATE TIMESTAMP NULL,		
	LAST_FILE_NAME    VARCHAR(4000),
	LAST_START_DATE   TIMESTAMP NULL,
	LAST_END_DATE     TIMESTAMP NULL, 
	ACTIVE INTEGER,
	ENABLE_AUDIT        VARCHAR(1) NOT NULL,				
	ALLOW_SHARING VARCHAR(1),
	ALLOW_SPLITTING VARCHAR(1),
	RECIPIENTS_QUERY_ID INTEGER,
	RUNS_TO_ARCHIVE INTEGER,
	MIGRATED_TO_QUARTZ VARCHAR(1),
	CREATION_DATE TIMESTAMP,
	UPDATE_DATE TIMESTAMP,
	CONSTRAINT aj_pk PRIMARY KEY(JOB_ID)
);


-- ART_JOB_TYPES
-- Reference table for job types

CREATE TABLE ART_JOB_TYPES
(
	JOB_TYPE INTEGER NOT NULL,
	DESCRIPTION VARCHAR(100),
	CONSTRAINT ajt_pk PRIMARY KEY(JOB_TYPE)
);
-- insert job types
INSERT INTO ART_JOB_TYPES VALUES(1,'Alert');
INSERT INTO ART_JOB_TYPES VALUES(2,'Email Output (Attachment)');
INSERT INTO ART_JOB_TYPES VALUES(3,'Publish');
INSERT INTO ART_JOB_TYPES VALUES(4,'Just Run It');
INSERT INTO ART_JOB_TYPES VALUES(5,'Email Output (Inline)');
INSERT INTO ART_JOB_TYPES VALUES(6,'Conditional Email Output (Attachment)');
INSERT INTO ART_JOB_TYPES VALUES(7,'Conditional Email Output (Inline)');
INSERT INTO ART_JOB_TYPES VALUES(8,'Conditional Publish');
INSERT INTO ART_JOB_TYPES VALUES(9,'Cache ResultSet (Append)');
INSERT INTO ART_JOB_TYPES VALUES(10,'Cache ResultSet (Delete & Insert)');


-- ART_JOBS_PARAMETERS
-- store jobs parameters

-- PARAM_TYPE: M = multi, I = inline 
-- PARAM_NAME: the html element name of the parameter

CREATE TABLE ART_JOBS_PARAMETERS
(
	JOB_ID        INTEGER NOT NULL,
	PARAM_TYPE	VARCHAR(1) NOT NULL,   
	PARAM_NAME		    VARCHAR(60),
	PARAM_VALUE		    VARCHAR(200)	
);


-- ART_JOBS_AUDIT
-- stores logs of every job execution when job auditing is enabled

-- USERNAME: user for whom the job is run
-- JOB_AUDIT_KEY: unique identifier for a job audit record
-- ACTION: S = job started, E = job ended, X = Error occurred while running job

CREATE TABLE ART_JOBS_AUDIT
(
	JOB_ID            INTEGER NOT NULL,
	USERNAME VARCHAR(50),
	JOB_AUDIT_KEY VARCHAR(100),
	JOB_ACTION   VARCHAR(1),             
	START_DATE TIMESTAMP NULL,
	END_DATE TIMESTAMP NULL	
);

		
-- ART_LOGS
-- Stores log information e.g. logins and query execution

-- LOG_TYPE: login = successful login, loginerr = unsuccessful login attempt
-- query = interactive query execution, upload = template file uploaded when
-- creating query that uses a template file
-- TOTAL_TIME: total execution time in secs, including fetch time and display time
-- FETCH_TIME: time elapsed from when the query is submitted to when the
-- database returns 1st row

CREATE TABLE ART_LOGS
(
	LOG_DATE TIMESTAMP NOT NULL,	
	USERNAME VARCHAR(50) NOT NULL,
	LOG_TYPE VARCHAR(50) NOT NULL, 
	IP          VARCHAR(15), 
	QUERY_ID   INTEGER,
	TOTAL_TIME  INTEGER, 
	FETCH_TIME  INTEGER, 
	MESSAGE     VARCHAR(500) 
);


-- ART_USER_JOBS
-- Stores users who have been given access to a job's output

-- USER_GROUP_ID: used to indicate if job was shared via user group. To enable
-- deletion of split job records where access was granted via user group,
-- when a user is removed from a group.
-- LAST_FILE_NAME: contains file name for individualized output (split job),
-- or NULL if file name to use comes from ART_JOBS table

CREATE TABLE ART_USER_JOBS
(
	JOB_ID INTEGER NOT NULL,
	USER_ID INTEGER,
	USERNAME VARCHAR(50) NOT NULL,
	USER_GROUP_ID INTEGER,
	LAST_FILE_NAME VARCHAR(4000),
	LAST_START_DATE TIMESTAMP NULL,
	LAST_END_DATE TIMESTAMP NULL,
	CONSTRAINT auj_pk PRIMARY KEY (JOB_ID,USERNAME)	
);


-- ART_JOB_SCHEDULES
-- Stores job schedules to enable re-use of schedules when creating jobs

CREATE TABLE ART_JOB_SCHEDULES
(
	SCHEDULE_ID INTEGER NOT NULL,
	SCHEDULE_NAME VARCHAR(50) NOT NULL,
	DESCRIPTION VARCHAR(200),
	JOB_MINUTE	    VARCHAR(100),               
	JOB_HOUR		    VARCHAR(100),               
	JOB_DAY		    VARCHAR(100), 
	JOB_MONTH		    VARCHAR(100),   	
	JOB_WEEKDAY	    VARCHAR(100),
	CREATION_DATE TIMESTAMP,
	UPDATE_DATE TIMESTAMP,
	CONSTRAINT ajs_pk PRIMARY KEY(SCHEDULE_NAME)
);


-- ART_USER_GROUPS
-- Stores user group definitions

CREATE TABLE ART_USER_GROUPS
(
	USER_GROUP_ID INTEGER NOT NULL,
	NAME VARCHAR(30) NOT NULL,
	DESCRIPTION VARCHAR(50),
	DEFAULT_QUERY_GROUP INTEGER,
	START_QUERY VARCHAR(500),
	CREATION_DATE TIMESTAMP,
	UPDATE_DATE TIMESTAMP,
	CONSTRAINT aug_pk PRIMARY KEY(USER_GROUP_ID),
	CONSTRAINT aug_name_uq UNIQUE(NAME)
);


-- ART_USER_GROUP_ASSIGNEMENT
-- Stores details of which users belong to which user groups

CREATE TABLE ART_USER_GROUP_ASSIGNMENT
(
	USER_ID INTEGER NOT NULL,
	USERNAME VARCHAR(50) NOT NULL,
	USER_GROUP_ID INTEGER NOT NULL,
	CONSTRAINT auga_pk PRIMARY KEY (USERNAME,USER_GROUP_ID)	
);


-- ART_USER_GROUP_QUERIES
-- Stores which queries certain user groups can access (users who are members of 
-- the group can access the queries)

CREATE TABLE ART_USER_GROUP_QUERIES
(
	USER_GROUP_ID INTEGER NOT NULL,
	QUERY_ID INTEGER NOT NULL,
	CONSTRAINT augq_pk PRIMARY KEY (USER_GROUP_ID,QUERY_ID)	
);


-- ART_USER_GROUP_GROUPS
-- Stores which query groups certain user groups can access (users who are members
-- of the group can access the query groups)

CREATE TABLE ART_USER_GROUP_GROUPS
(
	USER_GROUP_ID INTEGER NOT NULL,
	QUERY_GROUP_ID INTEGER NOT NULL,
	CONSTRAINT augg_pk PRIMARY KEY (USER_GROUP_ID,QUERY_GROUP_ID)	
);


-- ART_USER_GROUP_JOBS
-- Stores which jobs have been shared with certain user groups (users who are
-- members of the group can access the job output)

CREATE TABLE ART_USER_GROUP_JOBS
(
	USER_GROUP_ID INTEGER NOT NULL,
	JOB_ID INTEGER NOT NULL,
	CONSTRAINT augj_pk PRIMARY KEY (USER_GROUP_ID,JOB_ID)	
);


-- ART_DRILLDOWN_QUERIES
-- Stores details of drill down queries

-- OPEN_IN_NEW_WINDOW: boolean

CREATE TABLE ART_DRILLDOWN_QUERIES
(
	DRILLDOWN_ID INTEGER NOT NULL,
	QUERY_ID INTEGER NOT NULL,
	DRILLDOWN_QUERY_ID INTEGER NOT NULL,
	DRILLDOWN_QUERY_POSITION INTEGER NOT NULL,
	DRILLDOWN_TITLE VARCHAR(30),
	DRILLDOWN_TEXT VARCHAR(30),
	OUTPUT_FORMAT VARCHAR(15),
	OPEN_IN_NEW_WINDOW INTEGER,
	CONSTRAINT adq_pk PRIMARY KEY (QUERY_ID,DRILLDOWN_QUERY_POSITION)	
);


-- ART_JOB_ARCHIVES
-- Stored details of past runs for publish jobs

-- JOB_SHARED: N = job not shared, Y = job shared, S = split job

CREATE TABLE ART_JOB_ARCHIVES
(
	ARCHIVE_ID VARCHAR(100) NOT NULL,
	JOB_ID INTEGER NOT NULL,
	USER_ID INTEGER,
	USERNAME VARCHAR(50) NOT NULL,	
	ARCHIVE_FILE_NAME VARCHAR(4000),
	START_DATE TIMESTAMP NULL,
	END_DATE TIMESTAMP NULL,
	JOB_SHARED VARCHAR(1),
	CONSTRAINT aja_pk PRIMARY KEY(ARCHIVE_ID)
);


