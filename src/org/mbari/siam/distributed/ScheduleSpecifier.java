// MBARI copyright 2002
package org.mbari.siam.distributed;

import java.util.Calendar;
import java.util.StringTokenizer;
import java.lang.NumberFormatException;
import java.util.TimeZone;
import java.util.Date;
import java.util.Vector;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.text.DateFormat;
import java.io.Serializable;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;

/**
 * The ScheduleSpecifier class implements the portion of a schedule (entry) that
 * specifies at what times the job portion of the schedule is to be executed.
 */
public class ScheduleSpecifier implements Serializable, Importable {

    public static final String DEFAULT_SCHEDULE_NAME = "Default";

    // Constants
    /** Log4j logger */
    protected static Logger _logger = Logger.getLogger(ScheduleSpecifier.class);

    // Limits
    /**
     * Min length of valid entry 10 fields + 9 whitespace
     */
    public static final int MIN_LENGTH = 19;

    /** Max day of year day */
    public static final int MAX_DAY_OF_YEAR = 365;

    /** Min day of year */
    public static final int MIN_DAY_OF_YEAR = 0;

    /** Max month */
    public static final int MAX_MONTH = 11;

    /** Min month */
    public static final int MIN_MONTH = 0;

    /** Max day of month */
    public static final int MAX_DAY_OF_MONTH = 31;

    /** Min day of month */
    public static final int MIN_DAY_OF_MONTH = 1;

    /** Max day of week */
    public static final int MAX_DAY_OF_WEEK = 7;

    /** Min day of week */
    public static final int MIN_DAY_OF_WEEK = 1;

    /** Max hour */
    public static final int MAX_HOUR = 23;

    /** Min hour */
    public static final int MIN_HOUR = 0;

    /** Max minute */
    public static final int MAX_MINUTE = 59;

    /** Min minute */
    public static final int MIN_MINUTE = 0;

    /** Max second */
    public static final int MAX_SECOND = 59;

    /** Min second */
    public static final int MIN_SECOND = 0;

    /** ms per second */
    public static final long MS_PER_SECOND = 1000;

    /** ms per minute */
    public static final long MS_PER_MINUTE = 60000;

    /** ms per hour */
    public static final long MS_PER_HOUR = 3600000;

    /** ms per day */
    public static final long MS_PER_DAY = 86400000;

    /** s per day */
    public static final long S_PER_DAY = 86400;

    // Parameter indices (indicates parameter position in String)
    /** Position of SCHEDULE_TYPE field in schedule entry */
    public static final int SCHEDULE_TYPE = 0;

    /** Position of SECONDS field in schedule entry */
    public static final int SECONDS = 1;

    /** Position of MINUTES field in schedule entry */
    public static final int MINUTES = 2;

    /** Position of HOURS field in schedule entry */
    public static final int HOURS = 3;

    /** Position of DAYS field in schedule entry */
    public static final int DAYS = 4;

    /** Position of MONTHS field in schedule entry */
    public static final int MONTHS = 5;

    /** Position of DAYS_OF_WEEK field in schedule entry */
    public static final int DAYS_OF_WEEK = 6;

    /** Position of DAYS_OF_MONTH field in schedule entry */
    public static final int DAYS_OF_MONTH = 7;

    /** Position of DAYS_OF_YEAR field in schedule entry */
    public static final int DAYS_OF_YEAR = 8;

    /** Position of TIME_ZONE field in schedule entry */
    public static final int TIME_ZONE = 9;

    /** Position of CYCLES field in schedule entry */
    public static final int CYCLES = 10;

    /** Position of JOB field in schedule entry */
    public static final int JOB = 11;

    /** Position of DISPLAY_NAME field in schedule entry */
    public static final int DISPLAY_NAME = 12;

    /** Number of fields */
    public static final int MAX_FIELD = 13;

    /** Time fields only */
    public static final int SCHEDULE_TIME = MAX_FIELD + 1;

    // String Constants

    /** Names of days of the week */
    public static final String _dayNames[] = { "sun", "mon", "tue", "wed",
					       "thu", "fri", "sat" };

    /** Names of months */
    public static final String _monthNames[] = { "jan", "feb", "mar", "apr",
						 "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec" };

    /** Default value of JOB field */
    public static final String DEFAULT_JOB="n/a";

    /** Default value of DISPLAY_NAME field */
    public static final String DEFAULT_DISPLAY_NAME="n/a";

    // Class Data Members

    /** String representing the line in the Schedule file */
    String _line = null;

    // Schedule fields and their associated boolean arrays

    /** Schedule Type (Relative/Absolute) */
    String _scheduleType = null;

    /** Interpret Schedule as Relative (strictly periodic) */
    boolean _bRelative;

    /** Interpret Schedule as Absolute (like Cron) */
    boolean _bAbsolute;

    /** months in which to perform scheduled task */
    String _months = null;

    boolean _bMonths[] = new boolean[MAX_MONTH + 1];

    /** days of month on which to perform scheduled task */
    String _daysOfMonth = null;

    boolean _bDaysOfMonth[] = new boolean[MAX_DAY_OF_MONTH];

    /** days of year on which to perform scheduled task */
    String _daysOfYear = null;

