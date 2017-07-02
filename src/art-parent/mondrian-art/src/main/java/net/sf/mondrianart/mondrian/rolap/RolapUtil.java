/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2001-2005 Julian Hyde
// Copyright (C) 2005-2012 Pentaho and others
// All Rights Reserved.
//
// jhyde, 22 December, 2001
*/
package net.sf.mondrianart.mondrian.rolap;

import net.sf.mondrianart.mondrian.calc.ExpCompiler;
import net.sf.mondrianart.mondrian.olap.*;
import net.sf.mondrianart.mondrian.olap.Member;
import net.sf.mondrianart.mondrian.olap.fun.FunUtil;
import net.sf.mondrianart.mondrian.resource.MondrianResource;
import net.sf.mondrianart.mondrian.server.*;
import net.sf.mondrianart.mondrian.spi.Dialect;

import org.apache.log4j.Logger;

import org.eigenbase.util.property.StringProperty;

import java.io.*;
import java.lang.reflect.*;
import java.sql.SQLException;
import java.util.*;

import javax.sql.DataSource;

/**
 * Utility methods for classes in the <code>mondrian.rolap</code> package.
 *
 * @author jhyde
 * @since 22 December, 2001
 */
public class RolapUtil {
    public static final Logger MDX_LOGGER = Logger.getLogger("net.sf.mondrianart.mondrian.mdx");
    public static final Logger SQL_LOGGER = Logger.getLogger("net.sf.mondrianart.mondrian.sql");
    public static final Logger MONITOR_LOGGER =
        Logger.getLogger("net.sf.mondrianart.mondrian.server.monitor");
    public static final Logger PROFILE_LOGGER =
        Logger.getLogger("net.sf.mondrianart.mondrian.profile");

    static final Logger LOGGER = Logger.getLogger(RolapUtil.class);
    private static Semaphore querySemaphore;

    /**
     * Special cell value indicates that the value is not in cache yet.
     */
    public static final Object valueNotReadyException = new Double(0);

    /*
     * Hook to run when a query is executed. This should not be
     * used at runtime but only for testing.
     */
    private static ExecuteQueryHook queryHook = null;

    /**
     * Special value represents a null key.
     */
    public static final Comparable<?> sqlNullValue = new RolapUtilComparable();

    /**
     * Wraps a schema reader in a proxy so that each call to schema reader
     * has a locus for profiling purposes.
     *
     * @param connection Connection
     * @param schemaReader Schema reader
     * @return Wrapped schema reader
     */
    public static SchemaReader locusSchemaReader(
        RolapConnection connection,
        final SchemaReader schemaReader)
    {
        final Statement statement = connection.getInternalStatement();
        final Execution execution = new Execution(statement, 0);
        final Locus locus =
            new Locus(
                execution,
                "Schema reader",
                null);
        return (SchemaReader) Proxy.newProxyInstance(
            SchemaReader.class.getClassLoader(),
            new Class[]{SchemaReader.class},
            new InvocationHandler() {
                public Object invoke(
                    Object proxy,
                    Method method,
                    Object[] args)
                    throws Throwable
                {
                    Locus.push(locus);
                    try {
                        return method.invoke(schemaReader, args);
                    } finally {
                        Locus.pop(locus);
                    }
                }
            }
        );
    }

    /**
     * Sets the query-execution hook used by tests. This method and
     * {@link #setHook(mondrian.rolap.RolapUtil.ExecuteQueryHook)} are
     * synchronized to ensure a memory barrier.
     *
     * @return Query execution hook
     */
    public static synchronized ExecuteQueryHook getHook() {
        return queryHook;
    }

    public static synchronized void setHook(ExecuteQueryHook hook) {
        queryHook = hook;
    }

    public static final class RolapUtilComparable
        implements Comparable, Serializable
    {
        private static final long serialVersionUID = -2595758291465179116L;
        public boolean equals(Object o) {
            return o == this;
        }
        public int hashCode() {
            return super.hashCode();
        }
        public String toString() {
            return "#null";
        }
        public int compareTo(Object o) {
            return o == this ? 0 : -1;
        }
    }

    /**
     * A comparator singleton instance which can handle the presence of
     * {@link RolapUtilComparable} instances in a collection.
     */
    public static final Comparator ROLAP_COMPARATOR =
        new RolapUtilComparator();

    private static final class RolapUtilComparator<T extends Comparable<T>>
        implements Comparator<T>
    {
        public int compare(T o1, T o2) {
            try {
                return o1.compareTo(o2);
            } catch (ClassCastException cce) {
                if (o1 instanceof RolapUtilComparable) {
                    return -1;
                }
                if (o2 instanceof RolapUtilComparable) {
                    return 1;
                }
                throw new MondrianException(cce);
            }
        }
    }

