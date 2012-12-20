/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

import java.util.StringTokenizer;
import java.util.Vector;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.text.ParseException;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PacketParser;

/**
   Base class for PacketParsers which work on delimited string
   data. Note that PacketParser is intended to be transferred 
   across a network, and hence implements the java.io.Serializable
   interface. Thus any member objects of your subclass must either 
   implement Serializable, or be declared 'transient' or 'static'.
 */
public abstract class DelimitedStringParser 
    extends PacketParser {
		
		static private Logger _logger = 
		Logger.getLogger(DelimitedStringParser.class);
		
		private static final long serialVersionUID=1L;
		
		protected String _delimiters = null;
		protected Vector _fieldVector = new Vector();
		DecimalFormat _decimalFormat = new DecimalFormat();
		static private ParsePosition _parsePos = new ParsePosition(0);
		
		/** no arg constructor */
		public DelimitedStringParser(){
			super();
		}
		
		/** Create parser, specifying the delimiter string. */
		public DelimitedStringParser(String delimiters) {
			this();
			_delimiters = delimiters;
		}
		
		/** Create parser, specifying the delimiter string. */
		public DelimitedStringParser(String registryName, String delimiters) {
			super(registryName);
			_delimiters = delimiters;
		}
		
		/** Return fields parsed from DevicePacket. */
		public PacketParser.Field[] parseFields(DevicePacket packet) 
		throws NotSupportedException, ParseException {
			
			if (!(packet instanceof SensorDataPacket)) {
				throw new NotSupportedException("expecting SensorDataPacket");
			}
			
			SensorDataPacket sensorDataPacket = (SensorDataPacket )packet;
			String buffer = new String(sensorDataPacket.dataBuffer());
			
			StringTokenizer tokenizer = 
			new StringTokenizer(buffer.trim(),_delimiters);
			
			_fieldVector.clear();
			int nTokens = 0;
			
			if(_registryName!=null){
				PacketParser.Field field = new Field("name",_registryName, "String");
				_fieldVector.addElement(field);
			}
			while (tokenizer.hasMoreTokens()) {
				
				String token = tokenizer.nextToken().trim();
				
				PacketParser.Field field = processToken(nTokens, token);
				if (field != null)
					_fieldVector.addElement(field);
				
				nTokens++;
			}
			
			// Create array to hold vector elements
			int nFields = _fieldVector.size();
			
			 PacketParser.Field[] fields = new PacketParser.Field[nFields];
			 for (int i = 0; i < nFields; i++) {
			 fields[i] = (PacketParser.Field )_fieldVector.elementAt(i);
			 }
			 
			return fields;
		}
		
		
		/** Return Number represented by input String token; Number is null
		 if token does not represent a decimal number. */
		protected Number decimalValue(String token) {
			_parsePos.setIndex(0);
			return _decimalFormat.parse(token, _parsePos);
		}
		
		
		/** Process the token, whose position in string is nToken. If
		 token corresponds to a Field, create and return the field. 
		 Otherwise return null. */
		protected abstract PacketParser.Field processToken(int nToken, 
														   String token)
		throws ParseException;
		
	}
