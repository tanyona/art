/**
 * Copyright (C) 2016 Enrico Liboni <eliboni@users.sourceforge.net>
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
package art.ruleValue;

import art.dbutils.DbService;
import art.enums.ParameterDataType;
import art.rule.Rule;
import art.rule.RuleService;
import art.user.User;
import art.user.UserService;
import art.usergroup.UserGroup;
import art.usergroup.UserGroupService;
import art.utils.ArtUtils;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Provides methods for retrieving, adding, updating and deleting rule values
 *
 * @author Timothy Anyona
 */
@Service
public class RuleValueService {

	private static final Logger logger = LoggerFactory.getLogger(RuleValueService.class);

	private final DbService dbService;
	private final UserService userService;
	private final RuleService ruleService;
	private final UserGroupService userGroupService;

	@Autowired
	public RuleValueService(DbService dbService, UserService userService,
			RuleService ruleService, UserGroupService userGroupService) {

		this.dbService = dbService;
		this.userService = userService;
		this.ruleService = ruleService;
		this.userGroupService = userGroupService;
	}

	public RuleValueService() {
		dbService = new DbService();
		userService = new UserService();
		ruleService = new RuleService();
		userGroupService = new UserGroupService();
	}

	private final String SQL_SELECT_ALL_USER_RULE_VALUES = "SELECT * FROM ART_USER_RULES";
	private final String SQL_SELECT_ALL_USER_GROUP_RULE_VALUES = "SELECT * FROM ART_USER_GROUP_RULES";

	/**
	 * Maps a resultset to an object
	 */
	private class UserRuleValueMapper extends BasicRowProcessor {

		@Override
		public <T> List<T> toBeanList(ResultSet rs, Class<T> type) throws SQLException {
			List<T> list = new ArrayList<>();
			while (rs.next()) {
				list.add(toBean(rs, type));
			}
			return list;
		}

		@Override
		public <T> T toBean(ResultSet rs, Class<T> type) throws SQLException {
			UserRuleValue value = new UserRuleValue();

			value.setRuleValue(rs.getString("RULE_VALUE"));
			value.setRuleValueKey(rs.getString("RULE_VALUE_KEY"));

			User user = userService.getUser(rs.getInt("USER_ID"));
			value.setUser(user);

			Rule rule = ruleService.getRule(rs.getInt("RULE_ID"));
			value.setRule(rule);

			return type.cast(value);
		}
	}

	/**
	 * Maps a resultset to an object
	 */
	private class UserGroupRuleValueMapper extends BasicRowProcessor {

		@Override
		public <T> List<T> toBeanList(ResultSet rs, Class<T> type) throws SQLException {
			List<T> list = new ArrayList<>();
			while (rs.next()) {
				list.add(toBean(rs, type));
			}
			return list;
		}

		@Override
		public <T> T toBean(ResultSet rs, Class<T> type) throws SQLException {
			UserGroupRuleValue value = new UserGroupRuleValue();

			value.setRuleValue(rs.getString("RULE_VALUE"));
			value.setRuleValueKey(rs.getString("RULE_VALUE_KEY"));

			UserGroup userGroup = userGroupService.getUserGroup(rs.getInt("USER_GROUP_ID"));
			value.setUserGroup(userGroup);

			Rule rule = ruleService.getRule(rs.getInt("RULE_ID"));
			value.setRule(rule);

			return type.cast(value);
		}
	}

	/**
	 * Returns all user rule values
	 *
	 * @return all user rule values
	 * @throws SQLException
	 */
	public List<UserRuleValue> getAllUserRuleValues() throws SQLException {
		logger.debug("Entering getAllUserRuleValues");

		ResultSetHandler<List<UserRuleValue>> h = new BeanListHandler<>(UserRuleValue.class, new UserRuleValueMapper());
		return dbService.query(SQL_SELECT_ALL_USER_RULE_VALUES, h);
	}

	/**
	 * Returns a user rule value
	 *
	 * @param id the rule value key
	 * @return user rule value if found, null otherwise
	 * @throws SQLException
	 */
	public UserRuleValue getUserRuleValue(String id) throws SQLException {
		logger.debug("Entering getUserRuleValue: id='{}'", id);

		String sql = SQL_SELECT_ALL_USER_RULE_VALUES + " WHERE RULE_VALUE_KEY=?";
		ResultSetHandler<UserRuleValue> h = new BeanHandler<>(UserRuleValue.class, new UserRuleValueMapper());
		return dbService.query(sql, h, id);
	}