    /**
     * Runtime NullMemberRepresentation property change not taken into
     * consideration
     */
    private static String mdxNullLiteral = null;
    public static final String sqlNullLiteral = "null";

    public static String mdxNullLiteral() {
        if (mdxNullLiteral == null) {
            reloadNullLiteral();
        }
        return mdxNullLiteral;
    }

    public static void reloadNullLiteral() {
        mdxNullLiteral =
            MondrianProperties.instance().NullMemberRepresentation.get();
    }

    /**
     * Names of classes of drivers we've loaded (or have tried to load).
     *
     * <p>NOTE: Synchronization policy: Lock the {@link RolapConnection} class
     * before modifying or using this member.
     */
    private static final Set<String> loadedDrivers = new HashSet<String>();

    static RolapMember[] toArray(List<RolapMember> v) {
        return v.isEmpty()
            ? new RolapMember[0]
            : v.toArray(new RolapMember[v.size()]);
    }

    static RolapMember lookupMember(
        MemberReader reader,
        List<Id.Segment> uniqueNameParts,
        boolean failIfNotFound)
    {
        RolapMember member =
            lookupMemberInternal(
                uniqueNameParts, null, reader, failIfNotFound);
        if (member != null) {
            return member;
        }

        // If this hierarchy has an 'all' member, we can omit it.
        // For example, '[Gender].[(All Gender)].[F]' can be abbreviated
        // '[Gender].[F]'.
        final List<RolapMember> rootMembers = reader.getRootMembers();
        if (rootMembers.size() == 1) {
            final RolapMember rootMember = rootMembers.get(0);
            if (rootMember.isAll()) {
                member =
                    lookupMemberInternal(
                        uniqueNameParts, rootMember, reader, failIfNotFound);
            }
        }
        return member;
    }

    private static RolapMember lookupMemberInternal(
        List<Id.Segment> segments,
        RolapMember member,
        MemberReader reader,
        boolean failIfNotFound)
    {
        for (Id.Segment segment : segments) {
            List<RolapMember> children;
            if (member == null) {
                children = reader.getRootMembers();
            } else {
                children = new ArrayList<RolapMember>();
                reader.getMemberChildren(member, children);
                member = null;
            }
            for (RolapMember child : children) {
                if (child.getName().equals(segment.name)) {
                    member = child;
                    break;
                }
            }
            if (member == null) {
                break;
            }
        }
        if (member == null && failIfNotFound) {
            throw MondrianResource.instance().MdxCantFindMember.ex(
                Util.implode(segments));
        }
        return member;
    }

    /**
     * Executes a query, printing to the trace log if tracing is enabled.
     *
     * <p>If the query fails, it wraps the {@link SQLException} in a runtime
     * exception with <code>message</code> as description, and closes the result
     * set.
     *
     * <p>If it succeeds, the caller must call the {@link SqlStatement#close}
     * method of the returned {@link SqlStatement}.
     *
     * @param dataSource DataSource
     * @param sql SQL string
     * @param locus Locus of execution
     * @return ResultSet
     */
    public static SqlStatement executeQuery(
        DataSource dataSource,
        String sql,
        Locus locus)
    {
        return executeQuery(dataSource, sql, null, 0, 0, locus, -1, -1);
    }

    /**
     * Executes a query.
     *
     * <p>If the query fails, it wraps the {@link SQLException} in a runtime
     * exception with <code>message</code> as description, and closes the result
     * set.
     *
     * <p>If it succeeds, the caller must call the {@link SqlStatement#close}
     * method of the returned {@link SqlStatement}.
     *
     *
     * @param dataSource DataSource
     * @param sql SQL string
     * @param types Suggested types of columns, or null;
     *     if present, must have one element for each SQL column;
     *     each not-null entry overrides deduced JDBC type of the column
     * @param maxRowCount Maximum number of rows to retrieve, <= 0 if unlimited
     * @param firstRowOrdinal Ordinal of row to skip to (1-based), or 0 to
     *   start from beginning
     * @param locus Execution context of this statement
     * @param resultSetType Result set type, or -1 to use default
     * @param resultSetConcurrency Result set concurrency, or -1 to use default
     * @return ResultSet
     */
    public static SqlStatement executeQuery(
        DataSource dataSource,
        String sql,
        List<SqlStatement.Type> types,
        int maxRowCount,
        int firstRowOrdinal,
        Locus locus,
        int resultSetType,
        int resultSetConcurrency)
    {
        SqlStatement stmt =
            new SqlStatement(
                dataSource, sql, types, maxRowCount, firstRowOrdinal, locus,
                resultSetType, resultSetConcurrency);
        stmt.execute();
        return stmt;
    }

