/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package org.qp.android;
public interface IQuestPlugin extends android.os.IInterface
{
  /** Default implementation for IQuestPlugin. */
  public static class Default implements org.qp.android.IQuestPlugin
  {
    @Override public void showQSPFile(java.lang.String pathToQSPFile) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements org.qp.android.IQuestPlugin
  {
    private static final java.lang.String DESCRIPTOR = "org.qp.android.IQuestPlugin";
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an org.qp.android.IQuestPlugin interface,
     * generating a proxy if needed.
     */
    public static org.qp.android.IQuestPlugin asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof org.qp.android.IQuestPlugin))) {
        return ((org.qp.android.IQuestPlugin)iin);
      }
      return new org.qp.android.IQuestPlugin.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      switch (code)
      {
        case INTERFACE_TRANSACTION:
        {
          reply.writeString(descriptor);
          return true;
        }
        case TRANSACTION_showQSPFile:
        {
          data.enforceInterface(descriptor);
          java.lang.String _arg0;
          _arg0 = data.readString();
          this.showQSPFile(_arg0);
          reply.writeNoException();
          return true;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
    }
    private static class Proxy implements org.qp.android.IQuestPlugin
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public void showQSPFile(java.lang.String pathToQSPFile) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(pathToQSPFile);
          boolean _status = mRemote.transact(Stub.TRANSACTION_showQSPFile, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().showQSPFile(pathToQSPFile);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      public static org.qp.android.IQuestPlugin sDefaultImpl;
    }
    static final int TRANSACTION_showQSPFile = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    public static boolean setDefaultImpl(org.qp.android.IQuestPlugin impl) {
      // Only one user of this interface can use this function
      // at a time. This is a heuristic to detect if two different
      // users in the same process use this function.
      if (Stub.Proxy.sDefaultImpl != null) {
        throw new IllegalStateException("setDefaultImpl() called twice");
      }
      if (impl != null) {
        Stub.Proxy.sDefaultImpl = impl;
        return true;
      }
      return false;
    }
    public static org.qp.android.IQuestPlugin getDefaultImpl() {
      return Stub.Proxy.sDefaultImpl;
    }
  }
  public void showQSPFile(java.lang.String pathToQSPFile) throws android.os.RemoteException;
}
