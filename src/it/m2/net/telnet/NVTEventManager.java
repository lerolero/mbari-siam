package it.m2.net.telnet;


class NVTEventManager
{
	java.util.Vector listeners = new java.util.Vector();

	void addListener(NVTListener listener)
	{
		listeners.add(listener);
	}

	void fireEvent(NVTEvent e)
	{
		for (int i = 0 ; i < listeners.size() ; i++)
		{
			NVTListener listener = (NVTListener)listeners.elementAt(i);
			listener.action(e);
		}
	}

	int getListenerCount()
	{
		return listeners.size();
	}
}
