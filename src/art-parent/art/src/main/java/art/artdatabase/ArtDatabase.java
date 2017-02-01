/*
 * ART. A Reporting Tool.
 * Copyright (C) 2017 Enrico Liboni <eliboni@users.sf.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package art.artdatabase;

import art.datasource.DatasourceInfo;
import art.enums.ConnectionPoolLibrary;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

/**
 * Represents the art database configuration
 *
 * @author Timothy Anyona
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArtDatabase extends DatasourceInfo implements Serializable {

	private static final long serialVersionUID = 1L;
	private int maxPoolConnections; //setting used by art database and all report datasources
	private ConnectionPoolLibrary connectionPoolLibrary; //setting used by art database and all report datasources
	public static final int ART_DATABASE_DATASOURCE_ID = -1; //"datasource id" for the art database in the connection pool map
	private static final String ART_DATABASE_DATASOURCE_NAME = "ART Database"; //"datasource name" for the art database in the connection pool map

	@Override
	@JsonIgnore
	public int getDatasourceId() {
		return ART_DATABASE_DATASOURCE_ID;
	}

	@Override
	@JsonIgnore
	public String getName() {
		return ART_DATABASE_DATASOURCE_NAME;
	}
	
	/**
	 * @return the maxPoolConnections
	 */
	public int getMaxPoolConnections() {
		return maxPoolConnections;
	}

	/**
	 * @param maxPoolConnections the maxPoolConnections to set
	 */
	public void setMaxPoolConnections(int maxPoolConnections) {
		this.maxPoolConnections = maxPoolConnections;
	}

	/**
	 * @return the connectionPoolLibrary
	 */
	public ConnectionPoolLibrary getConnectionPoolLibrary() {
		return connectionPoolLibrary;
	}

	/**
	 * @param connectionPoolLibrary the connectionPoolLibrary to set
	 */
	public void setConnectionPoolLibrary(ConnectionPoolLibrary connectionPoolLibrary) {
		this.connectionPoolLibrary = connectionPoolLibrary;
	}

}