    /**
     * Raises an alert that native SQL evaluation could not be used
     * in a case where it might have been beneficial, but some
     * limitation in Mondrian's implementation prevented it.
     * (Do not call this in cases where native evaluation would
     * have been wasted effort.)
     *
     * @param functionName name of function for which native evaluation
     * was skipped
     *
     * @param reason reason why native evaluation was skipped
     */
    public static void alertNonNative(
        String functionName,
        String reason)
        throws NativeEvaluationUnsupportedException
    {
        // No i18n for log message, but yes for excn
        String alertMsg =
            "Unable to use native SQL evaluation for '" + functionName
            + "'; reason:  " + reason;

        StringProperty alertProperty =
            MondrianProperties.instance().AlertNativeEvaluationUnsupported;
        String alertValue = alertProperty.get();

        if (alertValue.equalsIgnoreCase(
                org.apache.log4j.Level.WARN.toString()))
        {
            LOGGER.warn(alertMsg);
        } else if (alertValue.equalsIgnoreCase(
                org.apache.log4j.Level.ERROR.toString()))
        {
            LOGGER.error(alertMsg);
            throw MondrianResource.instance().NativeEvaluationUnsupported.ex(
                functionName);
        }
    }

    /**
     * Loads a set of JDBC drivers.
     *
     * @param jdbcDrivers A string consisting of the comma-separated names
     *  of JDBC driver classes. For example
     *  <code>"sun.jdbc.odbc.JdbcOdbcDriver,com.mysql.jdbc.Driver"</code>.
     */
    public static synchronized void loadDrivers(String jdbcDrivers) {
        StringTokenizer tok = new StringTokenizer(jdbcDrivers, ",");
        while (tok.hasMoreTokens()) {
            String jdbcDriver = tok.nextToken();
            if (loadedDrivers.add(jdbcDriver)) {
                try {
                    Class.forName(jdbcDriver).newInstance();
                    LOGGER.info(
                        "Mondrian: JDBC driver "
                        + jdbcDriver + " loaded successfully");
                } catch (Exception e) {
                    LOGGER.warn(
                        "Mondrian: Warning: JDBC driver "
                        + jdbcDriver + " not found");
                }
            }
        }
    }

    /**
     * Creates a compiler which will generate programs which will test
     * whether the dependencies declared via
     * {@link net.sf.mondrianart.mondrian.calc.Calc#dependsOn(Hierarchy)} are accurate.
     */
    public static ExpCompiler createDependencyTestingCompiler(
        ExpCompiler compiler)
    {
        return new RolapDependencyTestingEvaluator.DteCompiler(compiler);
    }

    /**
     * Locates a member specified by its member name, from an array of
     * members.  If an exact match isn't found, but a matchType of BEFORE
     * or AFTER is specified, then the closest matching member is returned.
     *
     * @param members array of members to search from
     * @param parent parent member corresponding to the member being searched
     * for
     * @param level level of the member
     * @param searchName member name
     * @param matchType match type
     * @param caseInsensitive if true, use case insensitive search (if
     * applicable) when when doing exact searches
     *
     * @return matching member (if it exists) or the closest matching one
     * in the case of a BEFORE or AFTER search
     */
    public static Member findBestMemberMatch(
        List<? extends Member> members,
        RolapMember parent,
        RolapLevel level,
        Id.Segment searchName,
        MatchType matchType,
        boolean caseInsensitive)
    {
        switch (matchType) {
        case FIRST:
            return members.get(0);
        case LAST:
            return members.get(members.size() - 1);
        default:
            // fall through
        }
        // create a member corresponding to the member we're trying
        // to locate so we can use it to hierarchically compare against
        // the members array
        Member searchMember =
            level.getHierarchy().createMember(
                parent, level, searchName.name, null);
        Member bestMatch = null;
        for (Member member : members) {
            int rc;
            if (searchName.quoting == Id.Quoting.KEY
                && member instanceof RolapMember)
            {
                if (((RolapMember) member).getKey().toString().equals(
                        searchName.name))
                {
                    return member;
                }
            }
            if (matchType.isExact()) {
                if (caseInsensitive) {
                    rc = Util.compareName(member.getName(), searchName.name);
                } else {
                    rc = member.getName().compareTo(searchName.name);
                }
            } else {
                rc =
                    FunUtil.compareSiblingMembers(
                        member,
                        searchMember);
            }
            if (rc == 0) {
                return member;
            }
            if (matchType == MatchType.BEFORE) {
                if (rc < 0
                    && (bestMatch == null
                        || FunUtil.compareSiblingMembers(member, bestMatch)
                        > 0))
                {
                    bestMatch = member;
                }
            } else if (matchType == MatchType.AFTER) {
                if (rc > 0
                    && (bestMatch == null
                        || FunUtil.compareSiblingMembers(member, bestMatch)
                        < 0))
                {
                    bestMatch = member;
                }
            }
        }
        if (matchType.isExact()) {
            return null;
        }
        return bestMatch;
    }

