// Copyright (c), Pantor Engineering AB, 2013-
// All rights reserved

package com.pantor.test;

import com.pantor.blink.DefaultObjectModel;
import com.pantor.blink.Logger;
import com.pantor.blink.ConciseLogger;
import com.pantor.xmit.Server;
import com.pantor.xmit.InMemoryJournal;
import com.pantor.xmit.Server.NegotiationRequest;
import com.pantor.xmit.Server.EstablishmentRequest;
import com.pantor.xmit.XmitException;

public class TestServer implements Server.NegotiationObserver
{
   public static class Session implements Server.Session.EventObserver
   {
      Session (Server.Session sn)
      {
         this.sn = sn;
         sn.setEventObserver (this);
      }

      private final static java.util.Random rand = new java.util.Random ();
      
      public void onPing (test.Ping ping)
      {
         int val = ping.getValue ();
         int pongCount = ping.getPongCount ();
         
         // Induce some delay to test the operation timeout functionality
         // on the client side
         if (rand.nextInt (100) > 80)
            try
            {
               log.info ("Delaying pong %d", val);
               Thread.sleep (3500);
            }
            catch (InterruptedException e)
            {
            }
         
         try
         {
            log.info ("Got ping (%d), sending (%d) pong messages starting from (%d)", val, pongCount, val);
            for (int i = 0; i < pongCount; ++ i)
            {
               test.Pong p = new test.Pong ();
               p.setValue (val ++);
               sn.send (p);
            }
         }
         catch (Throwable e)
         {
            while (e.getCause () != null)
               e = e.getCause ();
            log.error (e.toString ());
         }
      }

      @Override
      public void onEstablishmentRequest (EstablishmentRequest e,
                                          Server.Session s)
      {
         log.info ("Session received establishment request");
      }

      @Override
      public void onEstablished (Server.Session s)
      {
         log.info ("Session established");
      }

      @Override
      public void onTerminate (String reason, Throwable cause, Server.Session s)
      {
         log.info ("Session terminated: %s", reason);
      }

      @Override
      public void onFinished (Server.Session s)
      {
      }

      @Override
      public void onDatagramStart (Server.Session s)
      {
      }
      
      @Override
      public void onDatagramEnd (Server.Session s)
      {
      }
      
      private final Server.Session sn;
   }

   @Override 
   public void onNegotiationRequest (NegotiationRequest req, Server.Session s)
      throws XmitException
   {
      s.addAppObserver (new Session (s));
      req.accept (new InMemoryJournal (req.getSessionId ()));
   }
   
   public static void main (String... args) throws Exception
   {
      DefaultObjectModel om = new DefaultObjectModel (args [0], args [1]);
      int port = Integer.parseInt (args [2]);
      Server s = Server.createServer (port, om, new TestServer ());
      log.info ("Starting server on port %d", port);
      s.run ();
   }

   private static final Logger.Level LogLevel = Logger.Level.Info;
   private static final int LogIndent = 30;
   
   static { Logger.Manager.setFactory (
         new ConciseLogger.Factory (LogLevel, LogIndent)); }

   private static Logger log = Logger.Manager.getLogger (TestClient.class);
}
