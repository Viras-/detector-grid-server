/*
 * This file is part of DetectorGridServer.
 * 
 * DetectorGridServer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DetectorGridServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with DetectorGridServer.  If not, see <http://www.gnu.org/licenses/>.
 */
package at.technikum_wien.detectorgridserver.communication;

import at.technikum_wien.detectorgrid.CommunicationProtocol;
import at.technikum_wien.detectorgrid.TagInformation;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author wkoller
 */
public abstract class Server implements CommunicationProtocol {
    /**
     * Reference to MySQL connection
     */
    Connection connection = null;
    
    /**
     * Init the server and open connection to MySQL
     * @param address 
     */
    protected void init() {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost/detector-grid?"
                        + "user=root&password=");
        } catch (SQLException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Handle an incoming message and trigger the action for it
     *
     * @param messageComponents
     * @return
     * @throws Exception
     */
    public void handleMessage(String[] messageComponents) throws Exception {
        // check if we have at least one message component
        if(messageComponents.length <= 0) {
            throw new Exception("Invalid message components passed to 'handleMessage'");
        }
        
        // determine action and trigger function for it
        if( messageComponents[0].equals(MESSAGE_FOUND)) {
            // check for tag-code to find in message
            if (messageComponents.length >= 5) {
                foundTag(TagInformation.fromMessage(messageComponents));
            } else {
                throw new Exception("Invalid Message: '" + messageComponents[0] + "' - missing payload!");
            }
        }
        else {
            throw new Exception("Unknown Message: '" + messageComponents[0] + "'" + " / '" + MESSAGE_FOUND + "'");
        }
    }

    /**
     * Called by the actual implementation whenever a tag was found
     * @param tagInformation 
     */
    public void foundTag(TagInformation tagInformation) {
        PreparedStatement prepStmt = null;
        try {
            int reader_id = 0;
            
            prepStmt = connection.prepareStatement("SELECT `reader_id` FROM `tbl_reader` WHERE `uuid` = ?");
            prepStmt.setString(1, tagInformation.readerId);
            ResultSet rs = prepStmt.executeQuery();
            if( rs.next() ) {
                reader_id = rs.getInt("reader_id");
            }
            else {
                throw new Exception("Invalid UUID from reader");
            }
            
            prepStmt = connection.prepareStatement("REPLACE INTO `tbl_tag_occurence` (`oid`, `strength`, `seenTick`, `reader_id`) values (?, ?, ?, ?)");
            prepStmt.setInt(1, Integer.parseInt(tagInformation.tagCode));
            prepStmt.setInt(2, tagInformation.distance);
            prepStmt.setInt(3, tagInformation.seenTick);
            prepStmt.setInt(4, reader_id);
            
            prepStmt.execute();
        } catch (Exception ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
}