    public static MondrianDef.Relation convertInlineTableToRelation(
        MondrianDef.InlineTable inlineTable,
        final Dialect dialect)
    {
        MondrianDef.View view = new MondrianDef.View();
        view.alias = inlineTable.alias;

        final int columnCount = inlineTable.columnDefs.array.length;
        List<String> columnNames = new ArrayList<String>();
        List<String> columnTypes = new ArrayList<String>();
        for (int i = 0; i < columnCount; i++) {
            columnNames.add(inlineTable.columnDefs.array[i].name);
            columnTypes.add(inlineTable.columnDefs.array[i].type);
        }
        List<String[]> valueList = new ArrayList<String[]>();
        for (MondrianDef.Row row : inlineTable.rows.array) {
            String[] values = new String[columnCount];
            for (MondrianDef.Value value : row.values) {
                final int columnOrdinal = columnNames.indexOf(value.column);
                if (columnOrdinal < 0) {
                    throw Util.newError(
                        "Unknown column '" + value.column + "'");
                }
                values[columnOrdinal] = value.cdata;
            }
            valueList.add(values);
        }
        view.addCode(
            "generic",
            dialect.generateInline(
                columnNames,
                columnTypes,
                valueList));
        return view;
    }

    public static RolapMember strip(RolapMember member) {
        if (member instanceof RolapCubeMember) {
            return ((RolapCubeMember) member).getRolapMember();
        }
        return member;
    }

    public static ExpCompiler createProfilingCompiler(ExpCompiler compiler) {
        return new RolapProfilingEvaluator.ProfilingEvaluatorCompiler(
            compiler);
    }

    /**
     * Writes to a string and also to an underlying writer.
     */
    public static class TeeWriter extends FilterWriter {
        StringWriter buf = new StringWriter();
        public TeeWriter(Writer out) {
            super(out);
        }

        /**
         * Returns everything which has been written so far.
         */
        public String toString() {
            return buf.toString();
        }

        /**
         * Returns the underlying writer.
         */
        public Writer getWriter() {
            return out;
        }

        public void write(int c) throws IOException {
            super.write(c);
            buf.write(c);
        }

        public void write(char cbuf[]) throws IOException {
            super.write(cbuf);
            buf.write(cbuf);
        }

        public void write(char cbuf[], int off, int len) throws IOException {
            super.write(cbuf, off, len);
            buf.write(cbuf, off, len);
        }

        public void write(String str) throws IOException {
            super.write(str);
            buf.write(str);
        }

        public void write(String str, int off, int len) throws IOException {
            super.write(str, off, len);
            buf.write(str, off, len);
        }
    }

    /**
     * Writer which throws away all input.
     */
    private static class NullWriter extends Writer {
        public void write(char cbuf[], int off, int len) throws IOException {
        }

        public void flush() throws IOException {
        }

        public void close() throws IOException {
        }
    }

    /**
     * Gets the semaphore which controls how many people can run queries
     * simultaneously.
     */
    static synchronized Semaphore getQuerySemaphore() {
        if (querySemaphore == null) {
            int queryCount = MondrianProperties.instance().QueryLimit.get();
            querySemaphore = new Semaphore(queryCount);
        }
        return querySemaphore;
    }

    /**
     * Creates a dummy evaluator.
     */
    public static Evaluator createEvaluator(
        Statement statement)
    {
        Execution dummyExecution = new Execution(statement, 0);
        final RolapResult result = new RolapResult(dummyExecution, false);
        return result.getRootEvaluator();
    }

    /**
     * A <code>Semaphore</code> is a primitive for process synchronization.
     *
     * <p>Given a semaphore initialized with <code>count</code>, no more than
     * <code>count</code> threads can acquire the semaphore using the
     * {@link #enter} method. Waiting threads block until enough threads have
     * called {@link #leave}.
     */
    static class Semaphore {
        private int count;
        Semaphore(int count) {
            if (count < 0) {
                count = Integer.MAX_VALUE;
            }
            this.count = count;
        }
        synchronized void enter() {
            if (count == Integer.MAX_VALUE) {
                return;
            }
            if (count == 0) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw Util.newInternal(e, "while waiting for semaphore");
                }
            }
            Util.assertTrue(count > 0);
            count--;
        }
        synchronized void leave() {
            if (count == Integer.MAX_VALUE) {
                return;
            }
            count++;
            notify();
        }
    }

    static interface ExecuteQueryHook {
        void onExecuteQuery(String sql);
    }

}

// End RolapUtil.java
