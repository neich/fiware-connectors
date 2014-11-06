/**
 * Copyright 2014 Telefonica Investigación y Desarrollo, S.A.U
 *
 * This file is part of fiware-connectors (FI-WARE project).
 *
 * fiware-connectors is free software: you can redistribute it and/or modify it under the terms of the GNU Affero
 * General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * fiware-connectors is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with fiware-connectors. If not, see
 * http://www.gnu.org/licenses/.
 *
 * For those usages not covered by the GNU Affero General Public License please contact with Francisco Romero
 * francisco.romerobueno@telefonica.com
 */

package es.tid.fiware.fiwareconnectors.cygnus.channels;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.Transaction;
import org.apache.flume.channel.MemoryChannel;
import org.apache.flume.conf.Configurable;
import org.apache.flume.lifecycle.LifecycleState;
import org.apache.log4j.Logger;

/**
 * Custom Flume channel addressing HA issues, based on the combination of a memory-based channel and a JDBC channel. The
 * memory-based sub-channel is used for local puts when the local Cygnus works as the active part of an active-passive
 * HA configuration, while the JDBC sub-channel is used by the remote Cygnus when such a Cyguns becomes active and the
 * local one becomes passive. This way, a double event put is done (locally in memory, remotely in JDBC), ensuring the
 * data is not lost is the active Cygnus crashes and the passive one becomes the new active one.
 * 
 * Obviously, it is needed a mechanism for removing remote copies of already processed local copies. This can be done by
 * a specific thread, freeing the sinks about this overhead.
 * 
 * @author frb
 */
public class HAMemoryChannel implements Channel, Configurable {
    
    private Logger logger;
    private String name;
    private MemoryChannel channel;
    private final ArrayList<Event> haChannel = new ArrayList<Event>();
    private ChannelSynchronizer thread;
    private boolean ha;
    private int localPort;
    private String remoteHost;
    private String remotePort;
    private int capacity;
    private int transCapacity;
    
    /**
     * Constructor.
     */
    public HAMemoryChannel() {
    } // HAMemoryChannel
    
    @Override
    public void configure(Context context) {
        logger = Logger.getLogger(HAMemoryChannel.class);
        this.ha = context.getString("ha", "false").equals("true") ? true : false;
        this.localPort = context.getInteger("local_port", 5090);
        this.remoteHost = context.getString("remote_host", "localhost");
        this.remotePort = context.getString("remote_port", "5090");
        this.capacity = context.getInteger("capacity", 1000);
        this.transCapacity = context.getInteger("capacity", 100);
        this.channel = new MemoryChannel();
        this.haChannel.ensureCapacity(capacity);
    } // configure
    
    @Override
    public Transaction getTransaction() {
        return null;
    } // getTransaction
    
    @Override
    public void put(Event event) {
        if (ha) {
            synchronized (haChannel) {
                haChannel.add(event);
            } // synchronized

            // remote_channel_put(remoteHost, remotePort, event);
        } else {
            channel.put(event);
        } // if else
    } // put
    
    @Override
    public Event take() {
        if (ha) {
            Event event = null;

            synchronized (haChannel) {
                event = haChannel.get(0);
                haChannel.remove(0);
            } // synchronized

            // remote_channel_remove();
            return event;
        } else {
            return channel.take();
        }
    } // take
    
    @Override
    public LifecycleState getLifecycleState() {
        return null;
    } // getLifecycleState
    
    @Override
    public void start() {
        if (ha) {
            thread = new ChannelSynchronizer(localPort, haChannel);
            thread.start();
        } // if
    } // start
    
    @Override
    public void stop() {
        if (ha) {
            thread.shutdown();
            
            try {
                thread.join();
            } catch (InterruptedException ex) {
                logger.error("");
            } // try catch
        } // if
    } // stop
    
    @Override
    public String getName() {
        return name;
    } // getName
           
    @Override
    public void setName(String name) {
        this.name = name;
    } // setName
    
    /**
     * Thread in charge of:
     *    - receiving remote messages about putting an event into the local channel, or deleting an event from the local
     *      channel.
     *    - putting an event, remotely sent, into the local channel.
     *    - deleting an event, remotely identified, from the local channel.
     */
    private class ChannelSynchronizer extends Thread {
        
        private Logger logger;
        private boolean shutdown;
        private ServerSocket serverSocket;
        
        public ChannelSynchronizer(int listeningPort, ArrayList<Event> channel) {
            logger = Logger.getLogger(ChannelSynchronizer.class);
            
            try {
                serverSocket = new ServerSocket(listeningPort);
            } catch (IOException e) {
                logger.fatal("The channel synchronizer could not be started. Details=" + e.getMessage());
            } // try catch
            
            shutdown = false;
        } // ChannelSynchronizer
        
        @Override
        public void run() {
            while (!shutdown) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                } catch (IOException e) {
                    logger.fatal("The channel synchronizer could not receive messages. Details=" + e.getMessage());
                } // try catch
            } // while
        } // run
        
        public void shutdown() {
            this.shutdown = true;
        } // shutdown
        
    } // ChannelSynchronizer
    
} // HAMemoryChannel
