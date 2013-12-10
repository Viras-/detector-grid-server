/*
 * This file is part of DetectorGridClient.
 * 
 * DetectorGridClient is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DetectorGridClient is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with DetectorGridClient.  If not, see <http://www.gnu.org/licenses/>.
 */
package at.technikum_wien.detectorgridserver.communication.spread;

import at.technikum_wien.detectorgridserver.communication.Server;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
import spread.BasicMessageListener;
import spread.SpreadConnection;
import spread.SpreadGroup;
import spread.SpreadMessage;

/**
 *
 * @author wkoller
 */
public class SpreadServer extends Server implements BasicMessageListener {
    /**
     * Group name for messages sent to the server
     */
    public static final String SERVER_GROUP_NAME = "detectorGridServer";
    /**
     * Group name for messages sent to the client
     */
    public static final String CLIENT_GROUP_NAME = "detectorGridClient";
    
    /**
     * Reference to spread connection
     */
    protected SpreadConnection spreadConnection = null;
    
    /**
     * Reference to listening group
     */
    protected SpreadGroup listenSpreadGroup = null;
    
    public void init(String address) {
        try {
            super.init();
            // open the connection to the spread daemon
            spreadConnection = new SpreadConnection();
            spreadConnection.connect(InetAddress.getByName(address), 0, "server", true, false);
            spreadConnection.add(this);

            // join the default group for the detector grid application
            listenSpreadGroup = new SpreadGroup();
            listenSpreadGroup.join(spreadConnection, SERVER_GROUP_NAME);
        } catch (Exception ex) {
            Logger.getLogger(SpreadServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void messageReceived(SpreadMessage sm) {
        if( sm.isRegular() ) {
            try {
                String messageContent = new String(sm.getData(), "UTF-8").trim();
                Logger.getLogger(SpreadServer.class.getName()).log(Level.FINE, "New Message: ''{0}''", messageContent);
                
                // sepearate message into components and start actions from it
                String[] messageComponents = messageContent.split(MESSAGE_SEPARATOR);
                try {
                    super.handleMessage(messageComponents);
                } catch (Exception ex) {
                    Logger.getLogger(SpreadServer.class.getName()).log(Level.SEVERE, "Error while handling message", ex);
                }
                
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(SpreadServer.class.getName()).log(Level.SEVERE, "Error while decoding message content", ex);
            }
        }
    }
}
