package it.m2.net.telnet;

import java.io.IOException;

class TelnetOptionStatus
{
	private int				option;
	private boolean				willSent;
	private boolean				wontSent;
	private boolean				doSent;
	private boolean				dontSent;
	private boolean				isDo;
	private boolean				isWill;
	private boolean				willProcessed;
	private boolean				doProcessed;
	private boolean				dontProcessed;
	private boolean				wontProcessed;
	private TelnetOptionHandler	handler;

	public TelnetOptionStatus(int option)
	{
		this.dontProcessed	= false;
		this.wontProcessed	= false;
		this.doProcessed	= false;
		this.willProcessed	= false;
		this.option		= option;
		this.willSent		= false;
		this.wontSent		= false;
		this.doSent		= false;
		this.dontSent		= false;
		this.isDo		= false;
		this.isWill		= false;
		this.handler		= null;
	}

	public boolean isDontProcessed()
	{
		return dontProcessed;
	}

	public void setDontProcessed(boolean mode)
	{
		dontProcessed = mode;
	}


	public boolean isWontProcessed()
	{
		return wontProcessed;
	}

	public void setWontProcessed(boolean mode)
	{
		wontProcessed = mode;
	}

	
	
	public void setWillProcessed(boolean mode)
	{
		willProcessed = mode;
	}

	public boolean isDoProcessed()
	{
		return doProcessed;
	}

	public void setDoProcessed(boolean mode)
	{
		doProcessed = mode;
	}

	public boolean isWillProcessed()
	{
		return willProcessed;
	}

	public void setDontSent(boolean mode)
	{
		dontSent = mode;
	}

	public void setDoSent(boolean mode)
	{
		doSent = mode;
	}

	public void setWillSent(boolean mode)
	{
		willSent = mode;
	}

	public void setWontSent(boolean mode)
	{
		wontSent = mode;
	}

	public boolean isDontSent()
	{
		return dontSent;
	}

	public boolean isDoSent()
	{
		return doSent;
	}

	public boolean isWillSent()
	{
		return willSent;
	}

	public boolean isWontSent()
	{
		return wontSent;
	}


	public boolean isDo()
	{
		return isDo;
	}

	public boolean isWill()
	{
		return isWill;
	}

	public void setDo(Telnet t,boolean mode) 
	{
		if (mode != isDo && handler != null)
		{
			handler.setDo(t,option,isWill,mode);
		}
		
		isDo = mode;
	}

	public void setWill(Telnet t,boolean mode)
	{
		if (mode != isDo && handler != null)
		{
			handler.setWill(t,option,isDo,mode);

		}
		isWill = mode;
	}
	

	public int		getOption()
	{
		return option;
	}




	public void		sb(Telnet telnet,int sbLength,byte sbBuffer[]) throws IOException
	{
		if (handler != null)
			handler.sb(telnet,getOption(),sbLength,sbBuffer);
	}

	/**
	 * Set a option handler 
	 */
	void setHandler(TelnetOptionHandler handler)
	{
		this.handler = handler;
	}

	TelnetOptionHandler getHandler()
	{
		return this.handler;
	}
	
	public String toString()
	{
		return "TELNET Option "+option;
	}

}