    boolean _bDaysOfYear[] = new boolean[MAX_DAY_OF_YEAR + 1];

    /** days of week on which to perform scheduled task */
    String _daysOfWeek = null;

    boolean _bDaysOfWeek[] = new boolean[MAX_DAY_OF_WEEK];

    /** days (period) at which to perform scheduled task */
    String _days = null;

    long _lDays = 0L;

    /** hours at which to perform scheduled task */
    String _hours = null;

    long _lHours = 0L;

    boolean _bHours[] = new boolean[MAX_HOUR + 1];

    /** minutes at which to perform scheduled task */
    String _minutes = null;

    long _lMinutes = 0L;

    boolean _bMinutes[] = new boolean[MAX_MINUTE + 1];

    /** seconds which to perform scheduled task */
    String _seconds = null;

    long _lSeconds = 0L;

    boolean _bSeconds[] = new boolean[MAX_SECOND + 1];

    /* stop performing task after n cycles */
    String _cycles = null;

    long _longCycles = (-1L);

    /** indicate time zone context. */
    String _timeZone = null;

    TimeZone _tzTimeZone = null;

    /** Period (for relative entries; execute every _period ms) */
    long _period = 0;

    /** Job to perform on given schedule (file,URI,URL?) */
    String _job = null;

    /** Display name of job (shown by showSchedule) */
    String _displayName = null;

    /** Class describing a range (#-#|#) */
    protected class Range {
	long _start;

	long _end;

	public Range() {
	    super();
	}

	public Range(long start, long end) {
	    super();
	    _start = start;
	    _end = end;
	}

	public long getStart() {
	    return _start;
	}

	public void setStart(long start) {
	    _start = start;
	}

	public long getEnd() {
	    return _end;
	}

	public void setEnd(long end) {
	    _end = end;
	}
    }

    /**
     * Token information parsed into FieldSpec (interpreted by adjustRelative()
     * and adjustAbsolute())
     */
    protected class FieldSpec {
	Vector _ranges = new Vector(10);

	long _modulus;

	public void addRange(Range range) {
	    _ranges.add(range);
	}

	public void setModulus(long modulus) {
	    _modulus = modulus;
	}

	public long getModulus() {
	    return _modulus;
	}

	public Vector getRanges() {
	    return _ranges;
	}

	public void clear() {
	    _ranges.removeAllElements();
	    _ranges.setSize(10);
	    _modulus = 0L;
	}
    }

    //Constructors

    /** Default vanilla constructor */
    public ScheduleSpecifier() {
	super();
    }

    /** Construct Specifier from schedule entry string */
    public ScheduleSpecifier(String spec) throws ScheduleParseException {
	super();

	// Check to see whether schedule has been specified as an integer
	// period.
	try {
	    int period = Integer.parseInt(spec);
	    _logger.debug("ctr: argument " + spec + " is integer period");
	    initInterval(period);
	    _logger.debug("ctr: initialized the interval");
	}
	catch (NumberFormatException e) {
	    // Not a simple integer period
	    if (spec.length() > 0) {
		parse(this, spec);
	    }	    
	}

	setLine(toString());
    }

    /**
     * Construct simple (relative schedule) Specifier from period Only exection
     * period is computed. No masks are set.
     */
    public ScheduleSpecifier(long periodMsec) throws ScheduleParseException {
	super();
	initInterval(periodMsec);
    }

    // Methods
    /**
     * Set up a (relative) schedule specifier given a sample interval in
     * milliseconds.
     */

    protected void initInterval(long intervalMillis)
	throws ScheduleParseException {
	if (intervalMillis < MS_PER_SECOND)
	    throw new ScheduleParseException("Period too short; must be at least " + MS_PER_SECOND);

	long days = intervalMillis / MS_PER_DAY;
	intervalMillis -= days * MS_PER_DAY;
	long hours = intervalMillis / MS_PER_HOUR;
	intervalMillis -= hours * MS_PER_HOUR;
	long minutes = intervalMillis / MS_PER_MINUTE;
	intervalMillis -= minutes * MS_PER_MINUTE;
	long seconds = intervalMillis / MS_PER_SECOND;
	intervalMillis -= seconds * MS_PER_SECOND;

	setScheduleType("R");
	setSeconds(("*/" + seconds));
	setMinutes("*/" + minutes);
	setHours("*/" + hours);
	setDays("*/" + days);
	setMonths("*");
	setDaysOfWeek("*");
	setDaysOfMonth("*");
	setDaysOfYear("*");
	setCycles("*");
	setTimeZone("GMT");
	setLine(toString());
	return;
    }

    /** Get name of field */
    public String getFieldName(int position) {
	switch (position) {
	case SCHEDULE_TYPE:
	    return "Schedule Type";
	case SECONDS:
	    return "Seconds";
	case MINUTES:
	    return "Minutes";
	case HOURS:
	    return "Hours";
	case DAYS:
	    return "Days";
	case MONTHS:
	    return "Month";
	case DAYS_OF_WEEK:
	    return "Days of Week";
	case DAYS_OF_MONTH:
	    return "Days of Month";
	case DAYS_OF_YEAR:
	    return "Days of Year";
	case CYCLES:
	    return "Cycles";
	case TIME_ZONE:
	    return "Time Zone";
	case JOB:
	    return "Job Name";
	default:
	    return "Undefined Field";
	}
    }