	/**
	 * Returns rule values for the given user and rule
	 *
	 * @param userId the user's user id
	 * @param ruleId the rule's rule id
	 * @return rule values for the given user and rule
	 * @throws SQLException
	 */
	public List<String> getUserRuleValues(int userId, int ruleId) throws SQLException {
		logger.debug("Entering getUserRuleValues: userId={}, ruleId={}", userId, ruleId);

		String sql = SQL_SELECT_ALL_USER_RULE_VALUES + " WHERE USER_ID=? AND RULE_ID=?";
		ResultSetHandler<List<String>> h = new ColumnListHandler<>("RULE_VALUE");
		return dbService.query(sql, h, userId, ruleId);
	}

	/**
	 * Returns all user group rule values
	 *
	 * @return all user group rule values
	 * @throws SQLException
	 */
	public List<UserGroupRuleValue> getAllUserGroupRuleValues() throws SQLException {
		logger.debug("Entering getAllUserGroupRuleValues");

		ResultSetHandler<List<UserGroupRuleValue>> h = new BeanListHandler<>(UserGroupRuleValue.class, new UserGroupRuleValueMapper());
		return dbService.query(SQL_SELECT_ALL_USER_GROUP_RULE_VALUES, h);
	}

	/**
	 * Returns a user group rule value
	 *
	 * @param id the rule value key
	 * @return user group rule value if found, null otherwise
	 * @throws SQLException
	 */
	public UserGroupRuleValue getUserGroupRuleValue(String id) throws SQLException {
		logger.debug("Entering getUserGroupRuleValue: id='{}'", id);

		String sql = SQL_SELECT_ALL_USER_GROUP_RULE_VALUES + " WHERE RULE_VALUE_KEY=?";
		ResultSetHandler<UserGroupRuleValue> h = new BeanHandler<>(UserGroupRuleValue.class, new UserGroupRuleValueMapper());
		return dbService.query(sql, h, id);
	}

	/**
	 * Returns rule values for a given user group and rule
	 *
	 * @param userGroupId the user group's id
	 * @param ruleId the rule id
	 * @return rule values for a given user group and rule
	 * @throws SQLException
	 */
	public List<String> getUserGroupRuleValues(int userGroupId, int ruleId) throws SQLException {
		logger.debug("Entering getUserGroupRuleValues: userGroupId={}, ruleId={}", userGroupId, ruleId);

		String sql = SQL_SELECT_ALL_USER_GROUP_RULE_VALUES + " WHERE USER_GROUP_ID=? AND RULE_ID=?";
		ResultSetHandler<List<String>> h = new ColumnListHandler<>("RULE_VALUE");
		return dbService.query(sql, h, userGroupId, ruleId);
	}

	/**
	 * Deletes a user rule value
	 *
	 * @param id the rule value key
	 * @throws SQLException
	 */
	public void deleteUserRuleValue(String id) throws SQLException {
		logger.debug("Entering deleteUserRuleValue: id='{}'", id);

		String sql;

		sql = "DELETE FROM ART_USER_RULES WHERE RULE_VALUE_KEY=?";
		dbService.update(sql, id);
	}

	/**
	 * Deletes a user group rule value
	 *
	 * @param id the rule value key
	 * @throws SQLException
	 */
	public void deleteUserGroupRuleValue(String id) throws SQLException {
		logger.debug("Entering deleteUserGroupRuleValue: id='{}'", id);

		String sql;

		sql = "DELETE FROM ART_USER_GROUP_RULES WHERE RULE_VALUE_KEY=?";
		dbService.update(sql, id);
	}

