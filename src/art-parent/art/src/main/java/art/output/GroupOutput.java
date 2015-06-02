/*
 * Copyright (C) 2015 Enrico Liboni <eliboni@users.sourceforge.net>
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
package art.output;

import art.servlets.Config;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 *
 * @author Timothy Anyona
 */
public abstract class GroupOutput {
	PrintWriter out;
	StringBuilder mainHeader = new StringBuilder();
	// temporary string used to store Main Header Values
	StringBuilder subHeader = new StringBuilder();

	public GroupOutput() {
	}

	/**
	 * Output report header. Report width is 80% of the page
	 */
	public abstract void header();

	/**
	 * Output report header with explicit report width
	 *
	 * @param width report width as percentage of page
	 */
	public abstract void header(int width);

	/**
	 *
	 * @param value
	 */
	public abstract void addCellToMainHeader(String value);

	/**
	 *
	 * @param value
	 */
	public abstract void addCellToSubHeader(String value);

	/**
	 *
	 */
	public abstract void printMainHeader();

	/**
	 *
	 */
	public abstract void printSubHeader();

	public abstract void separator();

	/**
	 *
	 * @param value
	 */
	public abstract void addCellToLine(String value);

	/**
	 *
	 * @param value
	 * @param numOfCells
	 */
	public abstract void addCellToLine(String value, int numOfCells);

	/**
	 *
	 * @param value
	 * @param cssclass
	 * @param numOfCells
	 */
	public abstract void addCellToLine(String value, String cssclass, int numOfCells);

	/**
	 *
	 * @param value
	 * @param cssclass
	 * @param align
	 * @param numOfCells
	 */
	public abstract void addCellToLine(String value, String cssclass, String align, int numOfCells);

	/**
	 *
	 */
	public abstract void beginLines();

	/**
	 *
	 */
	public abstract void endLines();

	/**
	 *
	 */
	public abstract void newLine();

	/**
	 *
	 */
	public abstract void footer();
	
	/**
	 * Generate a group report
	 *
	 * @param out
	 * @param rs needs to be a scrollable resultset
	 * @param splitCol
	 * @return number of rows output
	 * @throws SQLException
	 */
	public static int generateGroupReport(PrintWriter out, ResultSet rs, int splitCol) throws SQLException {
		
		ResultSetMetaData rsmd = rs.getMetaData();
		
		int colCount = rsmd.getColumnCount();
		int i;
		int counter = 0;
		GroupHtmlOutput o = new GroupHtmlOutput(out);
		String tmpstr;
		StringBuffer cmpStr; // temporary string used to compare values
		StringBuffer tmpCmpStr; // temporary string used to compare values

		// Report, is intended to be something like that:
		/*
		 * ------------------------------------- | Attr1 | Attr2 | Attr3 | //
		 * Main header ------------------------------------- | Value1 | Value2 |
		 * Value3 | // Main Data -------------------------------------
		 *
		 * -----------------------------... | SubAttr1 | Subattr2 |... // Sub
		 * Header -----------------------------... | SubValue1.1 | SubValue1.2
		 * |... // Sub Data -----------------------------... | SubValue2.1 |
		 * SubValue2.2 |... -----------------------------...
		 * ................................ ................................
		 * ................................
		 *
		 * etc...
		 */
		// Build main header HTML
		for (i = 0; i < (splitCol); i++) {
			tmpstr = rsmd.getColumnLabel(i + 1);
			o.addCellToMainHeader(tmpstr);
		}
		// Now the header is completed

		// Build the Sub Header
		for (; i < colCount; i++) {
			tmpstr = rsmd.getColumnLabel(i + 1);
			o.addCellToSubHeader(tmpstr);
		}

		int maxRows = Config.getMaxRows("htmlreport");

		while (rs.next() && counter < maxRows) {
			// Separators
			out.println("<br><hr style=\"width:90%;height:1px\"><br>");

			// Output Main Header and Main Data
			o.header(90);
			o.printMainHeader();
			o.beginLines();
			cmpStr = new StringBuffer();

			// Output Main Data (only one row, obviously)
			for (i = 0; i < splitCol; i++) {
				o.addCellToLine(rs.getString(i + 1));
				cmpStr.append(rs.getString(i + 1));
			}
			o.endLines();
			o.footer();

			// Output Sub Header and Sub Data
			o.header(80);
			o.printSubHeader();
			o.beginLines();

			// Output Sub Data (first line)
			for (; i < colCount; i++) {
				o.addCellToLine(rs.getString(i + 1));
			}

			boolean currentMain = true;
			while (currentMain && counter < maxRows) {  // next line
				// Get Main Data in order to compare it
				if (rs.next()) {
					counter++;
					tmpCmpStr = new StringBuffer();

					for (i = 0; i < splitCol; i++) {
						tmpCmpStr.append(rs.getString(i + 1));
					}

					if (tmpCmpStr.toString().equals(cmpStr.toString()) == true) { // same Main
						o.newLine();
						// Add data lines
						for (; i < colCount; i++) {
							o.addCellToLine(rs.getString(i + 1));
						}
					} else {
						o.endLines();
						o.footer();
						currentMain = false;
						rs.previous();
					}
				} else {
					currentMain = false;
					// The outer and inner while will exit
				}
			}
		}

		if (!(counter < maxRows)) {
			o.newLine();
			o.addCellToLine("<blink>Too many rows (>" + maxRows
					+ "). Data not completed. Please narrow your search.</blink>", "qeattr", "left", colCount);
		}

		o.endLines();
		o.footer();

		return counter + 1; // number of rows
	}
	
}