    /** Get _scheduleType field */
    public String getScheduleType() {
	return _scheduleType;
    }

    /** Set _scheduleType field */
    public void setScheduleType(String type) throws ScheduleParseException {
	type.trim();
	type = type.substring(0, 1).toUpperCase();
	if (!type.equals("R") && !type.equals("A"))
	    throw new ScheduleParseException("Invalid schedule type (" + type
					     + ")");
	_scheduleType = type;
	setBScheduleType(type);
	_logger.debug("ScheduleType = " + getScheduleType());
    }

    /** Set _BScheduleType field */
    public void setBScheduleType(String type) {
	// we're doing this in case we want to
	// capture behaviors/types in addition to
	// relative or absolute.
	if (type.substring(0, 1).toUpperCase().equals("R")) {
	    _bRelative = true;
	    _bAbsolute = false;
	}
	if (type.substring(0, 1).toUpperCase().equals("A")) {
	    _bRelative = false;
	    _bAbsolute = true;
	}
    }

    /** Get _bRelative field */
    public boolean isRelative() {
	return _bRelative;
    }

    /** Set _bRelative field */
    public void setBRelative(boolean bRelative) {
	_bRelative = bRelative;
    }

    /** Get _bAbsolute field */
    public boolean isAbsolute() {
	return _bAbsolute;
    }

    /** Set _bAbsolute field */
    public void setBAbsolute(boolean bAbsolute) {
	_bAbsolute = bAbsolute;
    }

    /** Get _bMonths field */
    public boolean[] getBMonths() {
	return _bMonths;
    }

    /** Set _bMonths field */
    public void setBMonths(boolean[] months) {
	_bMonths = months;
	return;
    }

    /** Get _bDaysOfMonth field */
    public boolean[] getBDaysOfMonth() {
	return _bDaysOfMonth;
    }

    /** Set _bDaysOfMonth field */
    public void setBDaysOfMonth(boolean[] daysOfMonth) {
	_bDaysOfMonth = daysOfMonth;
	return;
    }

    /** Get _bDaysOfYear field */
    public boolean[] getBDaysOfYear() {
	return _bDaysOfYear;
    }

    /** Set _bDaysOfYear field */
    public void setBDaysOfYear(boolean[] daysOfYear) {
	_bDaysOfYear = daysOfYear;
	return;
    }

    /** Get _bDaysOfWeek field */
    public boolean[] getBDaysOfWeek() {
	return _bDaysOfWeek;
    }

    /** Set _bDaysOfWeek field */
    public void setBDaysOfWeek(boolean[] daysOfWeek) {
	_bDaysOfWeek = daysOfWeek;
	return;
    }

    /** Get _bHours field */
    public boolean[] getBHours() {
	return _bHours;
    }

    /** Set _bHours field */
    public void setBHours(boolean[] hours) {
	_bHours = hours;
	return;
    }

    /** Get _bMinutes field */
    public boolean[] getBMinutes() {
	return _bMinutes;
    }

    /** Set _bMinutes field */
    public void setBMinutes(boolean[] minutes) {
	_bMinutes = minutes;
	return;
    }

    /** Get _bSeconds field */
    public boolean[] getBSeconds() {
	return _bSeconds;
    }

    /** Set _bSeconds field */
    public void setBSeconds(boolean[] seconds) {
	_bSeconds = seconds;
	return;
    }

    /** Get _longCycles field */
    public long getLongCycles() {
	return _longCycles;
    }

    /** Set _longCycles field */
    public void setLongCycles(long cycles) {
	_longCycles = cycles;
	return;
    }

    /** Get _timeZone field */
    public String getTimeZone() {
	return _timeZone;
    }

    /** Set _timeZone field */
    public void setTimeZone(String timeZone) throws ScheduleParseException {
	validateTimezone(timeZone);
	_tzTimeZone = TimeZone.getTimeZone(timeZone);
	_timeZone = timeZone;
	return;
    }

    /** Get _tzTimezone */
    public TimeZone getTZ() {
	return _tzTimeZone;
    }

    /** Get _Months field */
    public String getMonths() {
	return _months;
    }

    /** Set _Months field */
    public void setMonths(String months) throws ScheduleParseException {
	months.trim();
	FieldSpec fieldSpec = parseToken(months, (long) (_bMonths.length - 1));
	setBMonths(processToken(fieldSpec, _bMonths));
	_months = months;
	return;
    }

    /** Get _days field */
    public String getDays() {
	return _days;
    }

    /** Set _days field */
    public void setDays(String days) throws ScheduleParseException {
	days.trim();
	FieldSpec fieldSpec = parseToken(days, 0L);
	_lDays = fieldSpec.getModulus();
	_days = days;
	computePeriod();
	return;
    }

    /** Get _daysOfYear field */
    public String getDaysOfYear() {
	return _daysOfYear;
    }

    /** Set _daysOfYear field */
    public void setDaysOfYear(String daysOfYear) throws ScheduleParseException {
	daysOfYear.trim();
	FieldSpec fieldSpec = parseToken(daysOfYear,
					 (long) (_bDaysOfYear.length - 1));
	setBDaysOfYear(processToken(fieldSpec, _bDaysOfYear));
	_daysOfYear = daysOfYear;
	return;
    }

