/*
 * Copyright (C) 2016 Enrico Liboni <eliboni@users.sourceforge.net>
 *
 * This file is part of ART.
 *
 * ART is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, version 2 of the License.
 *
 * ART is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ART. If not, see <http://www.gnu.org/licenses/>.
 */
package art.chart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.MeterInterval;
import org.jfree.chart.plot.MeterPlot;
import org.jfree.data.Range;
import org.jfree.data.general.DefaultValueDataset;

/**
 * Provides methods for working with speedometer charts
 * 
 * @author Timothy Anyona
 */
public class SpeedometerChart extends Chart {

	private static final long serialVersionUID = 1L;
	private double minValue;
	private double maxValue;
	private String unitsDescription;
	private final Map<Integer, Double> rangeValues = new HashMap<>();
	private final Map<Integer, String> rangeColors = new HashMap<>();
	private final Map<Integer, String> rangeDescriptions = new HashMap<>();
	private final Map<Integer, Range> rangeRanges = new HashMap<>();
	private int rangeCount;

	public SpeedometerChart() {
		setType("meter"); //cewolf chart type as per <cewolf:chart type attribute. not case sensitive. also listed in net.sf.cewolfart.taglib.ChartTypes class code
	}

	//prepare graph data structures with query results
	@Override
	public void fillDataset(ResultSet rs) throws SQLException {
		Objects.requireNonNull(rs, "rs must not be null");

		DefaultValueDataset dataset = new DefaultValueDataset();
		
		//resultset structure
		//dataValue, minValue, maxValue, unitsDescription [, ranges]
		
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();

		if (rs.next()) {
			dataset.setValue(rs.getDouble(1));

			minValue = rs.getDouble(2);
			maxValue = rs.getDouble(3);
			unitsDescription = rs.getString(4);

			if (columnCount > 4) {
				//ranges have been specified
				rangeCount = 0;
				for (int i = 5; i <= columnCount; i++) {
					String rangeSpec = rs.getString(i);
					String[] rangeDetails = StringUtils.split(rangeSpec, ":");
					if (rangeDetails != null && rangeDetails.length == 3) {
						rangeCount++;
						String valuePart = rangeDetails[0];
						double rangeValue;
						if (valuePart.contains("%")) {
							rangeValue = Double.parseDouble(valuePart.replace("%", ""));
							rangeValue = minValue + (maxValue - minValue) * rangeValue / 100.0;
						} else {
							rangeValue = Double.parseDouble(valuePart);
						}

						rangeValues.put(rangeCount, rangeValue);
						rangeColors.put(rangeCount, StringUtils.trim(rangeDetails[1]));
						rangeDescriptions.put(rangeCount, StringUtils.trim(rangeDetails[2]));
					}
				}

				//build chart ranges
				double rangeMin;
				double rangeMax;
				for (int i = 1; i <= rangeCount; i++) {
					if (i == 1) {
						rangeMin = minValue;
						rangeMax = rangeValues.get(i);
					} else {
						rangeMin = rangeValues.get(i - 1);
						rangeMax = rangeValues.get(i);
					}
					Range range = new Range(rangeMin, rangeMax);
					rangeRanges.put(i, range);
				}
			}
		}

		setDataset(dataset);
	}

	@Override
	public void processChart(JFreeChart chart, Map<String, String> params) {
		MeterPlot plot = (MeterPlot) chart.getPlot();

		plot.setRange(new Range(minValue, maxValue));
		plot.setUnits(unitsDescription);

		//http://www.jfree.org/phpBB2/viewtopic.php?f=3&t=22082
//		chart.setBackgroundPaint(Color.LIGHT_GRAY);
//		plot.setDrawBorder(true);
//		plot.setBackgroundPaint(Color.BLUE);
//		plot.setNeedlePaint(Color.RED);
//		plot.setDialBackgroundPaint(Color.ORANGE);
		plot.setDialBackgroundPaint(Color.LIGHT_GRAY);
		plot.setNeedlePaint(Color.DARK_GRAY);

		//add color ranges
		int i;
		String description;
		Color rangeColor;
		for (i = 1; i <= rangeCount; i++) {
			description = rangeDescriptions.get(i);
			rangeColor = Color.decode(rangeColors.get(i));
			MeterInterval interval = new MeterInterval(description, rangeRanges.get(i), rangeColor, new BasicStroke(2.0F), null);
			plot.addInterval(interval);
		}

		//set tick interval. display interval every 10 percent
		//by default ticks are displayed every 10 units. can be too many with large values
		double tickInterval = (maxValue - minValue) / 10.0;
		plot.setTickSize(tickInterval);
	}
}
