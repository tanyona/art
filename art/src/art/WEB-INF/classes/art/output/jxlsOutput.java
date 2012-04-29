package art.output;

import java.sql.*;
import java.util.*;
import java.io.*;
import java.text.SimpleDateFormat;

import art.servlets.ArtDBCP;
import art.utils.*;

import net.sf.jxls.transformer.XLSTransformer;
import net.sf.jxls.report.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to generate formatted excel workbooks using the jxls library.
 * Doesn't implement artOutputInterface like other output classes
 * 
 * @author Timothy Anyona
 */
public class jxlsOutput {

    final static Logger logger = LoggerFactory.getLogger(jxlsOutput.class);
    
    String fullOutputFileName = "-No File";
    String queryName;
    String userName;
    String output;
    String y_m_d;
    String h_m_s;
    String exportPath;
    PrintWriter htmlout;

    /**
     * Set html output object
     * @param o html output object
     */
    public void setWriter(PrintWriter o) {
        htmlout = o;
    }

    /**
     * Set the directory where the file is to be saved
     * @param s directory where the file is to be saved
     */
    public void setExportPath(String s) {
        exportPath = s;
    }

    /**
     * Get the full filename where the output has been saved
     * @return full filename where the output has been saved
     */
    public String getFileName() {
        return fullOutputFileName;
    }

    /**
     * Set query name to be used in file name
     * @param s query name to be used in file name
     */
    public void setQueryName(String s) {
        queryName = s;
    }

    /**
     * Set username to be used in file name
     * @param s username to be used in file name
     */
    public void setUserName(String s) {
        userName = s;
    }

    /**
     * Set output format
     * @param s output format
     */
    public void setOutput(String s) {
        output = s;
    }

    /**
     * Generate output and set final filename
     * 
     * @param rs query resultset
     * @param queryId query id
     * @param inlineParams inline parameters
     * @param multiParams multi parameters
     */
    public void createFile(ResultSet rs, int queryId, Map<String,String> inlineParams, Map<String,String[]> multiParams) {
        Connection connQuery = null;
        Connection connArt = null;

        try {
            String templateFileName;
            String extension = "xls";
            int index;
            int datasourceId;
            String querySql;
            String templatesPath;
            String fullTemplateFileName;

            //get query datasource and template file name
            connArt = ArtDBCP.getConnection();
            ArtQuery aq = new ArtQuery();
            aq.create(connArt, queryId);
            templateFileName = aq.getTemplate();
            datasourceId = aq.getDatabaseId();
            querySql = aq.getText();

            //set report parameters
            HashMap<String,String> iParams = new HashMap<String,String>();
            iParams.putAll(inlineParams); //pass any inline parameters

            //process multi parameters to obtain parameter names instead of parameter identifiers
            HashMap<String,String> mParams = new HashMap<String,String>();
            PreparedQuery pq = new PreparedQuery();
            pq.setQueryId(queryId);
            pq.setMultiParams(multiParams);
            mParams.putAll(pq.getJxlsMultiParams(querySql));

            //set objects to be passed to jxls
            Map<String,Object> beans = new HashMap<String,Object>();
            beans.put("inlineParams", iParams);
            beans.put("multiParams", mParams);

            if (rs == null) {
                //pass connection to template query
                connQuery = ArtDBCP.getConnection(datasourceId);
                ReportManager reportManager = new ArtJxlsReportManager(connQuery);
                beans.put("rm", reportManager);
            } else {
                //use recordset based on art query 
                ArtJxlsResultSetCollection rsc = new ArtJxlsResultSetCollection(rs, false, true);
                beans.put("results", rsc);
            }

            //Build output filename 
            Calendar cal = Calendar.getInstance();
            java.util.Date today = cal.getTime();

            String dateFormat = "yyyy_MM_dd";
            SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat);
            y_m_d = dateFormatter.format(today);

            String timeFormat = "HH_mm_ss";
            SimpleDateFormat timeFormatter = new SimpleDateFormat(timeFormat);
            h_m_s = timeFormatter.format(today);

            index = templateFileName.lastIndexOf(".");
            if (index > -1) {
                //extension may be xlsx
                extension = templateFileName.substring(index);
            }

            String fileName = userName + "-" + queryName + "-" + y_m_d + "-" + h_m_s + extension;
            fileName=ArtDBCP.cleanFileName(fileName); //replace characters that would make an invalid filename
            fullOutputFileName = exportPath + fileName;

            templatesPath = ArtDBCP.getTemplatesPath();
            fullTemplateFileName = templatesPath + templateFileName;

            //generate output
            XLSTransformer transformer = new XLSTransformer();
            transformer.transformXLS(fullTemplateFileName, beans, fullOutputFileName);

            //display link to access report if run interactively			
            if (htmlout != null) {
                htmlout.println("<p><div align=\"Center\"><table border=\"0\" width=\"90%\">");
                htmlout.println("<tr><td colspan=2 class=\"data\" align=\"center\" >"
                        + "<a  type=\"application/octet-stream\" href=\"../export/" + fileName + "\"> "
                        + fileName + "</a>"
                        + "</td></tr>");
                htmlout.println("</table></div></p>");
            }

        } catch (Exception e) {
            logger.error("Error",e);
            if (htmlout != null) {
                //display error message on browser
                htmlout.println("<b>Error while generating report:</b> <p>" + e + "</p>");
            }
        } finally {
            try {
                if (connQuery != null) {
                    connQuery.close();
                }
                if (connArt != null) {
                    connArt.close();
                }
            } catch (Exception e) {
                logger.error("Error",e);
            }
        }
    }
}