    /** Get _daysOfMonth field */
    public String getDaysOfMonth() {
	return _daysOfMonth;
    }

    /** Set _daysOfMonth field */
    public void setDaysOfMonth(String daysOfMonth)
	throws ScheduleParseException {
	daysOfMonth.trim();
	FieldSpec fieldSpec = parseToken(daysOfMonth,
					 (long) (_bDaysOfMonth.length - 1));
	setBDaysOfMonth(processToken(fieldSpec, _bDaysOfMonth));
	_daysOfMonth = daysOfMonth;
	return;
    }

    /** Get _daysOfWeek field */
    public String getDaysOfWeek() {
	return _daysOfWeek;
    }

    /** Set _daysOfWeek field */
    public void setDaysOfWeek(String daysOfWeek) throws ScheduleParseException {
	daysOfWeek.trim();
	FieldSpec fieldSpec = parseToken(daysOfWeek,
					 (long) (_bDaysOfWeek.length - 1));
	setBDaysOfWeek(processToken(fieldSpec, _bDaysOfWeek));
	_daysOfWeek = daysOfWeek;
	return;
    }

    /** Get _hours field */
    public String getHours() {
	return _hours;
    }

    /** Set _hours field */
    public void setHours(String hours) throws ScheduleParseException {
	hours.trim();
	FieldSpec fieldSpec = parseToken(hours, (long) (_bHours.length - 1));
	setBHours(processToken(fieldSpec, _bHours));
	_lHours = fieldSpec.getModulus();
	_hours = hours;
	computePeriod();
	return;
    }

    /** Get _minutes field */
    public String getMinutes() {
	return _minutes;
    }

    /** Set _minutes field */
    public void setMinutes(String minutes) throws ScheduleParseException {
	minutes.trim();
	FieldSpec fieldSpec = parseToken(minutes, (long) (_bMinutes.length - 1));
	setBMinutes(processToken(fieldSpec, _bMinutes));
	_lMinutes = fieldSpec.getModulus();
	_minutes = minutes;
	return;
    }

    /** Get _seconds field */
    public String getSeconds() {
	return _seconds;
    }

    /** Set _seconds field */
    public void setSeconds(String seconds) throws ScheduleParseException {
	seconds.trim();
	FieldSpec fieldSpec = parseToken(seconds, (long) (_bSeconds.length - 1));
	setBSeconds(processToken(fieldSpec, _bSeconds));
	_lSeconds = fieldSpec.getModulus();
	_seconds = seconds;
	computePeriod();
	return;
    }

    /** Get _cycles field */
    public String getCycles() {
	return _cycles;
    }

    /** Set _cycles field */
    public void setCycles(String cycles) throws ScheduleParseException {
	try {
	    if (cycles.trim().equals("*"))
		setLongCycles(-1L);
	    else
		setLongCycles(Long.parseLong(cycles.trim()));
	    _cycles = cycles;
	} catch (NumberFormatException e) {
	    throw new ScheduleParseException("Invalid Cycles specifier: "
					     + cycles + " \n" + e.toString());
	}
	return;
    }

    /** Get _period field */
    public long getPeriod() {
	return _period;
    }

    /** Get _job field */
    public String getJob() {
	return _job;
    }

    /** Set _job field */
    public void setJob(String job) {
	_job = job;
	return;
    }

    /** Get _displayName field */
    public String getDisplayName() {
	return _displayName;
    }

    /** Set _displayName field */
    public void setDisplayName(String displayName) {
	if(displayName==null){
	    _displayName=null;
	}else{
	    _displayName = displayName.trim();
	    // set JOB field to default to make sure that displayName
	    // is exported in toString(), even if JOB is not explicitly set
	    if(_job==null)
		_job=DEFAULT_JOB;
	}
	return;
    }

    /** Get _line field */
    public String getLine() {
	return _line;
    }

    /** Set _Line field */
    public void setLine(String line) {
	_line = line;
	return;
    }

    /** Get schedule time part of line (no job) */
    public String getScheduleTime() {
	String spec = "";

	if (_scheduleType != null)
	    spec += _scheduleType;

	if (_seconds != null)
	    spec += " " + _seconds;

	if (_minutes != null)
	    spec += " " + _minutes;

	if (_hours != null)
	    spec += " " + _hours;

	if (_days != null)
	    spec += " " + _days;

	if (_months != null)
	    spec += " " + _months;

	if (_daysOfWeek != null)
	    spec += " " + _daysOfWeek;

	if (_daysOfMonth != null)
	    spec += " " + _daysOfMonth;

	if (_daysOfYear != null)
	    spec += " " + _daysOfYear;

	if (_timeZone != null)
	    spec += " " + _timeZone;

	if (_cycles != null)
	    spec += " " + _cycles;

	return spec;
    }