	/**
	 * Deletes all user rule values
	 *
	 * @param users user identifiers in the format user id-username
	 * @param userGroups user group ids
	 * @param ruleId the rule id
	 * @throws SQLException
	 */
	public void deleteAllRuleValues(String[] users, Integer[] userGroups,
			int ruleId) throws SQLException {

		logger.debug("Entering deleteAllRuleValues: ruleId={}", ruleId);

		//use parameterized where in
		//https://stackoverflow.com/questions/2861230/what-is-the-best-approach-using-jdbc-for-parameterizing-an-in-clause
		//http://www.javaranch.com/journal/200510/Journal200510.jsp#a2
		//delete user values
		if (users != null) {
			String baseSql = "DELETE FROM ART_USER_RULES WHERE USER_ID IN(%s) AND RULE_ID=?";

			//get user ids
			List<Integer> userIds = new ArrayList<>();
			for (String user : users) {
				Integer userId = Integer.valueOf(StringUtils.substringBefore(user, "-"));
				//username won't be needed once user id columns completely replace username in foreign keys
				userIds.add(userId);
			}

			//prepare parameter placeholders for user ids
			String userIdPlaceHolders = StringUtils.repeat("?", ",", userIds.size());

			//set final sql string to be used
			String finalSql = String.format(baseSql, userIdPlaceHolders);

			//prepare final parameter values, in the correct order to match the placeholders
			List<Object> finalValues = new ArrayList<>();
			finalValues.addAll(userIds);
			finalValues.add(ruleId);

			dbService.update(finalSql, finalValues.toArray());
		}

		//delete user group values
		if (userGroups != null) {
			String baseSql = "DELETE FROM ART_USER_GROUP_RULES"
					+ " WHERE USER_GROUP_ID IN(%s) AND RULE_ID=?";

			//prepare parameter placeholders for user group ids
			String userGroupIdPlaceHolders = StringUtils.repeat("?", ",", userGroups.length);

			//set final sql string to be used
			String finalSql = String.format(baseSql, userGroupIdPlaceHolders);

			//prepare final parameter values, in the correct order to match the placeholders
			List<Object> finalValues = new ArrayList<>();
			finalValues.addAll(Arrays.asList(userGroups));
			finalValues.add(ruleId);

			dbService.update(finalSql, finalValues.toArray());
		}
	}

	/**
	 * Adds a rule value for users or user groups
	 *
	 * @param users user identifiers in the format user id-username
	 * @param userGroups user group ids
	 * @param ruleKey the rule key
	 * @param ruleValue the rule value
	 * @throws SQLException
	 */
	public void addRuleValue(String[] users, Integer[] userGroups,
			String ruleKey, String ruleValue) throws SQLException {

		logger.debug("Entering addRuleValue: ruleId='{}', ruleValue='{}'",
				ruleKey, ruleValue);

		if (ruleKey == null) {
			logger.warn("Cannot add rule value. Rule not specified");
			return;
		}

		Integer ruleId = Integer.valueOf(StringUtils.substringBefore(ruleKey, "-"));
		//rule name won't be needed once rule id columns completely replace rule name in foreign keys
		String ruleName = StringUtils.substringAfter(ruleKey, "-");

		//add user rule values
		if (users != null) {
			String sql = "INSERT INTO ART_USER_RULES (RULE_VALUE_KEY, USER_ID, USERNAME, RULE_ID,"
					+ " RULE_NAME, RULE_VALUE)"
					+ " VALUES(" + StringUtils.repeat("?", ",", 6) + ")";

			for (String user : users) {
				Integer userId = Integer.valueOf(StringUtils.substringBefore(user, "-"));
				//username won't be needed once user id columns completely replace username in foreign keys
				String username = StringUtils.substringAfter(user, "-");

				dbService.update(sql, ArtUtils.getUniqueId(), userId, username,
						ruleId, ruleName, ruleValue);
			}
		}

		//add user group rule values
		if (userGroups != null) {
			String sql = "INSERT INTO ART_USER_GROUP_RULES (RULE_VALUE_KEY,"
					+ " USER_GROUP_ID, RULE_ID, RULE_NAME, RULE_VALUE)"
					+ " VALUES(" + StringUtils.repeat("?", ",", 5) + ")";

			for (Integer userGroupId : userGroups) {
				dbService.update(sql, ArtUtils.getUniqueId(), userGroupId,
						ruleId, ruleName, ruleValue);
			}
		}
	}

	/**
	 * Updates a user rule value
	 *
	 * @param value the updated rule value
	 * @throws SQLException
	 */
	public void updateUserRuleValue(UserRuleValue value) throws SQLException {
		logger.debug("Entering updateUserRuleValue: value={}", value);

		String sql = "UPDATE ART_USER_RULES SET RULE_VALUE=?"
				+ " WHERE RULE_VALUE_KEY=?";

		Object[] values = {
			value.getRuleValue(),
			value.getRuleValueKey()
		};

		dbService.update(sql, values);
	}

	/**
	 * Updates a user group rule value
	 *
	 * @param value the updated rule value
	 * @throws SQLException
	 */
	public void updateUserGroupRuleValue(UserGroupRuleValue value) throws SQLException {
		logger.debug("Entering updateUserGroupRuleValue: value={}", value);

		String sql = "UPDATE ART_USER_GROUP_RULES SET RULE_VALUE=?"
				+ " WHERE RULE_VALUE_KEY=?";

		Object[] values = {
			value.getRuleValue(),
			value.getRuleValueKey()
		};

		dbService.update(sql, values);
	}
}
