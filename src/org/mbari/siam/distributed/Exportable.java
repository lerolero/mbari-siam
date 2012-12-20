// Copyright 2002 MBARI
package org.mbari.siam.distributed;

import java.io.DataOutput;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;

/**
 * SiamSerializable is an interface implemented by exportable objects that
 * generate java-independent serialized forms.
 * 
 * @author Kent Headley
 */

public interface Exportable {
	public static final short EX_BASE = 0x0000;

	public static final short EX_STATE = 0x0001;

	public static final short EX_STATEATTRIBUTE = 0x0002;

	public static final short EX_BOOLEANOBJATT = 0x0003;

	public static final short EX_INTEGEROBJATT = 0x0004;

	public static final short EX_LONGOBJATT = 0x0005;

	public static final short EX_MNEMONICINTEGEROBJATT = 0x0006;

	public static final short EX_SCHEDULESPECIFIEROBJATT = 0x0007;

	public static final short EX_BYTEARRAYOBJATT = 0x0008;

	public static final short EX_FLOATOBJATT = 0x0009;

	public static final short EX_DOUBLEOBJATT = 0x000a;

	public static final short EX_DEVICEPACKET = 0x0100;

	public static final short EX_METADATAPACKET = 0x0101;

	public static final short EX_SENSORDATAPACKET = 0x0102;

	public static final short EX_DEVICEMESSAGEPACKET = 0x0103;

	public static final short EX_MAX = 0x0103;

	/** Export state to the specified stream */
	public void export(DataOutput out) throws IOException;

}