    /**
     * parse a line from the schedule file.
     */
    public static void parse(ScheduleSpecifier thisOne, String line)
	throws ScheduleParseException {

	// Throw away leading/trailing whitespace
	line.trim();

	// Throw away comments
	StringTokenizer lt = new StringTokenizer(line, "#");
	if (lt.hasMoreTokens())
	    line = lt.nextToken();

	_logger.debug("parsing line " + line);

	// first see if the string is just a string representation of
	// an interval
	try {
	    long period = Long.parseLong(line);
	    thisOne.initInterval(period);
	    return;
	} catch (NumberFormatException e) {
	    // move on, it wasn't a simple interval
	}

	// Start peeling off and processing fields (up to job and displayName)
	lt = new StringTokenizer(line, " \t\r\f\n");
	int field = 0;

	while (lt.hasMoreTokens() && field<=CYCLES) {
	    String fieldToken = lt.nextToken();
	    fieldToken.trim();

	    _logger.debug("parsing field " + fieldToken);
	    thisOne.set(field++, fieldToken);

	}
	
	// Parse the JOB and DISPLAY_NAME fields, if specified 
	try {
	    // look for start of JOB field
	    int jStart=(line.indexOf("\"")+1);
	    if(jStart>=1){

		// look for end of JOB field
		int jEnd=line.indexOf("\"",jStart);

		if(jEnd>=1){
		    String jn = line.substring(jStart,jEnd);

		    // set JOB field
		    thisOne.set(JOB, jn);

		    // look for start of DISPLAY_NAME field
		    int dnStart = line.indexOf("\"",(jEnd+2))+1;
		    if(dnStart>=1){

			// lood for end of DISPLAY_NAME field
			int dnEnd=line.indexOf("\"",dnStart);
			if(dnEnd>=1){
			    String dn = line.substring(dnStart,dnEnd);

			    // set DISPLAY_NAME field
			    thisOne.set(DISPLAY_NAME, dn);
			}else{
			    throw new ScheduleParseException("mismatched quotes parsing displayName");
			}
		    }else{ 
			//else no displayName specified (use default)
			thisOne.set(DISPLAY_NAME, null);
		    }
		}else{
		    throw new ScheduleParseException("mismatched quotes parsing job name");
		}
	    }else{ 
		//else no job or displayName specified (use default)
		thisOne.set(JOB, null);
		thisOne.set(DISPLAY_NAME, null);
	    }
	} catch (StringIndexOutOfBoundsException e) {
	    e.printStackTrace();
	    throw new ScheduleParseException(e.toString());
	} catch (NullPointerException n) {
	    n.printStackTrace();
	    throw new ScheduleParseException(n.toString());
	}
	
	// retain the cleaned up version of the line
	thisOne.setLine(thisOne.toString());

	//_logger.debug("Entry Complete; _period = "+_period);
	return;
    }

    /** Get a field */
    public String get(int field) {
	switch (field) {
	case SCHEDULE_TYPE:
	    return getScheduleType();
	case DAYS_OF_YEAR:
	    return getDaysOfYear();
	case DAYS_OF_MONTH:
	    return getDaysOfMonth();
	case DAYS_OF_WEEK:
	    return getDaysOfWeek();
	case MONTHS:
	    return getMonths();
	case DAYS:
	    return getDays();
	case HOURS:
	    return getHours();
	case MINUTES:
	    return getMinutes();
	case SECONDS:
	    return getSeconds();
	case CYCLES:
	    return getCycles();
	case TIME_ZONE:
	    return getTimeZone();
	case JOB:
	    return getJob();
	case DISPLAY_NAME:
	    return getDisplayName();
	case SCHEDULE_TIME:
	    return getScheduleTime();
	default:
	    return null;
	}
    }

    /** Set field (re-parse spec and set field accordingly) */
    public void set(int field, String spec) throws ScheduleParseException {
	switch (field) {
	case SCHEDULE_TYPE:
	    setScheduleType(spec);
	    break;
	case DAYS_OF_YEAR:
	    setDaysOfYear(spec);
	    break;
	case DAYS_OF_MONTH:
	    setDaysOfMonth(spec);
	    break;
	case DAYS_OF_WEEK:
	    setDaysOfWeek(spec);
	    break;
	case MONTHS:
	    setMonths(spec);
	    break;
	case DAYS:
	    setDays(spec);
	    break;
	case HOURS:
	    setHours(spec);
	    break;
	case MINUTES:
	    setMinutes(spec);
	    break;
	case SECONDS:
	    setSeconds(spec);
	    break;
	case CYCLES:
	    setCycles(spec);
	    break;
	case TIME_ZONE:
	    setTimeZone(spec);
	    break;
	case JOB:
	    setJob(spec);
	    break;
	case DISPLAY_NAME:
	    setDisplayName(spec);
	default:
	    // break when max fields reached
	    break;
	}
	return;
    }

    /**
     * Parse a single token into a FieldSpec representation. (Token is the token
     * to be parsed (1-3,*,10/2 etc.)) Interpretation is left to other methods,
     * depending on whether the schedule entry is absolute or relative.
     */
    protected FieldSpec parseToken(String token, long maxRange)
	throws ScheduleParseException {

	long modulus = 0L;
	long firstElement = 0L;
	long lastElement = 0L;
	FieldSpec fieldSpec = new FieldSpec();
	int i = 0;

	//_logger.debug("parsing token "+token);

	// remove whitespace
	token.trim();

	// get token tokenizer
	StringTokenizer tt = new StringTokenizer(token, ",");
	String fieldToken = null;
	while (tt.hasMoreTokens()) {
	    // Get field in token ( <*|#-#|#>[/#])
	    fieldToken = tt.nextToken();
	    //_logger.debug("parsing token field "+fieldToken);

	    // get modulus, if any
	    // More than one won't break it, but only the first one is used.

	    StringTokenizer mt = new StringTokenizer(fieldToken, "/");
	    if (mt.countTokens() > 1) {
		modulus = longIndex(fieldToken.substring(fieldToken
							 .indexOf("/") + 1));

		//_logger.debug("parsing token modulus "+modulus);

		// shave off the modulus, were done with it
		fieldToken = mt.nextToken();
		//_logger.debug("trimming modulus from token "+fieldToken);
	    }

	    // Check for range and parse
	    StringTokenizer rt = new StringTokenizer(fieldToken, "-");
	    if (rt.countTokens() > 1) {
		//_logger.debug("parsing range "+rt.countTokens());
		String tempString = rt.nextToken();
		firstElement = getLongIndex(tempString);
		lastElement = getLongIndex(fieldToken.substring(fieldToken
								.indexOf("-") + 1));
	    } else {
		// Non-range, either *,# or name
		//_logger.debug("parsing non-range token "+fieldToken);

		boolean parseDone = false;
		long test = (-1L);

		// check for 'all' (*) (should not have any other characters)
		if (fieldToken.equals("*")) {
		    //_logger.debug("parsing * ");
		    firstElement = 0L;
		    lastElement = (maxRange);
		    parseDone = true;
		}

		// check for numeric or day,month name
		if (parseDone == false) {
		    //_logger.debug("parsing name or number ");
		    test = getLongIndex(fieldToken);
		    if (test >= 0) {
			firstElement = lastElement = test;
			parseDone = true;
		    }
		}

		// It's no good; throw up our hands and quit.
		if (parseDone == false) {
		    throw new ScheduleParseException(
						     "parse(): invalid Schedule Entry (" + fieldToken
						     + ")");
		}
	    }

	    //_logger.debug("first,last elements:
	    // "+firstElement+","+lastElement);

	    //_logger.debug("Range checking...");

	    // Make sure the numbers make sense...
	    if (firstElement < 0 || firstElement > maxRange)
		throw new ScheduleParseException(
						 "RangeException in parse(): invalid firstElement");
	    if (lastElement < 0 || lastElement > maxRange)
		throw new ScheduleParseException(
						 "RangeException in parse(): invalid lastElement "
						 + lastElement + " " + maxRange);

	    // Note: ranges that have last<first are valid; wrap around
	    // is handled while filling the arrays.

	    if (modulus < 0)
		throw new ScheduleParseException(
						 "RangeException in parse(): invalid modulus");

	    fieldSpec.setModulus(modulus);
	    fieldSpec.addRange(new Range(firstElement, lastElement));
	    //_logger.debug(" ");

	}// end while
	//_logger.debug(" ");
	return fieldSpec;
    }

    /** Interpret token; fill out operation masks */
    public boolean[] processToken(FieldSpec fieldSpec, boolean[] barray) {

	//_logger.debug("Processing token...");

	if (barray == null)
	    return null;

	Vector ranges = fieldSpec.getRanges();
	long modulus = fieldSpec.getModulus();
	long fillModulus = 1L;

	if ((this.isAbsolute() == true) && (modulus > 1L))
	    fillModulus = modulus;

	for (Enumeration e = ranges.elements(); e.hasMoreElements();) {
	    Range r = (Range) e.nextElement();
	    int firstElement = (int) r.getStart();
	    int lastElement = (int) r.getEnd();

	    // Now we have all the numbers; fill in the binary
	    // accordingly.
	    //_logger.debug("(filling array) "+firstElement+" "+lastElement+"
	    // "+modulus);
	    int modCount = 0;
	    int pels = 1;
	    int i;

	    for (i = firstElement; i != lastElement; i++) {
		if ((modCount++ % fillModulus == 0)) {
		    barray[i] = true;
		    pels++;
		    if (pels % 15 == 0)
			;
		} else
		    barray[i] = false;
		// wrap around if necessary
		if (i == (barray.length - 1))
		    i = (-1);
	    }

	    // clean up lastElement
	    if ((i == lastElement) && (modCount++ % fillModulus == 0)) {
		barray[i] = true;
		//System.out.print(" ["+i+"]");
		pels++;
		if (pels % 15 == 0)
		    ;
	    }
	    //_logger.debug(" ");
	}
	return barray;
    }

    /** Checks _timezone field to see if it's valid */
    public void validateTimezone(String timezone) throws ScheduleParseException {
	StringTokenizer tzt = new StringTokenizer(timezone, ":");
	try {
	    String tokenString = tzt.nextToken();
	    if (!tokenString.toUpperCase().startsWith("GMT"))
		throw new ScheduleParseException(
						 "Invalid TimeZone Syntax (GMT[+|-hh[:mm]])");
	    if (tokenString.length() > 3) {
		int hhStart = tokenString.toUpperCase().indexOf("GMT") + 4;

		int hh = Integer.parseInt(tokenString.substring(hhStart));
		if (hh < (-24) || hh > 24
		    || tokenString.substring(hhStart).length() > 2)
		    throw new ScheduleParseException(
						     "Invalid TimeZone (Invalid hours)");

		int mm = 0;
		tokenString = "";
		if (tzt.hasMoreTokens()) {
		    tokenString = tzt.nextToken();
		    mm = Integer.parseInt(tokenString);
		}

		if (mm < 0 || mm > 59 || tokenString.length() > 2)
		    throw new ScheduleParseException(
						     "Invalid TimeZone (Invalid minutes)");
	    }
	} catch (NumberFormatException e) {
	    throw new ScheduleParseException(
					     "Invalid TimeZone Invalid Number Format");
	} catch (NoSuchElementException e2) {
	    throw new ScheduleParseException(
					     "Invalid TimeZone Syntax (GMT[+|-hh[:mm]])");
	} catch (IndexOutOfBoundsException e3) {
	    throw new ScheduleParseException(
					     "Invalid TimeZone Invalid Index (GMT[+|-hh[:mm]])");
	}
	return;
    }

    /**
     * getLongIndex() checks token for numeric or name and returns index or -1
     * if no match is found.
     */
    protected long getLongIndex(String token) {

	long test = longIndex(token);
	if (test >= 0)
	    return test;

	test = (long) dayIndex(token);
	if (test >= 0)
	    return test;

	test = (long) monthIndex(token);
	if (test >= 0)
	    return test;

	return -1L;
    }

    /**
     * numberIndex() checks token to see if it's a valid numeric index (>=0)
     * Returns number if valid, -1 if invalid.
     */
    protected int numberIndex(String token) {
	try {
	    int number = Integer.parseInt(token);
	    if (number < 0)
		return -1;
	    return number;
	} catch (NumberFormatException e) {
	    return -1;
	}
    }

    /**
     * longIndex() checks token to see if it's a valid numeric index (>=0)
     * Returns number if valid, -1 if invalid.
     */
    protected long longIndex(String token) {
	try {
	    long number = Long.parseLong(token);
	    if (number < 0L)
		return -1L;
	    return number;
	} catch (NumberFormatException e) {
	    return -1L;
	}
    }

    /**
     * dayIndex() compares token to day names and returns the (numeric) day or
     * -1 if not a day name.
     */
    protected int dayIndex(String name) {
	for (int i = 0; i < MAX_DAY_OF_WEEK; i++) {
	    if (name.toLowerCase().equals(_dayNames[i]))
		return i;
	}
	return -1;
    }

    /**
     * monthIndex() compares token to month names and returns the (numeric)
     * month or -1 if not a month name.
     */
    protected int monthIndex(String name) {
	for (int i = 0; i < MAX_MONTH; i++) {
	    if (name.toLowerCase().equals(_monthNames[i]))
		return i;
	}
	return -1;
    }

    /** toString() convert schedule entry to string 
	Note: The output order is important since the
	parse() method must be able use this exported form
	of the ScheduleSpecifier.
     */
    public String toString() {
	String spec = "";

	if (_scheduleType != null)
	    spec += " " + _scheduleType;

	if (_seconds != null)
	    spec += " " + _seconds;

	if (_minutes != null)
	    spec += " " + _minutes;

	if (_hours != null)
	    spec += " " + _hours;

	if (_days != null)
	    spec += " " + _days;

	if (_months != null)
	    spec += " " + _months;

	if (_daysOfWeek != null)
	    spec += " " + _daysOfWeek;

	if (_daysOfMonth != null)
	    spec += " " + _daysOfMonth;

	if (_daysOfYear != null)
	    spec += " " + _daysOfYear;

	if (_timeZone != null)
	    spec += " " + _timeZone;

	if (_cycles != null)
	    spec += " " + _cycles;

	if (_job != null){
	    spec += " \"" + _job + "\"";

	    // only export displayName if job is defined
	    //
	    // Note: By putting this logic here (inside if(_job!=null)),
	    // it makes it possible to have exported 
	    // ScheduleSpecifier strings that don't include
	    // the optional job and displayName. The alternative
	    // is to force these fields to be explicitly set in this export.

	    if ((_displayName != null))
		spec += " \"" + _displayName + "\"";
	}

	return spec.trim();

    }

    /** Compare two ScheduleEntries */
    public boolean equals(ScheduleSpecifier schedule) {
	// Compare the string version of all fields
	// If the field tokens match, the schedules should match
	if (!get(ScheduleSpecifier.SCHEDULE_TYPE).equals(
							 schedule.get(ScheduleSpecifier.SCHEDULE_TYPE)))
	    return false;
	if (!get(ScheduleSpecifier.SECONDS).equals(
						   schedule.get(ScheduleSpecifier.SECONDS)))
	    return false;
	if (!get(ScheduleSpecifier.MINUTES).equals(
						   schedule.get(ScheduleSpecifier.MINUTES)))
	    return false;
	if (!get(ScheduleSpecifier.HOURS).equals(
						 schedule.get(ScheduleSpecifier.HOURS)))
	    return false;
	if (!get(ScheduleSpecifier.DAYS).equals(
						schedule.get(ScheduleSpecifier.DAYS)))
	    return false;
	if (!get(ScheduleSpecifier.MONTHS).equals(
						  schedule.get(ScheduleSpecifier.MONTHS)))
	    return false;
	if (!get(ScheduleSpecifier.DAYS_OF_WEEK).equals(
							schedule.get(ScheduleSpecifier.DAYS_OF_WEEK)))
	    return false;
	if (!get(ScheduleSpecifier.DAYS_OF_MONTH).equals(
							 schedule.get(ScheduleSpecifier.DAYS_OF_MONTH)))
	    return false;
	if (!get(ScheduleSpecifier.DAYS_OF_YEAR).equals(
							schedule.get(ScheduleSpecifier.DAYS_OF_YEAR)))
	    return false;
	if (!get(ScheduleSpecifier.TIME_ZONE).equals(
						     schedule.get(ScheduleSpecifier.TIME_ZONE)))
	    return false;
	if (!get(ScheduleSpecifier.CYCLES).equals(
						  schedule.get(ScheduleSpecifier.CYCLES)))
	    return false;
	if (!get(ScheduleSpecifier.JOB).equals(
					       schedule.get(ScheduleSpecifier.JOB)))
	    return false;

	return true;
    }

    /** Compare calendar to mask/schedule boolean arrays */
    public boolean isSelectedTime(Calendar calendar) {
	try {
	    if (_bDaysOfYear[calendar.get(Calendar.DAY_OF_YEAR) - 1] == false)
		return false;
	    if (_bDaysOfMonth[calendar.get(Calendar.DAY_OF_MONTH) - 1] == false)
		return false;
	    if (_bDaysOfWeek[calendar.get(Calendar.DAY_OF_WEEK) - 1] == false)
		return false;
	    if (_bMonths[calendar.get(Calendar.MONTH)] == false)
		return false;
	    if (_bHours[calendar.get(Calendar.HOUR_OF_DAY)] == false)
		return false;
	    if (_bMinutes[calendar.get(Calendar.MINUTE)] == false)
		return false;
	    if (_bSeconds[calendar.get(Calendar.SECOND)] == false)
		return false;
	} catch (ArrayIndexOutOfBoundsException e) {
	    System.err.println("isSelectedTime(): Invalid calendar " + e
			       + " doy " + calendar.get(Calendar.DAY_OF_YEAR) + " dom "
			       + calendar.get(Calendar.DAY_OF_MONTH) + " dow "
			       + calendar.get(Calendar.DAY_OF_WEEK) + " mon "
			       + calendar.get(Calendar.MONTH) + " hr "
			       + calendar.get(Calendar.HOUR_OF_DAY) + " min "
			       + calendar.get(Calendar.MINUTE) + " sec "
			       + calendar.get(Calendar.SECOND));
	    return false;

	}
	return true;
    }

    /** Compute period of execution (for relative schedules) */
    public void computePeriod() {
	_period = 0L;
	_period += _lSeconds * MS_PER_SECOND;
	_period += _lMinutes * MS_PER_MINUTE;
	_period += _lHours * MS_PER_HOUR;
	_period += _lDays * MS_PER_DAY;
    }

    /** Parse from  string; fulfills Importable interface */
    public Object fromString(String mnemonic) throws InvalidPropertyException {

	// If mnemonic string represents just an integer, it is
	// to be interpreted as an interval in SECONDS, and so must
	// be converted to milliseconds.
	try {
	    long period = Long.parseLong(mnemonic);
	    mnemonic = new String("" + (period * 1000));
	} catch (NumberFormatException e) {
	    // Nope, not just a simple integer; keep going...
	}

	Object object;
	try {
	    object = new ScheduleSpecifier(mnemonic);
	    return object;
	} catch (ScheduleParseException e) {
	    throw new InvalidPropertyException(e.getMessage());
	}
    }

    /** Return array of valid string values. */
    public String[] validValues() {
	String[] values = new String[] {"[interval (sec)]", 
					"[cron-style string]"};
	return values;
    }

    /** Eine Kleine test code */
    public static void main(String[] args) {
	/*
	 * Set up a simple configuration that logs on the console. Note that
	 * simply using PropertyConfigurator doesn't work unless JavaBeans
	 * classes are available on target. For now, we configure a
	 * PropertyConfigurator, using properties passed in from the command
	 * line, followed by BasicConfigurator which sets default console
	 * appender, etc.
	 */
	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");

	BasicConfigurator.configure(new ConsoleAppender(layout));

	if (args.length > 0)
	    if (args[0].trim().toLowerCase().startsWith("--h")) {
		System.err
		    .println("\nUsage: java ScheduleSpecifier [period]\n");
		System.exit(0);
	    }

	try {

	    long period = 1000L;
	    if (args.length > 0)
		period = Long.parseLong(args[0]);
	    ScheduleSpecifier ssp = new ScheduleSpecifier(period);
	    _logger.debug("ssp:\n" + ssp);
	    ScheduleSpecifier sse = new ScheduleSpecifier();
	    _logger.debug("sse\n" + sse);
	    String quux = ssp.toString() + " foo.job";
	    ScheduleSpecifier ssg = new ScheduleSpecifier(quux);
	    _logger.debug(("ssg\n" + ssg));
	    ScheduleSpecifier ssps = new ScheduleSpecifier("180000");
	    _logger.debug(("ssps\n" + ssps));
	} catch (ScheduleParseException e) {
	    System.err.println(e);
	}

    }
	
}
